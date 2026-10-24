# Migraciones de la base de datos (Flyway)

Hasta el Sprint 3, Hibernate creaba y cambiaba las tablas solo, al arrancar (`ddl-auto=update`). Con un servidor en uso eso tiene tres problemas: nadie revisa el cambio antes de que toque los datos, `update` nunca borra ni corrige nada (solo agrega), y no queda registro de qué cambió ni cuándo. Desde el Sprint 4 el esquema lo manejan **migraciones versionadas con Flyway**.

## Cómo funciona

- Cada cambio del esquema es un archivo SQL en `gestion-asilo-backend/src/main/resources/db/migration`, con nombre `V<n>__descripcion.sql` (dos guiones bajos). Ejemplo: `V2__agrega_estado_cancelada_a_receta.sql`.
- Al arrancar, Flyway compara esos archivos con la tabla `flyway_schema_history` de la base y ejecuta, en orden, los que faltan. Cada uno corre una sola vez.
- Después, HibernaPte solo **valida** (`ddl-auto=validate`): si una entidad pide una tabla o columna que no existe, o de otro tipo, el backend no arranca y el error dice cuál.
- Las pruebas (`./mvnw test`) arman su base con las mismas migraciones y también validan. Una entidad cambiada sin su migración rompe todas las pruebas.
- `EnumsEnLaBaseTest` revisa que la restricción CHECK de cada columna enum admita todos los valores del enum. Reemplaza a `RestriccionesDeEnum`, que corregía esas restricciones al arrancar.
- Los datos iniciales (roles, administrador, especialidades, exámenes) siguen en los seeders de Java. No van en migraciones.

## Reglas

1. **Todo cambio en una entidad lleva su migración en la misma rama.** Campo nuevo, campo que cambia de tipo, tabla nueva, índice, y también **un valor nuevo en un enum**.
2. **Una migración que ya llegó a `develop` no se edita nunca.** Flyway guarda una suma de control de cada archivo aplicado; si el archivo cambia, el backend no arranca. Un error se corrige con una migración nueva.
3. El número sigue al último que haya en `develop`. Si dos ramas toman el mismo número, la que se fusiona segunda renumera el suyo antes de fusionar.
4. SQL de PostgreSQL, a mano y revisado. Para escribirlo se puede partir del que generaría Hibernate (ver "Generar el SQL de una entidad").
5. Columna `NOT NULL` en una tabla con filas: lleva `DEFAULT`, o se agrega primero nula, se rellena con `UPDATE` y al final se pone `SET NOT NULL`.

### Ejemplos

Columna nueva, opcional:

```sql
-- V2__agrega_observaciones_a_receta.sql
ALTER TABLE receta ADD COLUMN observaciones varchar(500);
```

Columna nueva obligatoria en una tabla con filas:

```sql
ALTER TABLE interno ADD COLUMN activo boolean NOT NULL DEFAULT true;
```

Valor nuevo en un enum. La restricción se llama `<tabla>_<columna>_check` y se vuelve a crear con **la lista completa**, en el orden del enum:

```sql
-- V3__agrega_cancelada_a_estado_receta.sql
ALTER TABLE receta DROP CONSTRAINT receta_estado_check;
ALTER TABLE receta ADD CONSTRAINT receta_estado_check
    CHECK (estado IN ('PENDIENTE', 'DESPACHADA', 'CANCELADA'));
```

Columna enum nueva: basta con su migración (`CREATE TABLE` o `ADD COLUMN` con su `CHECK`); `EnumsEnLaBaseTest` la encuentra sola recorriendo las entidades.

## Generar el SQL que pediría Hibernate

Hibernate puede escribir el SQL completo del esquema según las entidades actuales, sin tocar la base de desarrollo. Así se generó `V1__esquema_inicial.sql`, y sirve también para copiar la definición exacta de una tabla nueva.

1. Cree una base vacía aparte, en el contenedor de desarrollo:

   ```powershell
   docker exec postgres_asilo_db createdb -U admin_asilo asilo_esquema
   ```

