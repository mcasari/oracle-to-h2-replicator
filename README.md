# Oracle to H2 Replicate Maven Plugin

Maven plugin that reads one or more **Oracle** schemas and replicates their structure (and optionally data) into an **H2** database running in **Oracle compatibility mode** (`MODE=Oracle`). Useful for local development, tests, or demos without a real Oracle instance.

## Requirements

- Java 8+
- Oracle JDBC driver (ojdbc11) and H2 are pulled in by the plugin
- Oracle user must have `SELECT` on the target schemas (e.g. `SELECT ANY DICTIONARY` or grants on `ALL_TABLES`/`ALL_TAB_COLUMNS` for the schemas to replicate)

## Installation

Install the plugin into your local repository:

```bash
cd oracle-to-h2-replicate-maven-plugin
mvn clean install
```

## Usage

### Command line

Replicate schemas by passing Oracle connection and schema list:

```bash
mvn com.example.maven.plugins:oracle-to-h2-replicate-maven-plugin:1.0.0-SNAPSHOT:replicate-schemas \
  -Doracle-h2.oracleJdbcUrl=jdbc:oracle:thin:@//localhost:1521/ORCL \
  -Doracle-h2.oracleUser=myuser \
  -Doracle-h2.oraclePassword=mypass \
  -Doracle-h2.schemas=SCOTT,HR
```

Optional: copy table data into H2:

```bash
mvn ... -Doracle-h2.copyData=true
```

Optional: use a **file-based** H2 database so it persists and can be used by tests or other processes:

```bash
mvn ... -Doracle-h2.h2JdbcUrl="jdbc:h2:file:./target/replicated;MODE=Oracle;DB_CLOSE_DELAY=-1"
```

Optional: **populate base data with Flyway** (run migrations against H2 after replication):

```bash
mvn ... -Doracle-h2.runFlyway=true -Doracle-h2.flywayLocations=classpath:db/migration
```

### From a POM

Add the plugin and run the goal `oracle-h2:replicate-schemas` (optionally in a profile):

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.example.maven.plugins</groupId>
      <artifactId>oracle-to-h2-replicate-maven-plugin</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <configuration>
        <oracleJdbcUrl>jdbc:oracle:thin:@//dbhost:1521/SERVICE</oracleJdbcUrl>
        <oracleUser>myuser</oracleUser>
        <oraclePassword>${env.ORACLE_PASSWORD}</oraclePassword>
        <schemas>SCOTT,HR,MYAPP</schemas>
        <!-- optional: list form -->
        <!-- <schemaList><param>SCOTT</param><param>HR</param></schemaList> -->
        <h2JdbcUrl>jdbc:h2:file:${project.build.directory}/replicated;MODE=Oracle;DB_CLOSE_DELAY=-1</h2JdbcUrl>
        <copyData>false</copyData>
        <!-- Populate base data from Flyway scripts -->
        <runFlyway>true</runFlyway>
        <flywayLocations>
          <param>classpath:db/migration</param>
        </flywayLocations>
        <flywayBaselineOnMigrate>true</flywayBaselineOnMigrate>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Then run:

```bash
mvn oracle-h2:replicate-schemas
```

Or with data copy:

```bash
mvn oracle-h2:replicate-schemas -Doracle-h2.copyData=true
```

## Configuration

| Parameter           | Property                     | Required | Default                                               | Description |
|--------------------|------------------------------|----------|--------------------------------------------------------|-------------|
| `oracleJdbcUrl`    | `oracle-h2.oracleJdbcUrl`    | Yes      | -                                                     | Oracle JDBC URL (e.g. `jdbc:oracle:thin:@//host:1521/SID`) |
| `oracleUser`       | `oracle-h2.oracleUser`       | Yes      | -                                                     | Oracle user |
| `oraclePassword`   | `oracle-h2.oraclePassword`   | Yes      | -                                                     | Oracle password |
| `schemas`          | `oracle-h2.schemas`          | Yes*     | -                                                     | Comma-separated schema names (e.g. `SCOTT,HR`) |
| `schemaList`       | `oracle-h2.schemaList`       | No       | -                                                     | Alternative: list of schema names in POM |
| `h2JdbcUrl`        | `oracle-h2.h2JdbcUrl`       | No       | `jdbc:h2:mem:replicated;MODE=Oracle;DB_CLOSE_DELAY=-1` | H2 JDBC URL; use `MODE=Oracle` for compatibility |
| `h2User`           | `oracle-h2.h2User`          | No       | `SA`                                                  | H2 user |
| `h2Password`       | `oracle-h2.h2Password`      | No       | ``                                                    | H2 password |
| `copyData`         | `oracle-h2.copyData`        | No       | `false`                                               | If `true`, copy table data from Oracle to H2 |
| `runFlyway`        | `oracle-h2.runFlyway`       | No       | `false`                                               | If `true`, run Flyway migrations against H2 after replication (base data) |
| `flywayLocations`  | `oracle-h2.flywayLocations` | No       | `classpath:db/migration`                              | Flyway script locations (e.g. `classpath:db/migration`, `filesystem:src/main/resources/db/migration`) |
| `flywayBaselineOnMigrate` | `oracle-h2.flywayBaselineOnMigrate` | No | `true`  | If `true`, Flyway baselines existing replicated schema and applies pending migrations |
| `skip`             | `oracle-h2.skip`             | No       | `false`                                               | Skip execution |

