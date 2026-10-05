# Validación de master

Ejecutada el 4 de octubre de 2026 (America/Santiago).

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
