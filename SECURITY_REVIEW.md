# Revisión de seguridad

master es deliberadamente básico. La evolución prevista se realiza en secure sobre las mismas aplicaciones.

| Problema | Riesgo | Impacto | Corrección en secure | Categoría |
|---|---|---|---|---|
| Commit separado de publicación | Caída entre operaciones | Orden sin evento | Transactional Outbox y confirmaciones | Consistencia |
| Reentrega de mensajes | Duplicados | Operaciones repetidas | eventId único e idempotencia transaccional | Integridad |
| Mensajes sin validar | Datos inválidos | Persistencia incorrecta | DTO explícito y Jakarta Validation | Validación |
| Error permanente | Reentrega sin límite | Saturación | Máximo 3 intentos y DLQ | Disponibilidad |
| Publicación sin confirmar | Ruta inexistente | Pérdida de eventos | mandatory, returns y confirms | Fiabilidad |
| Secretos en archivos | Filtración | Acceso indebido | Entorno / gestor de secretos | Confidencialidad |
| Logs con payload | Exposición de clientes | Filtración de datos personales | Log de identificadores técnicos | Privacidad |

## AWS y red

RabbitMQ EC2 y ambas RDS deben estar en red privada/VPC, sin exposición pública.
Security Groups: AMQP (5671 con TLS; 5672 solo para demo privada) desde las EC2 autorizadas; MySQL 3306 solo desde Publisher; PostgreSQL 5432 solo desde Consumer.
Management no debe quedar público. Usar túnel o acceso privado.
Usuarios RabbitMQ específicos, vhost dedicado, permisos mínimos y usuarios SQL separados.
En producción, un usuario de migraciones distinto del usuario de ejecución; evitar permisos DDL en este último.
Guardar secretos en AWS Secrets Manager, Parameter Store o equivalente; rotarlos y no registrar contraseñas.
TLS debe validar certificados y hostname. Una red privada no sustituye el cifrado.

La rama secure es una demostración de controles, no una certificación de preparación para producción. Revisar migraciones, monitoreo, límites, backups, retención, pruebas de carga y recuperación antes de un despliegue real.
