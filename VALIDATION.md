# Evidencia de validación

Ejecutada el 4 de octubre de 2026 (America/Santiago).

## master — commit 63277b9

- JDK 17, Maven 3.9.9, Spring Boot 4.0.3.
- Publisher: mvn verify, BUILD SUCCESS, 4 pruebas, 0 fallos/errores.
- Consumer: mvn verify, BUILD SUCCESS, 4 pruebas, 0 fallos/errores.
- Jars ejecutados como procesos independientes.
- Servicios temporales autorizados: MySQL 8.4.4, PostgreSQL 17.4, RabbitMQ 4.1.0.
- MySQL: orders.id=1, customer_id=1001, amount=25000.00, status=CREATED.
- RabbitMQ: ambos procesos conectaron y el listener recibió orderId=1.
- PostgreSQL: orders.id=1, source_order_id=1, amount=25000.00, status=CREATED.

Los servicios temporales viven fuera del repositorio y solo escuchan en loopback.
Esto no instala RabbitMQ dentro de las aplicaciones. La configuración AWS exige hosts externos.
La conectividad EC2/RDS, TLS y Security Groups no se verificaron: no se proporcionaron accesos AWS.

## secure

Compilación y pruebas finales: Maven test + package, JDK 17.

| Proyecto | Pruebas | Integración real incluida | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|---:|
| Publisher | 23 | 3 | 0 | 0 | 0 |
| Consumer | 26 | 5 | 0 | 0 | 0 |
| Total | 49 | 8 | 0 | 0 | 0 |

Se usó RUN_INFRA_TESTS=true con bases orders_test y vhost integration dedicados.
Sin esa variable, las ocho pruebas externas se omiten explícitamente.

### Evidencia automatizada

- Serialización y validación: datos faltantes, amount negativo, eventType inválido, exceso de tamaño, JSON malformado, campos desconocidos, claves duplicadas y JSON sobrante.
- MySQL real: commit de orden + Outbox y rollback de ambos cuando falla la inserción del Outbox.
- Confirmaciones: ACK positivo, NACK, devolución mandatory, timeout y fallo de conexión.
- Broker real: quitar el binding produce un return; el Outbox permanece pendiente y se publica al restaurar el binding.
- PostgreSQL real: doce llamadas concurrentes con el mismo eventId producen exactamente una fila de negocio y un marcador.
- PostgreSQL real: el fallo de negocio revierte el marcador de idempotencia; el mismo evento puede procesarse después.
- RabbitMQ real: entrega válida, duplicado ignorado, validación inválida y JSON malformado enviados a DLQ con x-death.reason=rejected.
- Retry: tres intentos transitorios, recuperación antes del límite, errores permanentes y modo pedagógico para inválidos.
- ACK solo después del procesamiento; un fallo al enviar ACK no repite el negocio en el mismo listener.

### Evidencia de los jars independientes

Se aplicaron db/mysql.sql y db/postgres.sql a bases orders_secure nuevas.
Ambas aplicaciones iniciaron con ddl-auto=validate, sin crear tablas automáticamente.

1. Normal: orden persistida y consumida.
2. Duplicate: dos mensajes del mismo eventId, una sola fila; log Duplicado ignorado.
3. Invalid: copia con amount negativo; logs 1/3, 2/3, 3/3 y llegada a DLQ.
4. Malformed: copia JSON inválida enviada directamente a DLQ.
5. Conexión de Publisher a un puerto cerrado: orden y Outbox persistieron; sent_at permaneció NULL.
6. Reinicio con conexión restaurada y demo.create-order=false: el evento pendiente llegó a Consumer.

Resultado acumulado de esta secuencia:

- MySQL: 5 órdenes, 8 filas Outbox confirmadas, 0 pendientes.
- PostgreSQL: 5 órdenes, 5 eventId procesados.
- RabbitMQ: 2 mensajes en orders.created.dlq (inválido y malformado).

El duplicado y las copias de diagnóstico explican las 8 filas Outbox frente a 5 órdenes.
Los servicios temporales se detienen al terminar; no forman parte de las aplicaciones.
La prueba de conectividad perdida afectó solo al Publisher de prueba, sin apagar ningún broker compartido.
TLS estaba desactivado únicamente para estos servicios temporales aislados. Su funcionamiento y el acceso AWS quedan por comprobar con la infraestructura del usuario.
