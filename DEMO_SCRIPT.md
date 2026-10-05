# Guión de demostración — 18 minutos

## Preparación previa

JDK 17, Maven, acceso privado a EC2/RDS y RabbitMQ Management. Preparar dos archivos de entorno no versionados: .env.master y .env.secure.
Usar vhosts /demo y /demo-secure y bases independientes para evitar incompatibilidades de cola/esquema.
Aplicar db/mysql.sql y db/postgres.sql de secure con usuario de migraciones antes de la charla.
Compilar ambas ramas previamente para llenar la caché Maven. Después de cada checkout, detener procesos y recompilar.
Cargar las variables correspondientes **en ambas terminales**. Spring no lee .env automáticamente.
PowerShell: en secure, `. ./scripts/load-env.ps1 -Path .env.secure`; en master configurar las variables con `$env:NOMBRE='valor'` o cargar un script propio preparado.
Bash: `set -a; source .env.master; set +a`, y análogo para secure.

## 0:00–7:00 — funciona

```bash
git checkout master
mvn -f publisher-service/pom.xml clean package
mvn -f consumer-service/pom.xml clean package
# Terminal 1:
java -jar consumer-service/target/consumer-service-1.0.0.jar
# Terminal 2:
java -jar publisher-service/target/publisher-service-1.0.0.jar
```

1. Mostrar diagrama: Publisher EC2 → RabbitMQ EC2 → Consumer EC2; cada aplicación tiene su RDS.
2. Mostrar OrderService, OrderPublisher y RabbitMQConfig.
3. Identificar producer, exchange orders.exchange, routing key orders.created, queue orders.created.queue y consumer.
4. Mostrar log de publicación y recepción.
5. MySQL: `SELECT id, amount, status FROM orders ORDER BY id DESC;`
6. PostgreSQL: `SELECT id, source_order_id, amount FROM orders ORDER BY id DESC;`
7. Para observar un mensaje en espera: detener Consumer, detener y volver a ejecutar Publisher; mirar Ready en RabbitMQ; reiniciar Consumer.

Preguntar: **“¿Que funcione significa que está listo para producción?”**
Discutir: broker caído, duplicado, mensaje inválido, Consumer fallando, cantidad de intentos, destino final, secretos y logs.

## 7:00–14:00 — seguridad y resiliencia

Detener ambos procesos, cambiar rama y cargar .env.secure en ambas terminales.

```bash
git checkout secure
mvn -f publisher-service/pom.xml clean package
mvn -f consumer-service/pom.xml clean package
# Terminal 1, modo pedagógico explícito:
java -jar consumer-service/target/consumer-service-1.0.0.jar --demo.retry-invalid=true
# Terminal 2:
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=normal
```

Mostrar OrderCreatedEvent: eventId, orderId, correlationId, occurredAt, eventType y payload mínimo.
Mostrar validación y ausencia de nombre/correo en el mensaje.
Esperar “Outbox confirmado”, detener Publisher con Ctrl+C.

### Duplicado

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=duplicate
```

Se crea una orden y se envía dos veces el **mismo eventId**.
Esperar “Orden procesada” y “Duplicado ignorado”.
Consultar orders y processed_events; el escenario agrega una sola orden y un solo eventId.
Detener Publisher.

### Inválido y tres intentos

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=invalid
```

Se crea la orden normal más una copia de prueba con amount=-1.
Mostrar logs attempt=1/3, 2/3, 3/3 y rechazo hacia DLQ.
En Management, vhost /demo-secure → orders.created.dlq → consultar con requeue y mostrar x-death.reason=rejected.
Los intentos son **tres en total**, no un intento más tres reintentos.
Explicar que repetir una validación permanente no la arregla: se habilitó solo para visualizar el flujo; normalmente se rechaza directamente.
Detener Publisher.

Opcional:

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.scenario=malformed
```

JSON roto va directamente a DLQ, sin persistir negocio para esa copia.

## 14:00–18:00 — consistencia

Mostrar en master (con git show, sin cambiar procesos):

```bash
git show master:publisher-service/src/main/java/com/example/publisher/OrderService.java
```

Explicar: save() confirma MySQL, RabbitMQ falla, queda una orden sin evento.

En secure, detener el Publisher previo. Simular una conexión de Publisher fallida usando **un puerto de prueba confirmado como cerrado**, sin apagar el broker compartido:

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --spring.rabbitmq.port=35999
```

MySQL:

```sql
SELECT id, amount FROM orders ORDER BY id DESC;
SELECT id, event_id, attempts, sent_at FROM outbox_events WHERE sent_at IS NULL;
```

La orden y su Outbox existen; sent_at sigue NULL. Esperar “Outbox pendiente”.
Detener Publisher, restaurar su conexión normal y arrancar sin generar una nueva orden:

```bash
java -jar publisher-service/target/publisher-service-1.0.0.jar --demo.create-order=false
```

Mostrar confirmación, sent_at poblado y registro en PostgreSQL.
Explicar la posible duplicación si el proceso cae entre confirmación RabbitMQ y actualización de Outbox; por eso también existe idempotencia.

Cerrar comparando los dos prompts de PROMPTS.md:
“La IA puede generar código que funciona. La responsabilidad del desarrollador es asegurar que además sea seguro, resiliente y correcto.”
