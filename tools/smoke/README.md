# Cross-version smoke matrix

Boots real Paper and Folia servers across the supported range and plays Donut Teams with
[mineflayer](https://github.com/PrismarineJS/mineflayer) bots over the network protocol. Every check goes through the
real plugin, a real economy (the fixture's Vault economy), PlaceholderAPI and the server's own storage.

| Target | Java | Bot protocol | Extra |
| --- | --- | --- | --- |
| Paper 1.20.1, 1.20.4 | 17 | same version | |
| Paper 1.20.6 | 21 | same version | starts from Donut Teams 1.0.1 |
| Paper 1.21.1, 1.21.4, 1.21.8 | 21 | same version | |
| Paper 1.21.11 | 21 | same version | starts from 1.0.1, restarts mid-test |
| Paper 1.21.11 + MariaDB | 21 | same version | MariaDB storage, restarts mid-test |
| Paper 26.1.2, 26.2 | 25 | 26.1 through ViaVersion + ViaBackwards | |
| Paper 26.3 | 25 | as above | starts from 1.0.1 |
| Folia 1.20.4, 1.21.4 | 17 / 21 | same version | |
| Folia 1.21.11 | 21 | same version | restarts mid-test |
| Folia 26.3 | 25 | as above | starts from 1.0.1 |

**Starting from 1.0.1**: the server first runs Donut Teams 1.0.1 (from Modrinth). Two bots make a team, a home, and
turn friendly fire on. Then the 2.0 jar replaces it: the database, `config.yml` and `messages.yml` must upgrade, with
backups, and the team must come back intact.

Then `scenario.mjs` runs on every server:

1. The plugin enables on the right platform with the right storage and economy.
2. `Bob` creates **Nightbeam [NB]**. The team menu opens with the team banner and member heads; a click opens the
   members menu.
3. `Cara` gets an invite whose **[Accept]** button runs `/team join Nightbeam`, and joins. Bob promotes her to
   Officer; she invites and kicks `Dan`.
4. Team chat reaches the team and nobody else. `Dan`, kicked with team chat switched on, can still talk in public
   chat (1.x swallowed his messages).
5. Friendly fire: Bob cannot hurt Cara until `/team pvp`, then can.
6. The bank: a deposit of `30k` and a withdrawal of `5000` move exactly that money. `/team level up` spends it.
7. Homes: `/team sethome base`, then Cara's `/team home base` lands her on it after the warmup. Damage and being
   teleported cancel the warmup.
8. The chest: 5 diamonds go in with real clicks, the window closes, and they are there when it reopens (two rows at
   level 2).
9. Diplomacy: `Alice`'s team asks to ally (with a clickable button); Bob accepts. Ally chat works and allies can't
   hurt each other. `%donutteams_*%` and relational `%rel_donutteams_relation%` placeholders are right.
10. `Eve`'s team **Rivals** declares war and kills Bob: Rivals has 1 kill, Nightbeam 1 death. The leaderboard
    placeholder and the top menu put Rivals first, and a floating board appears in the world.
11. `Frank` asks to join and Bob accepts; after `/team open`, `Gus` joins without an invite.
12. The bank menu's deposit button works; colour, description, icon (held banner) and message of the day are set.
    Ownership goes to Cara.
13. On restart targets the server restarts: teams, relations, stats, bank and the chest's items all come back.
14. Cara disbands: she gets the $100 bank and the 5 diamonds from the chest.
15. `/donutteams info`, `disband`, `reload` and `version` answer on the console.
16. The console is scanned for errors and warnings from the plugin, stack frames in `io.nightbeam.donutteams`,
    and linkage errors.

## Running it

```bash
./gradlew build
./gradlew -p tools/smoke/fixture jar
npm --prefix tools/smoke install
node tools/smoke/run.mjs                           # all targets (about 30 minutes)
node tools/smoke/run.mjs paper-1.20.1 folia-26.3   # selected targets
```

The MariaDB target needs a local server with a `donutteams` database and user (password in
`$TEAMLAB_MARIADB_PASSWORD`, default `teams`). JDKs, server jars and plugins are downloaded on first use and cached in
`$TEAMLAB_HOME` (default `~/.cache/donutteams-smoke`). Servers listen on `$TEAMLAB_PORT` (default 25610).

A JSON report is written to `$TEAMLAB_HOME/reports/`. `node tools/smoke/compat.mjs` turns the newest report into
`marketing/source/compat.json` for the compatibility card.

`fixture/` is a test-only plugin, never to be installed on a real server. It provides an in-memory Vault economy, a
listener that prints Donut Teams' API events, and console helpers under `teamlab`: `money`, `balance`, `perm`, `pos`,
`tp`, `sudo`, `hit`, `kill`, `damage`, `give`, `hold`, `count`, `papi`, `rel`, `team`, `teams`, `displays`, `set`,
`yaml` and `file`.
