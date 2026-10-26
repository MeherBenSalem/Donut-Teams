# Donut Teams Patch Notes

## 2.0.0: The Alliance Update

A full rewrite. The full notes are in [DonutTeams-2.0.0-PatchNotes.md](DonutTeams-2.0.0-PatchNotes.md).

* **Ranks**: Owner, Officer and Member, with twelve team permissions the owner can set per member.
* **Allies and enemies**: ally requests with a clickable accept, war declarations, ally chat, no ally damage.
* **Team bank** (Vault) with history, and **team levels** bought from it: more members, homes and chest rows.
* **Team chest**: 1 to 6 shared rows, saved safely, returned to the owner on disband.
* **Named homes** with a title countdown, cancelled by moving or damage.
* **Clickable invites, join requests and open teams.**
* **Kills, deaths and leaderboards**: `/team top`, PlaceholderAPI top lists, and floating top-teams boards in the
  world.
* **Every feature in menus**, all configurable in `menus.yml`, and **native input dialogs** on 1.21.7+.
* Team colour, icon, tag, description and message of the day; staff tools, chat spy and inactive-team purge.
* PlaceholderAPI (including relational placeholders), LuckPerms contexts, MariaDB/MySQL, eight new API events.
* Paper, Folia and Purpur **1.20.1 to 26.3** in one jar, tested by bots on every version.
* Fixes: public chat lost after leaving a team with team chat on, expired invites blocking new ones, underscores in
  names, a possible two-second freeze when inviting, and more.
* 1.x teams, members, homes and settings upgrade themselves on the first start.

---

## 1.0.1

### Platform
* Minecraft **26.3** support (Paper API `26.3.build.49-alpha`, the latest published 26.3 API artifact)
* Boot-tested on Paper **26.3 ALPHA build #133**, Paper 26.2 build 129, Paper 1.20.6 build 151 (oldest supported), and Folia 26.2 build 7
* Folia 26.3 is not published yet (Paper Fill API returns 404)
* Existing support for 1.20.6–26.2, Folia, Bukkit, Spigot, and Purpur is unchanged (`api-version: 1.20`, `folia-supported: true`)
* Sound lookups use the Paper registry with a reflective `valueOf` fallback (`Sound.valueOf` is deprecated for removal in 26.3)
* Compile with JDK 25; the plugin jar still emits Java 21 bytecode

### Upgrade Notes
1. Replace the jar with **1.0.1**. Keep `plugins/DonutTeams/`.
2. Paper / Folia 26.x servers require Java 25; the plugin jar still runs on Java 21 for 1.20.6–1.21.x.
