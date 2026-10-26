![Donut Teams 2.0: The Alliance Update](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/banner.png)

## Teams your players will actually build around

**Donut Teams** is a free, open-source team plugin for Paper, Folia and Purpur, from Minecraft **1.20.1 to 26.3**.

Players start a team in a few seconds (on 1.21.7+ in a **native input dialog**), invite friends with a clickable
**[Accept]**, and share **homes, a bank and a chest**. Teams **rank up** their members, **level up** for more room,
make **allies and enemies**, and race up **leaderboards that stand in your world**. Everything works from menus you can
reshape in `menus.yml`.

**2.0 "The Alliance Update"** is a full rewrite:
- ranks and twelve per-member permissions;
- allies, enemies, ally chat and no ally damage;
- a team bank, team levels and a shared team chest;
- named homes with a title countdown;
- clickable invites, join requests and open teams;
- kills, leaderboards and floating top-teams boards;
- menus for everything, native dialogs, PlaceholderAPI with relational placeholders, LuckPerms contexts and
  MariaDB/MySQL.

Each release is played by bots on 15 Paper and Folia servers from 1.20.1 to 26.3, including upgrades from 1.0.1.

![Creating a team in the native dialog](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/gifs/create.gif)

![divider](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/divider.png)

![The Team Menu](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-menu.png)

![Your whole team in one menu](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-menu.png)

`/team` opens one menu with everything: members, homes, the bank, the chest, allies and enemies, levels, the
leaderboard, team chat and settings. Players without a team get a menu to create one, see their invites, browse
every team or check the top teams.

- **Every menu is in `menus.yml`**: titles, sizes, slots, items, names and lore, with pages for long lists.
- Commands exist for everything too, with tab completion: `/team invite`, `/team home`, `/team deposit 2.5k`...
- On **1.21.7 and newer**, names and amounts are typed in a **native dialog** instead of chat.

![Ranks](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-ranks.png)

![Ranks and permissions](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-ranks.png)

- **Owner, Officer and Member.** `/team promote` and `/team demote`, or two clicks in the member menu.
- **Twelve permissions**: invite, kick, use homes, set homes, team chat, friendly fire, deposit, withdraw, chest,
  diplomacy, settings and level up. Each rank has defaults in `config.yml`; the owner can switch any of them for any
  member.
- Members can only kick, promote or demote ranks below their own. Ownership changes with `/team transfer`.

![Allies & Enemies](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-allies.png)

![Allies and enemies](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-allies.png)

- `/team ally Sunfire` asks; they accept with a **clickable [Ally]** button. `/team enemy Ravens` declares war, no
  consent needed. `/team neutral` makes peace.
- **Ally chat** (`/ac`) reaches your team and every ally.
- **Allies can't hurt each other**, unless both teams switch ally damage on. Teammates are protected the same way:
  melee, arrows, tridents, pets and harmful potions.
- Relational placeholders colour teammates green, allies aqua and enemies red in your tab list and name tags.

![Bank & Levels](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-bank.png)

![Team bank and levels](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-bank.png)

- **A team bank** through Vault or VaultUnlocked: `/team deposit 2.5k`, `/team withdraw 1m`, or preset and typed
  amounts in the bank menu, with the latest moves and who made them.
- **Levels** bought from the bank: each one raises the member cap, the number of homes and the rows of the team chest.
  Five by default, as many as you like.
- Teams can also cost money to create. When a team disbands, the owner gets the bank.

![Team Chest](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-chest.png)

![A shared team chest](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-chest.png)

- `/team chest` (or `/team ec`): one to six rows, set by the team level.
- On Paper several teammates can use it at once; on Folia, one at a time, so nothing is ever duplicated.
- Saved when it closes and every minute while open, in Paper's versioned item format, so items survive Minecraft
  updates. Disbanding gives the items to the owner.

![Team Homes](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-homes.png)

![Team homes](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-homes.png)

- **Named homes**: `/team sethome farm`, `/team home farm`, and a homes menu. The team level sets how many.
- A countdown on a title and the action bar. **Moving off your block or taking damage cancels it.**
- Optional cooldown, sounds, particles, and worlds where team homes are off.

![Team homes](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/gifs/home.gif)

![Joining](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-joining.png)

![Invites and requests](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-joining.png)

- **Clickable invites**: **[Accept]** and **[Decline]** right in chat, and an invites menu.
- **Join requests**: `/team request Nightbeam`; officers get **[Accept]** and **[Deny]** buttons.
- **Open teams**: `/team open` and anyone can `/team join`.
- Officers invite from a menu of online players' heads.

![Chat](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-chat.png)

![Team and ally chat](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-chat.png)

- Team chat and ally chat: `/tc` and `/ac` with a message, as a toggle, or with an optional quick prefix (`@hello`).
- Teammates are told when members come online or leave, and see the team's **message of the day** when they join.
- Staff can read every team and ally chat with `/donutteams spy`.

![Leaderboards](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-leaderboards.png)

![Leaderboards](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-leaderboards.png)