2. Cree, junto a su `application.properties`, el archivo `gestion-asilo-backend/src/main/resources/application-esquema.properties` (es temporal; no lo suba a git):

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5433/asilo_esquema
   spring.flyway.enabled=false
   spring.jpa.hibernate.ddl-auto=create
   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/esquema.sql
   spring.jpa.properties.hibernate.hbm2ddl.delimiter=;
   spring.jpa.properties.hibernate.format_sql=true
   ```

3. Arranque con ese perfil desde `gestion-asilo-backend`. Cuando el registro diga `Started GestionAsiloBackendApplication`, deténgalo con Ctrl+C:

   ```powershell
   Remove-Item target/esquema.sql -ErrorAction SilentlyContinue   # Hibernate agrega al final si ya existe
   ./mvnw spring-boot:run "-Dspring-boot.run.profiles=esquema"
   ```

4. El SQL queda en `target/esquema.sql`. Borre `application-esquema.properties` y la base temporal:

   ```powershell
   docker exec postgres_asilo_db dropdb -U admin_asilo asilo_esquema
   ```

Para V1 se copió el archivo completo a `db/migration/V1__esquema_inicial.sql`. Para una migración posterior se copian solo las sentencias de lo que cambió, pasadas a `ALTER TABLE` cuando la tabla ya existe.

## Bases que existían antes de Flyway

La base de producción y las de desarrollo ya tienen tablas, creadas por `update`, pero no tienen `flyway_schema_history`. Con `spring.flyway.baseline-on-migrate=true` y `baseline-version=1`, la primera vez que Flyway encuentra una base así la marca como si ya estuviera en V1, **sin ejecutar V1**, y desde ahí aplica V2 en adelante. Eso solo es correcto si el esquema de esa base es igual al de V1; por eso se comprueba antes.

### Desarrollo

Lo más simple es empezar de cero (se pierden los datos de prueba locales; los seeders vuelven a crear roles, administrador y catálogos):

```powershell
docker compose down -v
docker compose up -d
```

Si prefiere conservar los datos, agregue a su `application.properties` local las mismas líneas que tiene `application-example.properties` (`ddl-auto=validate` y las dos de `spring.flyway.baseline-*`) y arranque. Si `validate` se queja de algo, lo más rápido es el reinicio de arriba.

### Producción (primer despliegue con Flyway)

El despliegue con Flyway debe tener **las mismas entidades** que la versión que ya corre en el servidor. Antes del primer arranque se compara el esquema real con V1. Desde la carpeta del repositorio en el servidor, ya con la etiqueta nueva descargada y **antes** de reconstruir:

```bash
# 0. Respaldo manual, como en todo despliegue
~/respaldar.sh

# 1. Cargar V1 en una base temporal del mismo contenedor
C="docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T db"
$C createdb -U admin_asilo asilo_v1
$C psql -U admin_asilo -d asilo_v1 -v ON_ERROR_STOP=1 -q \
   < gestion-asilo-backend/src/main/resources/db/migration/V1__esquema_inicial.sql

# 2. Describir las dos bases de la misma forma
cat > /tmp/esquema.sql <<'SQL'
SELECT 'col', table_name, column_name, data_type, is_nullable,
       coalesce(column_default, ''), coalesce(character_maximum_length::text, ''),
       coalesce(numeric_precision::text, ''), coalesce(numeric_scale::text, '')
FROM information_schema.columns WHERE table_schema = 'public'
UNION ALL
SELECT 'restr', conrelid::regclass::text, contype::text, pg_get_constraintdef(oid), '', '', '', '', ''
FROM pg_constraint WHERE connamespace = 'public'::regnamespace
UNION ALL
SELECT 'indice', tablename, indexdef, '', '', '', '', '', ''
FROM pg_indexes WHERE schemaname = 'public'
ORDER BY 1, 2, 3, 4;
SQL
$C psql -U admin_asilo -d asilo_cabeza_algodon -At < /tmp/esquema.sql > /tmp/prod.txt
$C psql -U admin_asilo -d asilo_v1             -At < /tmp/esquema.sql > /tmp/v1.txt

# 3. Comparar: si no imprime nada, son iguales
diff /tmp/prod.txt /tmp/v1.txt

# 4. Borrar la base temporal
$C dropdb -U admin_asilo asilo_v1
```

Cómo leer el resultado:

- **Sin diferencias:** se reconstruye y arranca como siempre. En el registro debe aparecer `Successfully baselined schema with version: 1`.
- **Solo cambia la forma de escribir un `CHECK`, con los mismos valores:** no importa.
- **Columnas o tablas de más en producción** (de campos que ya no existen; `update` nunca borraba): `validate` no se queja, pero si una de esas columnas es `NOT NULL` sin valor por defecto, los `INSERT` fallarán. Se quitan a mano después del respaldo (`ALTER TABLE ... DROP COLUMN ...`), o se deja una migración V2 que lo haga.
- **Cualquier otra diferencia** (columna que falta, tipo distinto): no se arranca. Se revisa antes de seguir.

Si se prefiere no comparar, la otra opción es vaciar la base de producción y dejar que Flyway corra V1 desde cero. Se pierden los datos que haya cargado QA, pero el resultado es seguro:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
docker volume rm <carpeta>_asilo_db_data   # el nombre exacto sale de: docker volume ls
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Con la base vacía, el administrador inicial se vuelve a crear con `APP_ADMININICIAL_*` del `.env.prod`.

### Despliegues siguientes

Nada especial: respaldo, reconstruir y arrancar. Flyway aplica las migraciones nuevas, cada una en su transacción. Si una falla, el backend no arranca, la base queda como estaba antes de esa migración, y se vuelve a la etiqueta anterior. El registro de lo aplicado se consulta con:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml exec db \
  psql -U admin_asilo -d asilo_cabeza_algodon \
  -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank"
```
