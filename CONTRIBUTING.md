# Contributing to Donut Teams

Thanks for helping improve Donut Teams.

## How to contribute

1. Fork the repository and create a topic branch from `main`.
2. Make focused changes with clear commit messages.
3. Add or update tests when behavior changes.
4. Run `./gradlew build` (or `gradlew.bat build` on Windows) before opening a PR.
5. Open a pull request describing the problem and the fix.

## Coding expectations

- Java 21, Folia-safe scheduling (no `BukkitScheduler`).
- Prefer small, reviewable diffs over drive-by refactors.
- Match existing package structure and naming.
- Do not commit secrets, `build/`, or generated `graphify-out/` output.

## Licensing

By contributing, you agree that your contributions are licensed under the
Apache License, Version 2.0, unless you clearly state otherwise.
