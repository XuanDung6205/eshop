# Furniture database implementation plan

> Historical plan for the initial database-creation phase. The original constraints and execution record below describe that phase only. Later user-approved changes are recorded at the end; see `docs/database/README.md` for current setup instructions.

> **For agentic workers:** Use superpowers:executing-plans for inline execution. The user approved all proposed tables and local database creation.

**Goal:** Create a separate MariaDB `furniture_shop` database containing all 45 approved business tables without changing the existing `eshop` database or application.

**Architecture:** Versioned, fail-fast SQL bootstrap; InnoDB, utf8mb4, explicit foreign keys and checks. Separate product/SKU, inventory reservation, order snapshots, payment events, shipment allocation, returns and refunds.

**Tech Stack:** Installed MariaDB 12.2.2 client/server; Python standard library for database verification.

**Spec:** The approved 33-table proposal and 12 additional tables in this chat; reproduced in `docs/database/README.md` with implementation decisions.

## Global constraints

- Work in the current workspace; only add database SQL, verification and documentation files. Preserve the two existing modified application files.
- Never drop, truncate or modify an existing user database. Bootstrap must fail if the target database already exists.
- Never persist supplied database credentials in files or CLI arguments.
- No Spring Boot configuration/entity changes or user data migration in this phase.
- Stock allocation, cumulative quantities, coupon concurrency and verified payment callbacks require application transactions; schema checks cannot replace them.

## Review focus

- A variant/image/material or shipment/return linked to the wrong product/order must be rejected.
- Negative prices, impossible reserved quantities and invalid ratings must be rejected.
- Duplicate SKU, duplicate payment callback and duplicate cart lines must be rejected.
- Historical order/address/price snapshots survive catalog edits.
- Test data must be rolled back; only test-created scratch database may be dropped.

## Task 1: Verification contract

- [x] Add `docs/database/verify_schema.py`: verify the exact 45-table set, InnoDB and referential constraints, then exercise meaningful invalid/valid inserts inside a rolled-back transaction.
- [x] Run against the missing `furniture_shop` database and confirm failure identifies its absence (MariaDB error 1049).

## Task 2: Schema bootstrap

- [x] Add `docs/database/V001__create_furniture_shop.sql` for all 45 tables, indexes, checks, roles and one default warehouse.
- [x] Add `docs/database/README.md`: table inventory, units, ownership/status rules, application responsibilities and HeidiSQL instructions.
- [x] Apply to a uniquely named scratch DB, run verification, and remove only that newly created scratch DB after verification.

## Task 3: Install and verify

- [x] Confirm target is absent, apply bootstrap directly to local MariaDB, and run the same verification on the actual database.
- [x] Obtain one independent schema review, address material findings and rerun affected checks.
- [x] Verify existing `eshop` table count and no application-file edits by this task. Report the actual database name, table count, seed contents and compatibility limits.

## Execution record

- Server discovery: MariaDB 12.2.2; `eshop` has 7 tables; `furniture_shop` absent.
- Decision: create a separate database. No worktree is needed for additive database artifacts; no branch changes or automatic commits.
- Compatibility finding: MariaDB rejects a CHECK referencing AUTO_INCREMENT category ID (error 1901). Category-cycle/self-parent validation stays in the application, documented in README; all remaining SQL applied successfully.
- Independent review: mismatched order/coupon redemption and implicit UUID refund keys reproduced by failing tests, fixed with composite FK and mandatory caller-supplied refund key. Duplicate refund keys already rejected.
- Final verification: scratch and actual `furniture_shop` each passed 22 behavior checks; actual database has exactly 45 tables, 67 foreign keys, 61 CHECK constraints. Seed contains three roles and MAIN warehouse only. Test fixtures rolled back.
- Existing `eshop` still has 7 tables. The task-created `furniture_shop_test_20261112_a` was dropped after successful final verification. Application configuration, Java code and legacy SQL were not changed.

## Later user-approved transition

- The user subsequently requested deletion of the old local `eshop` database, then clarified and approved cleanup of legacy SQL/configuration in source. That deletion was performed; `furniture_shop` remains the 45-table business database.
- Removed legacy database/seed/tracking SQL and updated documentation. Datasource now defaults to `furniture_shop`; `DB_PASSWORD` and `JWT_SECRET` come from environment. Automatic SQL/vector initialization is disabled.
- Added `.env.example` without secret values and ignored local environment files.
- Java entities remain unmigrated. The new schema and existing Java application are not yet compatible; this is an explicitly documented intermediate state, not a completed application migration.
- Added the five-member work allocation without a day-by-day schedule, as requested by the user.
