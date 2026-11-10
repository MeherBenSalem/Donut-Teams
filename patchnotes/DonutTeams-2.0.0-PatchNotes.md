# Donut Teams 2.0.0: The Alliance Update

The biggest Donut Teams update yet: a full rewrite that turns a small team plugin into a complete team system,
still free and open source. Replace the jar and restart: your 1.x teams, members, invites, homes and settings carry
over.

## New

### Ranks
* Three ranks: **Owner**, **Officer** and **Member**. `/team promote` and `/team demote`, or the member menu.
* Twelve team permissions (invite, kick, homes, set homes, chat, friendly fire, deposit, withdraw, chest, diplomacy,
  settings, level up). Each rank has defaults in `config.yml`; the owner can change any member's permissions with
  one click in the member menu.
* Members can only kick, promote or demote ranks below their own.

### Allies and enemies
* `/team ally <team>` asks another team; they accept with the same command or a clickable **[Ally]** button.
* `/team enemy <team>` declares war (no consent needed); `/team neutral <team>` makes peace or ends an alliance.
* **Ally chat**: `/ac <message>` or a toggle, shared by your team and every ally.
* Allies can't hurt each other unless both teams turn ally damage on.
* The allies and enemies menu lists both, plus incoming ally requests.

### Team bank
* `/team deposit 2.5k`, `/team withdraw 1m`, or the bank menu with preset and typed amounts.
* The bank menu shows the latest deposits and withdrawals with who and when.
* Vault or VaultUnlocked. Optional maximum balance. When a team disbands, the owner gets the bank.

### Team levels
* Five levels (configurable, as many as you like). Each sets the member cap, the number of homes and the rows of
  the team chest. Teams buy the next level from their bank.

### Team chest
* `/team chest` (or `/team ec`): a shared chest of 1 to 6 rows, set by the team level.
* On Paper several members can use it at the same time; on Folia, one at a time.
* Saved when it closes and every minute while open. Items are stored in Paper's versioned format, so they survive
  Minecraft updates. When a team disbands, the owner gets the items.

### Homes
* **Named homes**: `/team sethome farm`, `/team home farm`, and a homes menu. The number of homes comes from the
  team level.
* The warmup shows as a title and on the action bar, and is cancelled by moving off your block or taking damage.
* Optional cooldown, sounds and particles, and worlds where homes are disabled.

### Joining
* **Clickable invites**: **[Accept]** and **[Decline]** in chat, and an invites menu.
* **Join requests**: `/team request <team>`; officers get **[Accept]** and **[Deny]** buttons.
* **Open teams**: `/team open` lets anyone `/team join` without an invite.
* Pick online players to invite from a menu of heads.

### Stats and leaderboards
* Player kills and deaths count for the team (teammates and allies don't count). KDR on every team.
* `/team top`: leaderboards by kills, bank, level or members.
* **Top-teams boards in the world**: `/donutteams board create spawn kills`. A floating board that updates itself, no
  hologram plugin needed; `leaderboard.board-scale` sets its size.
* `/team list`: every team, busiest first, with a profile for each (members, relation, join or diplomacy buttons).

### Team identity
* Team **colour** (16 colours), **icon** (hold any item, a banner looks best), **tag**, **description** and a
  **message of the day** shown to members when they join.
* `/team rename` for owners.

### Menus
* Every feature in menus: team, members, member, invites, requests, homes, bank, levels, allies and enemies,
  settings, colours, top teams, all teams, team profiles and confirmations.
* **Everything in `menus.yml`**: titles, sizes, slots, items, names and lore, with paging.
* **Native input dialogs** on Minecraft 1.21.7 and newer: create a team, rename, deposit and more in a real text box
  instead of typing in chat. Older servers use chat prompts.

### Chat
* Team chat and ally chat by command (`/tc`, `/ac`), toggle, or an optional quick prefix (`@message`).
* `/donutteams spy` for staff to read every team and ally chat.
* Teammates are told when members come online and go offline.

### Staff
* `/donutteams info|disband|join|kick|setowner|rename|bank|level|chest|spy|board|purge|version`, all from the
  console too (except opening a chest).
* `/donutteams purge <days>` disbands teams nobody has played on for that long.

### Integrations
* **PlaceholderAPI**: 40+ placeholders, including leaderboards (`%donutteams_top_kills_1_name%`) and
  **relational placeholders** (`%rel_donutteams_relation_color%`) so tab and name tag plugins can colour teammates,
  allies and enemies.
* **LuckPerms contexts**: `donutteams:has-team`, `donutteams:role`, `donutteams:team`.
* **MariaDB and MySQL** storage, with a table prefix, as well as SQLite.
* A bigger developer API: relations, ranks, every team; and eight new cancellable events (disband, rank change,
  relation change, bank, home teleport, level up, rename, chat).
* Update check on Modrinth for staff.

## Fixed (from 1.0.1)
* A player who had team chat on and was then kicked, or whose team was disbanded, could not chat at all: every message
  disappeared.
* An expired invite blocked new invites to the same player until the server restarted.
* `/team create My_Team` failed: the tag was made from the name, underscores included, and refused.
* Inviting while the team owner was offline could freeze the server for up to two seconds (a blocking LuckPerms
  lookup).
* The members menu could open the wrong member, because the order changed between drawing and clicking.
* Toggling friendly fire from the menu showed the old state.
* The settings menu always said the member cap was 8.
* Disbanding from the menu needed two clicks across two menus; there is now a confirmation menu.
* Spigot was listed as supported but the plugin could not run on it. Donut Teams now says clearly that it needs Paper
  or a Paper fork, and stops.

## Compatibility
* Paper, Folia and Purpur (and other Paper forks), Minecraft **1.20.1 to 26.3**, one jar.
* Java 17 for 1.20.1-1.20.4, 21 for 1.20.5-1.21.11, 25 for 26.x (the server's own requirement).
* Every release is tested by bots on real Paper and Folia servers from 1.20.1 to 26.3, including an upgrade from
  1.0.1 and MariaDB storage.

## Upgrading from 1.x
1. Back up `plugins/DonutTeams/`.
2. Replace the jar and restart.
3. On the first start:
   * the database gains the 2.0 tables and columns; teams, members, invites, homes and friendly fire are kept, and
     the 1.x home is called `home`;
   * members get the permissions their rank has in 2.0 for the new features (bank, chest);
   * `config.yml` is upgraded with your values (the old file is kept as `config-1.x.yml`);
   * `messages.yml` is replaced with the 2.0 messages (yours is kept as `messages-1.x.yml`), and `menus.yml` is added.
4. The 8-member "lite" cap is gone: member caps now come from team levels and `donutteams.slots.<n>`.