- Player kills and deaths count for the team (teammates and allies don't). KDR for every team.
- `/team top`: the best teams by kills, bank, level or members.
- **Boards in the world**: `/donutteams board create spawn kills` puts up a floating top-teams board that updates
  itself, at the size you set. No hologram plugin needed.
- Leaderboards for your scoreboards and holograms: `%donutteams_top_kills_1_name%`.

![Make It Yours](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-yours.png)

![Make it yours](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-yours.png)

- Teams pick a **colour**, an **icon** (hold any item, banners look great), a **tag**, a **description** and a
  **message of the day**.
- You set the rules: name length, blocked words, create cost, rank permissions, levels, home warmup and cooldown,
  ally limits, the bank cap.
- Every message is MiniMessage (or `&` codes) in `messages.yml`; every menu is in `menus.yml`; every sound is in
  `config.yml`.

![Integrations](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-integrations.png)

![Integrations](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-integrations.png)

All of these are optional:

| Plugin | What Donut Teams does with it |
| --- | --- |
| Vault (or VaultUnlocked) | Team bank, levels, create cost |
| PlaceholderAPI | 40+ placeholders, leaderboards, and relational ones for tab and name tags |
| LuckPerms | Contexts: `donutteams:has-team`, `donutteams:role`, `donutteams:team` |
| MariaDB or MySQL | Storage instead of SQLite, with a table prefix |
| DonutCore | Display names |

**For developers**: `DonutTeamsApi` (teams, members, ranks, relations) and eleven cancellable events, from
`TeamCreateEvent` to `TeamRelationChangeEvent`, `TeamBankTransactionEvent` and `TeamHomeTeleportEvent`.

![Upgrading from 1.x](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-upgrade.png)

![Upgrade in place](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-upgrade.png)

Replace the jar and restart. Donut Teams upgrades your 1.x install on the first start:
- **Teams, members, invites and homes are kept.** The 1.x home is now called `home`; friendly fire settings stay.
- `config.yml` keeps your values (the old file is saved as `config-1.x.yml`).
- `messages.yml` is replaced with the new messages (yours is saved as `messages-1.x.yml`).
- The 8-member cap of 1.x is gone: levels and `donutteams.slots.<n>` set it now.

![Compatibility](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-compatibility.png)

![Tested from 1.20 to 26.3](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/card-compatibility.png)

| Server | Minecraft | Java |
| --- | --- | --- |
| Paper, Purpur and other Paper forks | 1.20.1 – 1.20.4 | 17 |
| Paper, Purpur and other Paper forks | 1.20.5 – 1.21.11 | 21 |
| Paper, Purpur and other Paper forks | 26.1 – 26.3 | 25 |
| Folia | 1.20.1 – 26.3 | 17 – 25 |

One jar for all of them. Native dialogs need 1.21.7 or newer; older servers ask in chat. Spigot and Bukkit are not
supported (they lack Paper's API): the plugin says so and stops.

Each release is played by bots on Paper 1.20.1, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.8, 1.21.11, 26.1.2, 26.2 and
26.3 and Folia 1.20.4, 1.21.4, 1.21.11 and 26.3, with SQLite and MariaDB: about 30 checks each, from creating a team
to disbanding it, including an upgrade from 1.0.1 and a restart. The console must stay free of errors.

![Commands & Permissions](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-commands.png)

| Command | |
| --- | --- |
| `/team` | The team menu |
| `/team create [name] [tag]` | Start a team |
| `/team invite <player>`, `join <team>`, `request <team>`, `accept <player>`, `deny <player>` | Joining |
| `/team leave`, `kick`, `promote`, `demote`, `transfer <player>`, `disband` | Members |
| `/team chat [msg]` (`/tc`), `allychat [msg]` (`/ac`) | Chat |
| `/team home [name]`, `sethome [name]`, `delhome [name]`, `homes` | Homes |
| `/team bank`, `deposit <amount>`, `withdraw <amount>`, `level [up]`, `chest` | Bank, levels, chest |
| `/team ally <team>`, `enemy <team>`, `neutral <team>`, `relations` | Diplomacy |
| `/team pvp`, `allypvp`, `open`, `color`, `icon`, `tag`, `rename`, `description`, `motd` | Settings |
| `/team info [team]`, `list`, `top [kills\|balance\|level\|members]` | Info and leaderboards |
| `/donutteams reload\|info\|disband\|join\|kick\|setowner\|rename\|bank\|level\|chest\|spy\|board\|purge` | Staff |

| Permission | Default | |
| --- | --- | --- |
| `donutteams.use`, `.create`, `.chat`, `.home` | everyone | The player features |
| `donutteams.slots.<n>` | nobody | The owner's team can have `n` members |
| `donutteams.bypass.cost`, `.cooldown` | op | Free teams, no home cooldown |
| `donutteams.bypass.warmup`, `.friendlyfire` | nobody | Instant homes, hurt teammates anyway |
| `donutteams.admin`, `donutteams.admin.spy` | op | Staff commands, chat spy |

![Questions](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-faq.png)

**Does it need Vault?** Only for the bank, levels that cost money, and a create cost. Everything else works without
it.

**Folia?** Yes, every feature. The team chest is used by one player at a time on Folia.

**Can I change the menus?** Every title, slot, item, name and lore line is in `menus.yml`. Set a slot to `-1` to hide
an item.

**How do I colour teammates in the tab list?** Use `%rel_donutteams_relation_color%` in a tab or name tag plugin that
supports relational placeholders.

**Several servers?** Use one database per server. Teams shared across a network are NightTeams Pro's job.

**Will my 1.x teams survive?** Yes. They are upgraded on the first start; back up `plugins/DonutTeams/` first anyway.

![Gallery](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/header-gallery.png)

![The team menu](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/screenshots/menu-main.jpg)

![Members](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/screenshots/menu-members.jpg)

![The team chest](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/screenshots/chest.jpg)

![A top-teams board at the base](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/screenshots/board.jpg)

![Team chat](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/screenshots/team-chat.jpg)

![divider](https://raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/divider.png)

**Discord**: [discord.gg/TJc3CxR4jK](https://discord.gg/TJc3CxR4jK) · **Source**:
[GitHub](https://github.com/MeherBenSalem/Donut-Teams) (Apache 2.0) · **Support the project**:
[nightbeam.dev/support](https://nightbeam.dev/support)
