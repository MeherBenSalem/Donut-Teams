# Donut Teams Patch Notes

## 1.0.1

### Platform
* Minecraft **26.3** support (Paper API `26.3.build.49-alpha`, the latest published 26.3 API artifact)
* Folia 26.3 is not published yet
* Existing support for 1.20.6–26.2, Folia, Bukkit, Spigot, and Purpur is unchanged (`api-version: 1.20`, `folia-supported: true`)
* Sound lookups use the Paper registry with a reflective `valueOf` fallback (`Sound.valueOf` is deprecated for removal in 26.3)
* Compile with JDK 25; the plugin jar still emits Java 21 bytecode

### Upgrade Notes
1. Replace the jar with **1.0.1**. Keep `plugins/DonutTeams/`.
2. Paper / Folia 26.x servers require Java 25; the plugin jar still runs on Java 21 for 1.20.6–1.21.x.
