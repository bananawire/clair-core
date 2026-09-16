# Agent Instructions

* Use `nix-shell` to run Java, Maven, tests, and project commands.
* Do not create a new SQL migration for every change.
* Keep **1 migration SQL file per bounded context** and edit that file when the schema changes.
* Keep **1 seed SQL file per feature** and edit that file when seed data changes.
* Before creating a new SQL file, check if the corresponding migration or seed file already exists.
