# Integración de la IA en el desarrollo seguro

Dos aplicaciones independientes: Java 17, Spring Boot 4.0.3, Maven, Spring AMQP, Jackson 3, Jakarta Validation, JPA, JUnit 5 y Mockito.
Publisher usa MySQL; Consumer usa PostgreSQL. **La integración es exclusivamente RabbitMQ**, sin API REST.

## Comparar las etapas

| master | secure |
|---|---|
| Guardar orden → enviar JSON | Guardar orden + Outbox en una transacción |
| Mensaje simple con datos de cliente | Evento formal, sin nombre ni correo |
| Exchange, cola, binding | Topología anterior + DLX + DLQ |
| Sin idempotencia | eventId registrado atómicamente con el negocio |
| Comportamiento básico del listener | ACK tras commit; errores clasificados y retry limitado |
| Configuración mínima | Configuración validada, TLS por defecto, secretos externos |
| 8 pruebas | 49 pruebas, incluidas 8 de integración con servicios reales |

`secure` nace del commit de `master`; son los mismos proyectos.
Detener procesos antes de cambiar rama y ejecutar `clean package` para no reutilizar jars de otra etapa.

## Estructura

```text
publisher-service/        Order, OrderService, OrderPublisher, Outbox*, EventCodec, pruebas
consumer-service/         OrderConsumer, OrderService, ProcessedEvent*, EventCodec, pruebas
db/mysql.sql              Esquema secure del productor
db/postgres.sql           Esquema secure del consumidor
scripts/load-env.ps1      Carga de variables en PowerShell
.env.example              Valores ficticios
ARCHITECTURE.md            Diagramas y garantías
SECURITY_REVIEW.md         Controles, AWS y límites
PROMPTS.md                 Prompt básico y mejorado
DEMO_SCRIPT.md             Guión ejecutable de 18 minutos
VALIDATION.md              Evidencia de ejecución y alcance
```

## Preparar AWS

RabbitMQ ya existe en EC2: no se instala en las aplicaciones. Usar sus hosts privados y las dos RDS.
Crear un vhost **distinto** para cada rama: `/demo` y `/demo-secure`.
La cola de secure incorpora argumentos DLX; reutilizar la de master causa `PRECONDITION_FAILED`.
No borrar colas compartidas. Para reutilizar una topología real se necesita una migración planificada.

Crear bases dedicadas `orders_secure` en cada RDS y aplicar los scripts con un usuario de migraciones:

```bash
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u migration_user -p "$MYSQL_DATABASE" < db/mysql.sql
psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U migration_user -W -d "$POSTGRES_DATABASE" -v ON_ERROR_STOP=1 -f db/postgres.sql
```

En PowerShell, importar MySQL desde su cliente con `source C:/ruta/db/mysql.sql`.
Usar usuarios de ejecución distintos y con permisos mínimos. Publisher necesita SELECT/INSERT en orders y SELECT/INSERT/UPDATE en outbox_events. Consumer necesita SELECT/INSERT en orders y processed_events, y permisos de secuencia en PostgreSQL.
Las aplicaciones declaran topología, por lo que sus usuarios RabbitMQ necesitan configure/read/write sobre los recursos `orders.*` del vhost; en producción se puede provisionar topología con un usuario separado.

## Configurar

Copiar `.env.example` a `.env` (ignorado por Git), sustituir valores ficticios y cargarlo en **cada terminal**.
Spring Boot no carga `.env` automáticamente.

PowerShell:

```powershell
. ./scripts/load-env.ps1 -Path .env
```

Bash, únicamente con un archivo propio de confianza:

```bash
set -a
source .env
set +a
```

Publisher requiere RABBITMQ_* y MYSQL_*; Consumer RABBITMQ_* y POSTGRES_*.
Host, usuario, contraseña, vhost y puertos RabbitMQ se validan al iniciar.
TLS está activado por defecto: configurar certificados de confianza para RabbitMQ/MySQL en el truststore de Java, y la CA RDS de PostgreSQL mediante `--spring.datasource.hikari.data-source-properties.sslrootcert=/ruta/rds-ca.pem`.
Nunca desactivar verificación de hostname/certificado en AWS.

