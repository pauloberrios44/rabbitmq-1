# Integración de la IA en el desarrollo seguro

Dos aplicaciones Java 17 / Spring Boot 4.0.3 / Maven / Spring AMQP / Jackson 3 / JPA.
La integración es exclusivamente RabbitMQ; no hay API REST ni servidor RabbitMQ embebido.

## Ramas

- `master`: ejemplo pedagógico. MySQL commit → JSON → RabbitMQ → PostgreSQL.
- `secure`: evolución del mismo código, después de validar master.
La versión básica no tiene retry explícito, DLQ, idempotencia ni Outbox.

## Estructura

```text
publisher-service/   Aplicación y pruebas independientes, MySQL
consumer-service/    Aplicación y pruebas independientes, PostgreSQL
.env.example        Valores ficticios; nunca subir secretos
ARCHITECTURE.md     Diagrama y decisiones
SECURITY_REVIEW.md  Riesgos y evolución
PROMPTS.md          Prompts para la charla
DEMO_SCRIPT.md      Guión de 18 minutos
```

## Preparar

1. Instalar JDK 17 y Maven 3.9+. Crear bases vacías `orders` en cada RDS y usuarios con permisos para la demo.
2. RabbitMQ ya debe existir en EC2. Crear un vhost dedicado `/demo` y un usuario con permisos configure/write/read sobre la topología orders.
3. Exportar las variables de `.env.example` en cada terminal (Publisher necesita MYSQL y RABBITMQ; Consumer POSTGRES y RABBITMQ). Spring Boot **no carga .env automáticamente**.
4. Usar hostname privado de EC2/RDS, nunca localhost en AWS. Los valores de ejemplo no son servidores reales.
5. La creación automática de tablas (`ddl-auto=update`) es únicamente para la demo sobre bases dedicadas.

En Bash: `set -a; source .env; set +a` (solo un archivo de confianza, fuera de Git).
En PowerShell: `$env:RABBITMQ_HOST='nombre-privado'`, etc. No poner contraseñas en comandos guardados ni en Git.

## Compilar y probar

```bash
mvn -f publisher-service/pom.xml clean verify
mvn -f consumer-service/pom.xml clean verify
```

JUnit 5 y Mockito verifican orden de operaciones, JSON y fallos. Las pruebas de repositorio usan H2 y no demuestran compatibilidad RDS. Se inicia el contexto explícitamente para conservar JUnit 5; Spring Framework 7 SpringExtension requiere JUnit 6.

## Ejecutar (dos terminales con sus variables)

```bash
java -jar consumer-service/target/consumer-service-1.0.0.jar
java -jar publisher-service/target/publisher-service-1.0.0.jar
```

Publisher crea una orden por inicio (`DEMO_CREATE_ORDER=false` desactiva la creación). Detener con Ctrl+C; repetir Publisher genera otra orden.
Consumer convierte el JSON y guarda una fila con `source_order_id` del productor. Las bases son independientes.

## Verificar

MySQL: `SELECT id, customer_id, amount, status FROM orders ORDER BY id DESC;`

PostgreSQL: `SELECT id, source_order_id, amount, status FROM orders ORDER BY id DESC;`

RabbitMQ Management, por túnel o red privada: vhost /demo → Exchanges → orders.exchange y Queues → orders.created.queue. Para ver mensajes pendientes, detener Consumer, ejecutar Publisher y después reiniciar Consumer. No usar “Get messages” con eliminación de mensajes para inspeccionar tráfico real.

## Límites deliberados

El commit MySQL termina antes del envío: una caída del broker puede dejar una orden sin evento. Un mensaje duplicado crea otra fila. Los errores del consumidor quedan sujetos al comportamiento por defecto del contenedor (posibles reentregas sin límite). No usar esta rama en producción.

Referencias: [Spring Boot 4](https://docs.spring.io/spring-boot/4.0/index.html), [conversión AMQP](https://docs.spring.io/spring-amqp/reference/amqp/message-converters.html).