\* Either `schemas` or `schemaList` must be set.

### Populate with Flyway (base data)

When `runFlyway=true`, the plugin runs [Flyway](https://flywaydb.org/) against the replicated H2 database after creating the schema (and optionally copying Oracle data). Use this to load base/reference data from versioned SQL (or Java) migrations.

- **flywayLocations**: Where to find scripts. Default `classpath:db/migration` looks for resources in `src/main/resources/db/migration/` (e.g. `V1__base_data.sql`, `V2__more_data.sql`). Use `filesystem:path` for a directory on disk.
- **flywayBaselineOnMigrate**: Should be `true` when the H2 schema already has tables (replicated from Oracle), so Flyway baselines the existing schema and only runs *pending* migrations (e.g. INSERT scripts). Set to `false` only if you want Flyway to manage the schema from scratch.

Example migration script for H2 (Oracle mode) in `src/main/resources/db/migration/V1__base_data.sql`:

```sql
-- Base data for replicated schema (H2 Oracle mode)
INSERT INTO MYSCHEMA.MYTABLE (ID, NAME) VALUES (1, 'Sample');
```

## What is replicated

- **Schemas**: each specified Oracle schema is created in H2.
- **Tables**: all tables in those schemas (from `DatabaseMetaData.getTables(..., "TABLE")`).
- **Columns**: name, type (mapped to H2/Oracle-compatible types), size, nullability, default.
- **Primary keys**: single- or multi-column primary key constraints.
- **Data** (optional): when `copyData=true`, row data is copied table by table (no FK ordering; large tables may be slow).

Indexes, foreign keys, views, and triggers are not replicated in this version.

## Type mapping (Oracle → H2 Oracle mode)

- `VARCHAR2` / `VARCHAR` → `VARCHAR2(size)`
- `NUMBER` / `DECIMAL` → `NUMBER` or `NUMBER(p,s)`
- `DATE` / `TIMESTAMP` → `DATE` / `TIMESTAMP`
- `CLOB` / `LONG` → `CLOB`
- `BLOB` / `RAW` → `BLOB` / `RAW`
- Other types are mapped to the closest H2/Oracle-compatible type.

## In-memory vs file H2

- **In-memory** (default): `jdbc:h2:mem:replicated;MODE=Oracle;DB_CLOSE_DELAY=-1`  
  The database exists only in the JVM that runs the plugin. After the goal finishes, the JVM may still be running (e.g. next Maven goals), but nothing in the same build can reuse that in-memory DB unless you run custom code in the same process. Prefer a **file** URL for reuse (e.g. by tests in a later phase).

- **File**: e.g. `jdbc:h2:file:./target/replicated;MODE=Oracle;DB_CLOSE_DELAY=-1`  
  The database is created under `./target/replicated.*`. Other processes (or later Maven goals) can connect to the same URL to use the replicated schema and data.

## Skip execution

To bind the goal to a phase but run only when needed:

```xml
<execution>
  <phase>initialize</phase>
  <goals><goal>replicate-schemas</goal></goals>
  <configuration>
    <skip>true</skip>
  </configuration>
</execution>
```

Run with:

```bash
mvn initialize -Doracle-h2.skip=false -Doracle-h2.oracleJdbcUrl=... ...
```

## License

Same as your project or MIT/ASL 2.0 as needed.
