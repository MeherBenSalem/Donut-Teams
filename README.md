# Donut Teams

Folia-first Paper team plugin by **Nightbeam Studio** (NAIZO / MeherBenSalem). Companion to Donut Auction, Orders, RTP, and Shards.

This repository ships **lite v1.0.0** only. Pro (Redis, extra/personal homes, cosmetics, tablist, allies, team bank) is a later SKU. Lite is still Nightbeam IP — see `LICENSE`.

## Requirements

- Java 21
- Paper 1.20.6+ / 1.21.x / 26.x, or Folia of the same game versions
- Optional soft-depends: DonutCore, Vault, LuckPerms, PlaceholderAPI (never required)

## Build

```bash
./gradlew build
```

The shaded plugin jar is:

```
build/libs/DonutTeams-1.0.0.jar
```

Drop that jar into `plugins/` and restart.

## Commands

| Command | Description |
| --- | --- |
| `/team` | Opens the team GUI (create / members / info / home / pvp / settings) |
| `/team create <name> [tag]` | Create a team |
| `/team disband [confirm]` | Owner disbands the team |
| `/team invite <player>` | Invite an online player |
| `/team join <team>` | Accept an invite |
| `/team leave` | Leave (owners must transfer or disband) |
| `/team kick <player>` | Kick a member |
| `/team info [team]` | Team overview |
| `/team chat [message]` | Toggle team chat, or send one message |
| `/team home` | Teleport to the team home (warmup, cancel-on-move) |
| `/team sethome` | Set the single team home |
| `/team delhome` | Delete the team home |
| `/team pvp` | Toggle friendly fire |
| `/team transfer <player>` | Transfer ownership |
| `/donutteams reload` | Reload `config.yml` and `messages.yml` |

## Permissions

| Node | Default | Description |
| --- | --- | --- |
| `donutteams.use` | true | Use `/team` |
| `donutteams.create` | true | Create a team |
| `donutteams.chat` | true | Use team chat |
| `donutteams.home` | true | Teleport to team home |
| `donutteams.admin` | op | `/donutteams reload` |
| `donutteams.slots.8` | true | Lite member cap of 8 |

The plugin also parses the **highest** `donutteams.slots.<n>` node the owner has. Lite still hard-caps at 8 (`teams.lite-max-members`).

### Team roles (not Bukkit nodes)

- **Owner** has every team permission.
- **Members** default to **speak only**.
- Owner can grant: `invite`, `kick`, `home`, `sethome`, `speak`, `pvp` (GUI member editor).

## Folia notes

- `folia-supported: true` in `plugin.yml`.
- **No `BukkitScheduler`.** Paper regional APIs only: player entity scheduler for GUI/teleport/warmup, region scheduler for location work, global scheduler when needed, async scheduler for SQL.
- Friendly-fire is handled on the **victim region thread** (`EntityDamageByEntityEvent`). Teammate checks use UUID cache only — the attacker entity is not mutated.
- Team chat is captured on `AsyncChatEvent`, then bounced to the speaker's entity scheduler.
- Use `player.teleportAsync` for home teleports.

## Storage

SQLite by default (`plugins/DonutTeams/teams.db`). Optional MySQL via HikariCP.

Tables: `teams`, `members`, `invites`, `homes`, `settings`.

All SQL runs off-thread. Teams are cached in memory (Caffeine + concurrent maps).

## PlaceholderAPI (best-effort)

| Placeholder | Value |
| --- | --- |
| `%donutteams_name%` | Team name |
| `%donutteams_tag%` | Team tag |
| `%donutteams_count%` | Member count |
| `%donutteams_home_world%` | Home world |
| `%donutteams_leader%` | Owner name |

## Public API

For a future Homes SKU (and other Nightbeam plugins):

```java
DonutTeamsApi api = DonutTeams.getApi();
api.teamByPlayer(playerId);
```

Cancelable events: `TeamCreateEvent`, `TeamJoinEvent`, `TeamLeaveEvent`.

## Lite vs future Pro

| Feature | Lite | Pro (later) |
| --- | --- | --- |
| Team GUI + roles | yes | yes |
| One team home | yes | yes |
| Extra named homes / personal homes | no | planned |
| Team chat + friendly fire | yes | yes |
| SQLite / MySQL | yes | yes |
| Redis sync | stub only | planned |
| Allies / wars / claims / bank | no | planned |
| Nametags / tablist / color prefixes | no | planned |
| BBB license checks | no | planned |

Pro stub keys live under `pro:` in `config.yml` and as locked items in the settings GUI. They do nothing in lite.

## Configuration

- `config.yml` — storage, member cap, home warmup, Pro stubs
- `messages.yml` — MiniMessage strings

## License

All Rights Reserved © 2026 Nightbeam Studio / NAIZO / MeherBenSalem.
