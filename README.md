# Donut Teams

Folia-first Paper team plugin by Nightbeam Studio. Create teams, manage members
in a GUI, set a team home, use team chat, and control friendly fire.

NightTeams Pro (Redis sync, extra homes, allies, bank, cosmetics) is maintained
in a **separate** repository and is not part of this project.

## Features

- Team create / disband / invite / join / leave / kick / transfer
- Role permissions (invite, kick, home, sethome, speak, pvp)
- One team home with warmup and cancel-on-move
- Team chat and friendly-fire toggle
- SQLite storage (HikariCP)
- Folia-safe scheduling (no `BukkitScheduler`)
- Optional soft-depends: DonutCore, Vault, LuckPerms, PlaceholderAPI
- Public `DonutTeamsApi` and cancelable team events

## Requirements

- Java 21
- Paper 1.20.6+ / 1.21.x / 26.x, or Folia of the same game versions

## Installation

1. Build or download `DonutTeams-1.0.0.jar`.
2. Place it in `plugins/`.
3. Restart the server.
4. Edit `plugins/DonutTeams/config.yml` and `messages.yml` as needed.

## Commands

| Command | Description |
| --- | --- |
| `/team` | Team GUI and subcommands |
| `/team create <name> [tag]` | Create a team |
| `/team disband [confirm]` | Disband (owner) |
| `/team invite <player>` | Invite a player |
| `/team join <team>` | Accept an invite |
| `/team leave` | Leave the team |
| `/team kick <player>` | Kick a member |
| `/team chat [message]` | Toggle or send team chat |
| `/team home` / `sethome` / `delhome` | Team home |
| `/team pvp` | Toggle friendly fire |
| `/team transfer <player>` | Transfer ownership |
| `/donutteams reload` | Reload config and messages |

## Permissions

| Node | Default | Description |
| --- | --- | --- |
| `donutteams.use` | true | Use `/team` |
| `donutteams.create` | true | Create a team |
| `donutteams.chat` | true | Team chat |
| `donutteams.home` | true | Team home teleport |
| `donutteams.admin` | op | Admin / reload |
| `donutteams.slots.8` | true | Member cap of 8 |

Higher `donutteams.slots.<n>` nodes are parsed, but this edition hard-caps at
`teams.lite-max-members` (default 8).

## Build

```bash
./gradlew build
```

Shaded jar: `build/libs/DonutTeams-1.0.0.jar`

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Security

See [.github/SECURITY.md](.github/SECURITY.md).

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
