# El contexto cambia el resultado

## Prompt deliberadamente sencillo

> Crea con Spring Boot un productor y consumidor RabbitMQ para procesar órdenes.

## Prompt mejorado

> Crea dos aplicaciones Java 17 y Spring Boot 4 que procesen órdenes exclusivamente por RabbitMQ remoto. Publisher persiste en MySQL y Consumer en PostgreSQL. Usa configuración por variables de entorno validada y secretos externos. Define un OrderCreatedEvent con eventId, correlationId, occurredAt, eventType y un payload mínimo. Valida con Jakarta Validation y rechaza JSON malformado. Implementa un máximo de tres intentos totales, clasificación de errores recuperables y permanentes, y DLQ observable. Confirma el mensaje solo después del commit. Garantiza idempotencia atómica en PostgreSQL. Guarda Order y OutboxEvent en la misma transacción MySQL, publica con mandatory, returns y publisher confirms, y marca enviado solo tras confirmación positiva. No registres datos sensibles. Agrega pruebas de fallos, duplicados, retry, DLQ y Outbox, y explica limitaciones. Conserva el ejemplo básico en master y evoluciona los mismos proyectos en secure.

Preguntas de revisión: ¿qué pruebas sustentan cada afirmación?, ¿qué ocurre entre COMMIT y ACK?, ¿puede fallar el broker?, ¿puede repetirse el mensaje?, ¿hay secretos o datos personales en logs?

La IA puede generar código que funciona. La responsabilidad del desarrollador es asegurar que además sea seguro, resiliente y correcto.
