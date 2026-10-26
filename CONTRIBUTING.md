# Contributing to Donut Teams

Thanks for helping improve Donut Teams.

## How to contribute

1. Fork the repository and create a topic branch from `main`.
2. Make focused changes with clear commit messages.
3. Add or update tests when behavior changes.
4. Run `./gradlew build compileMatrix` (or `gradlew.bat build compileMatrix` on Windows) before opening a PR.
   For gameplay changes, run the smoke matrix in `tools/smoke` for at least one Paper and one Folia version.
5. Open a pull request describing the problem and the fix.

## Coding expectations

- Java 17 bytecode against the Paper 1.20.1 API (build with JDK 25; Gradle fetches it). Anything newer than 1.20.1
  is reached only where the server has it.
- Folia-safe scheduling only (no `BukkitScheduler`; `./gradlew check` enforces it). Touch a player, block or entity
  only on the thread that owns it.
- Every database call runs on the plugin's database thread (`DataWriter`); never block a server thread on SQL.
- Prefer small, reviewable diffs over drive-by refactors.
- Match existing package structure and naming.
- Do not commit secrets, `build/`, or generated `graphify-out/` output.

## Licensing

By contributing, you agree that your contributions are licensed under the
Apache License, Version 2.0, unless you clearly state otherwise.