Solo para servicios temporales aislados, se permite:
`RABBITMQ_SSL_ENABLED=false`, `MYSQL_SSL_MODE=DISABLED`, `POSTGRES_SSL_MODE=disable`.
`DB_DDL_AUTO=validate` es el valor normal; usar los SQL de arriba. `update` solo es aceptable para una base desechable de demo.

## Compilar y probar

```bash
mvn -f publisher-service/pom.xml clean verify
mvn -f consumer-service/pom.xml clean verify
```

Sin infraestructura se ejecutan 41 pruebas y se omiten las 8 de integración.
Se mantiene JUnit 5 iniciando contextos explícitamente, porque Spring Framework 7 SpringExtension requiere JUnit 6.

Para las pruebas completas: crear bases **desechables** `orders_test` en ambos motores y vhost `integration`.
Exportar las credenciales normales de prueba y:

```bash
export RUN_INFRA_TESTS=true
export RABBITMQ_VHOST=integration
export TEST_MYSQL_URL='jdbc:mysql://mysql-test.internal:3306/orders_test?sslMode=VERIFY_IDENTITY'
export TEST_POSTGRES_URL='jdbc:postgresql://postgres-test.internal:5432/orders_test?sslmode=verify-full'
mvn -f publisher-service/pom.xml verify
mvn -f consumer-service/pom.xml verify
```

Los tests crean/eliminan sus tablas y purgan sus colas; nunca apuntarlos a RDS de negocio. Ejecutarlos secuencialmente porque comparten vhost.
Las pruebas cubren confirmaciones, devolución mandatory, timeout, rollback MySQL, duplicados concurrentes PostgreSQL, rollback de idempotencia, retry y DLQ real.

## Ejecutar

Terminal Consumer:

```bash
java -jar consumer-service/target/consumer-service-1.0.0.jar
```

Terminal Publisher, una orden por inicio:

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=normal
```

El proceso Outbox permanece activo. Detener Publisher con Ctrl+C después de observar confirmación para ejecutar otro escenario:

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=duplicate
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=invalid
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=malformed
```

Ejecutar **uno por vez**. Cada escenario crea una orden válida; duplicate agrega una segunda copia del mismo eventId, invalid agrega una copia con amount negativo y malformed agrega JSON roto. Las copias de prueba se guardan explícitamente en Outbox fuera del flujo normal validado.

Para observar tres intentos de validación durante la charla, iniciar Consumer con `--demo.retry-invalid=true`.
El valor normal es false: errores permanentes van directamente a DLQ; errores transitorios tienen **3 intentos totales (inicial + 2 reintentos)**. JSON malformado se rechaza inmediatamente incluso en demo.
Este límite es por entrega; un reinicio antes de ACK puede causar otra entrega.

## Verificar resultados

MySQL:

```sql
SELECT id, customer_id, amount, status FROM orders ORDER BY id DESC;
SELECT id, event_id, correlation_id, attempts, sent_at FROM outbox_events ORDER BY id DESC;
SELECT COUNT(*) AS pending FROM outbox_events WHERE sent_at IS NULL;
```

PostgreSQL:

```sql
SELECT id, source_order_id, customer_id, amount FROM orders ORDER BY id DESC;
SELECT event_id, processed_at FROM processed_events ORDER BY processed_at DESC;
```

RabbitMQ Management mediante red privada/túnel: seleccionar /demo-secure; revisar `orders.created.queue`, `orders.dlx` y `orders.created.dlq`.
DLQ retiene el mensaje con `x-death.reason=rejected`. Consultar con requeue si se quiere conservarlo.
Los logs propios muestran UUID, categoría e intento; no incluyen nombre, correo, JSON ni secretos.

Para recuperar pendientes después de una caída:

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.create-order=false
```

Ver [DEMO_SCRIPT.md](DEMO_SCRIPT.md) y [SECURITY_REVIEW.md](SECURITY_REVIEW.md) para la secuencia y los límites.

Referencias: [Spring Boot](https://docs.spring.io/spring-boot/4.0/index.html), [confirms RabbitMQ](https://www.rabbitmq.com/docs/confirms), [dead lettering](https://www.rabbitmq.com/docs/dlx).
