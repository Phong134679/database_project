# Library Database — Access Log Update (v2)

## Included files

1. `01_create_database_final_v2.sql` — schema, including nullable `ACCESS_LOG.person_id` and its foreign key to `PERSON`.
2. `02_insert_data_final_v2.sql` — sample data. `ACCESS_LOG` intentionally starts empty; no static IP values are seeded.
3. `views_final_v2.sql` — detailed Access Log view includes optional Person ID/name and the Guest label.
4. `procedures_final_v2.sql` — `sp_access_digital_journal` no longer accepts an IP input. It generates a synthetic IP on each successful access and stores the optional Person ID.
5. `ERD_final_v2.md` — updated ERD and structural rules.
6. `DATABASE_WORKFLOW_FINAL_v2.md` — complete workflow documentation.

## Deployment order

```bash
mysql -u root -p < 01_create_database_final_v2.sql
mysql -u root -p < 02_insert_data_final_v2.sql
mysql -u root -p < views_final_v2.sql
mysql -u root -p < procedures_final_v2.sql
```

Use MySQL CLI or Workbench for stored procedure scripts containing `DELIMITER`.

## Access procedure signature

```sql
CALL sp_access_digital_journal(
    p_person_id,
    p_resource_id,
    @allowed,
    @reason,
    @log_id
);
```

The Java application must not obtain or pass the device/client IP. The procedure generates a synthetic random IPv4 value from documentation/test ranges for each successful access. This value is not a real IP, does not identify a device/person, may repeat, and is not suitable for authentication or security controls.

`ACCESS_LOG.person_id` stores the supplied valid Person ID; it is `NULL` for anonymous Guest access. Denied accesses are not logged.

**Important:** The schema script drops and recreates database objects. Back up any existing database before running it; this is a rebuild bundle, not a data-preserving migration.
