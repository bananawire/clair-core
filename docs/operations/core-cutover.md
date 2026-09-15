# clair-core: operación del corte HTTP con edge

## Preparación de base de datos

La versión actual usa `spring.jpa.hibernate.ddl-auto=update` para compatibilidad
con instalaciones existentes; los cambios de esquema del roster se mantienen
como SQL versionado y deben ejecutarse antes del despliegue de la aplicación.
En PostgreSQL:

```sh
# DB_PSQL_URL is a PostgreSQL libpq URL (not the JDBC DB_URL).
psql "$DB_PSQL_URL" -v ON_ERROR_STOP=1 \
  -f src/main/resources/db/migration/V1__add_device_roster_columns.sql
```

La migración es idempotente. Verificar con:

```sql
SELECT column_name FROM information_schema.columns
 WHERE table_name = 'devices' AND column_name = 'deleted';
SELECT indexname FROM pg_indexes WHERE indexname = 'idx_devices_updated_at';
```

No se elimina ni modifica ningún dato existente; el valor inicial de `deleted`
es `false`.

## Variables de integración

- `EDGE_TO_CORE_TOKEN`: secreto requerido por core para todos los endpoints
  `/api/v1/edge/**` y por el batch de telemetría. Si falta, core responde 401.
- `EDGE_TOKEN`: secreto usado por core al enviar el webhook de aviso al edge.
- `EDGE_WEBHOOK_URL`: URL base del edge. El webhook solo es una sugerencia de
  reconciliación; el edge debe usar los endpoints pull.
- `EDGE_WEBHOOK_DEVICES_URL`: alias legado aceptado temporalmente para la URL.

Los dos tokens son independientes y deben rotarse/distribuirse por separado.
Nunca se deben registrar en logs ni incluir en documentación con valores reales.

## Smoke check de Fase 1

Con core levantado y un token de prueba:

```sh
curl -i -H "X-Core-Token: $EDGE_TO_CORE_TOKEN" \
  'http://localhost:49220/api/v1/edge/devices?since=0'
curl -i 'http://localhost:49220/api/v1/edge/devices?since=0'
```

El primer comando debe devolver `200` y `devices`; el segundo `401`. Validar
además el batch con un registro de prueba y comprobar que cada resultado
conserva su `client_ref`.

## Rollback

El cambio es aditivo. Si la aplicación falla, revertir el artefacto a la versión
anterior; la columna e índice pueden permanecer en la base de datos. No ejecutar
`DROP COLUMN` durante este corte.
