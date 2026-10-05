# Guión de demostración (18 minutos)

## Preparación fuera del tiempo de charla

JDK 17, Maven, variables en ambas EC2 y servicios accesibles. Vhosts/bases exclusivamente de demo.
Compilar cada rama antes de mostrarla: no hacer descargas Maven durante la charla.
Al cambiar rama, detener ambas aplicaciones y recompilar; no ejecutar jars antiguos de target.

## 0–7 minutos: master

```bash
git checkout master
mvn -f publisher-service/pom.xml clean package
mvn -f consumer-service/pom.xml clean package
java -jar consumer-service/target/consumer-service-1.0.0.jar
# Otra terminal:
java -jar publisher-service/target/publisher-service-1.0.0.jar
```

Mostrar ARCHITECTURE.md, OrderService, OrderPublisher y RabbitMQConfig.
Señalar producer, exchange, routing key, queue y consumer.
Consultar orders en MySQL y PostgreSQL con las consultas del README.
Detener Consumer, publicar otra orden y mostrar Ready en RabbitMQ; reiniciar Consumer y ver cómo se vacía.
Preguntar: “¿Que funcione significa que está listo para producción?”
Plantear caída del broker, duplicados, contenido inválido, errores de Consumer, reintentos, destino final, secretos y logs.

## 7–14 minutos: secure

Detener ambos procesos antes de `git checkout secure`; recompilar ambas aplicaciones.
Usar un vhost distinto dedicado a secure: la cola con DLX tiene argumentos distintos y no se puede redeclarar encima de la cola de master.
Mostrar evento formal, validación, eventId, correlationId, retry limitado y DLQ.
Publicar un evento inválido, observar tres intentos y la DLQ.
Reenviar exactamente el mismo eventId, comprobar una sola fila de negocio.
Consultar las instrucciones de la rama secure para los comandos y opciones de sus escenarios.

## 14–18 minutos: consistencia

En master: MySQL COMMIT exitoso + publicación falla = orden persistida sin evento.
En secure: Order + OutboxEvent en una transacción; envío posterior con confirmación.
Interrumpir la conexión del Publisher de prueba al broker, crear orden, comprobar pendiente en outbox_events, restaurar conexión y observar envío.
Nunca detener infraestructura compartida para simular el fallo.
Concluir revisando el prompt mejorado y las pruebas que verifican su cumplimiento.
