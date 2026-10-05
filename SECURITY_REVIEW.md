# Revisión de seguridad: master → secure

| Problema | Riesgo | Impacto | Corrección implementada en secure | Categoría |
|---|---|---|---|---|
| Commit separado de envío | Caída del broker | Orden sin evento | Orden + Outbox atómicos | Consistencia |
| Envío sin confirmación | NACK, timeout o ruta inexistente | Pérdida silenciosa | Confirms, mandatory y returns | Fiabilidad |
| Reentrega | Duplicar operaciones | Negocio incorrecto | PK eventId y transacción PostgreSQL | Integridad |
| Dos consumidores simultáneos | Carrera check-then-insert | Duplicados | INSERT ON CONFLICT, probado concurrentemente | Concurrencia |
| Datos sin validar | Datos nulos, importes negativos | Persistencia incorrecta | DTO, Jakarta Validation, JSON estricto | Validación |
| Mensajes excesivos | Agotamiento de recursos | Indisponibilidad | Límite de 16 KiB en consumo | Disponibilidad |
| Error permanente | Bucle de requeue | Saturación | Rechazo sin requeue a DLQ | Resiliencia |
| Error transitorio | Reintentos ilimitados | Saturación | Tres intentos totales por entrega | Resiliencia |
| ACK antes del commit | Fallo de persistencia | Pérdida del negocio | ACK manual posterior al commit | Consistencia |
| Datos personales en mensajes | Exposición innecesaria | Filtración | Payload mínimo, sin nombre ni email | Privacidad |
| Logs de payload/excepciones | Exponer datos | Filtración | Logs propios con UUID/categoría/intento | Privacidad |
| Configuración incompleta | Fallo tardío | Demo o servicio caído | Properties tipadas validadas al arrancar | Configuración |
| Transporte sin cifrar | Intercepción | Credenciales expuestas | TLS por defecto y validación de hostname | Confidencialidad |
| DDL automático | Cambios de esquema accidentales | Pérdida de control | SQL versionados y ddl-auto=validate | Operación |

## AWS / red

RabbitMQ EC2 debe estar en VPC privada, sin AMQP ni Management abiertos a Internet.
Publisher y Consumer acceden por direcciones privadas.
Security Groups: RabbitMQ 5671 solo desde los grupos de las EC2 de aplicaciones; MySQL 3306 solo desde Publisher; PostgreSQL 5432 solo desde Consumer.
Las RDS no deben ser públicas. Management solo por túnel/red privada autorizada.
No se entrega Terraform porque esta demo usa infraestructura existente.

Mínimo privilegio: usuarios RabbitMQ específicos por aplicación/vhost, usuarios SQL de ejecución sin DDL, usuario de migraciones separado.
Si se preaprovisiona la topología, se pueden retirar privilegios configure de las aplicaciones ajustando la declaración automática.
Guardar secretos en AWS Secrets Manager, Parameter Store o equivalente, inyectarlos al proceso, rotarlos y evitar su registro. .env es solo una comodidad local y está ignorado por Git.
TLS valida cadena y nombre del servidor; configurar CA/truststores. Red privada no sustituye TLS.

## Límites explícitos

- Esto es una demostración revisable, no una certificación de producción.
- Los servicios temporales utilizados para validar estaban aislados en loopback y usaron TLS desactivado; AWS, certificados, IAM y Security Groups no se verificaron.
- Las classic queues y su DLX no garantizan transferencia de dead letters si el destino falla; evaluar quorum y políticas at-least-once. Proteger y monitorizar también la DLQ.
- Retry está en memoria y limita intentos por entrega, no a través de caídas del proceso. Dimensionar prefetch/concurrencia y evaluar retry persistente para producción.
- Outbox no elimina duplicados; garantiza registro atómico del evento, mientras idempotencia protege efectos en PostgreSQL.
- Outbox usa bloqueo durante I/O para claridad. Una flota grande requiere leases/claim, backoff y observabilidad.
- No purgar processed_events antes del plazo máximo de replay, o los eventos antiguos volverán a procesarse.
- eventId debe provenir de productores autorizados. No hay firma de mensajes ni deduplicación semántica de diferentes eventId.
- Retención/limpieza de Outbox, alertas por pendientes, DLQ, backups, límites del broker y pruebas de carga quedan como trabajo de producción.
- Los escenarios inválidos son ayudas explícitas de la charla. Desactivar DEMO_CREATE_ORDER en ejecución continua.
- El límite de 16 KiB se comprueba después de recibir; configurar también límites de tamaño en RabbitMQ para mitigar tráfico hostil.
- No activar DEBUG ni logs de parámetros SQL en entornos con datos reales; revisar también logs de dependencias y acceso a diagnósticos.
