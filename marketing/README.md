# Marketing assets: 2.0 "The Alliance Update"

Listing copy and artwork for CurseForge, Modrinth and Hangar. Nothing here ships in the plugin jar.

## Listing

| File | Use |
| --- | --- |
| [`curseforge/description.md`](curseforge/description.md) | Full project description (Markdown; also works on Modrinth and Hangar) |
| [`curseforge/summary.txt`](curseforge/summary.txt) | Short project summary |
| [`images/icon.png`](images/icon.png) | Project avatar |
| [`../DonutTeams-2.0.0-PatchNotes.md`](../DonutTeams-2.0.0-PatchNotes.md) | File changelog the publish workflow uploads |

Image links in the description point at `raw.githubusercontent.com/MeherBenSalem/Donut-Teams/main/marketing/images/...`,
so they work once this folder is on `main`. To host on CurseForge instead, upload the images to the project gallery and
replace each URL.

**Modrinth**: the *Sync Modrinth listing* workflow (`.github/workflows/listing.yml`, run by hand) sets the summary,
description, icon and gallery from these files. **CurseForge** has no API for the description: paste `description.md`
into the project's description editor (Markdown) and `summary.txt` into its summary.

The artwork shares the pixel donut, fonts and dark backdrop of the other Donut plugins (DonutRTP, DonutAuctionHouse,
DonutShards, DonutOrders). Donut Teams' donut has mint icing.

## Adding a Minecraft version

The supported range comes from [`release/supported-minecraft.json`](../release/supported-minecraft.json), which the
publish workflow also uses. The banner and the compatibility card read it, so a new version needs no artwork edits:

1. Add the version to `release/supported-minecraft.json` and to `MATRIX` in
   [`tools/smoke/run.mjs`](../tools/smoke/run.mjs).
2. Run the smoke matrix, then `node tools/smoke/compat.mjs` to refresh `source/compat.json`.
3. Rerun the renderer for `banner compatibility`.

A new release name or number goes in `RELEASE` in `source/render.cjs` and in the first lines of the description.

## Images

| File | Size | Use |
| --- | --- | --- |
| `images/banner.png` | 1800×600 | Description header: the team menu, "2.0 · The Alliance Update", platforms and versions |
| `images/icon.png` | 512×512 | Project avatar: the pixel donut with a "TEAMS" tag |
| `images/card-*.png` | 1600×900 | Feature cards |
| `images/header-*.png` | 1400×120, transparent | Section headings for the description |
| `images/divider.png` | 1400×40, transparent | Section divider |
| `images/gifs/create.gif` | 960×540 | Creating a team in the native dialog |
| `images/gifs/home.gif` | 640×360 | A team home teleport, from the countdown to the base |
| `images/screenshots/*.jpg` | 1920×1080 | Gallery uploads and card art |

### How the screenshots were captured

Every screenshot and GIF is an unedited capture of the **vanilla Minecraft 1.21.11 client**, launched with
portablemc, on a **Paper 1.21.11** server running Donut Teams 2.0.0.

- The client ran in a 1920×1080 Xvfb display with software rendering (Mesa llvmpipe) at GUI scale 3.
- It was driven by [`tools/marketing/capture.mjs`](../tools/marketing/capture.mjs) and
  [`scenes.mjs`](../tools/marketing/scenes.mjs) through console commands and XTEST mouse and keyboard input.
- **The world** is seed 8675309 on peaceful, at noon. A small team camp was built in the nearest cherry grove with
  `/fill`.
- **The players**: Moonbeam is the real client. Kiri, Pixel, Ember, Sol, Nova, Rook, Vex and Ash are mineflayer bots,
  so they wear the default skins.
- **Real actions**: every team, invite, rank, deposit, level, alliance, war, home teleport and chest item in the shots
  happened through the plugin, typed by Moonbeam or the bots (`teamlab sudo` runs a command as that player). The menus
  were clicked with the mouse and the create dialog was typed with the keyboard.
- **Set up with the console**: the camp, player positions, the bots' money (the smoke fixture's in-memory Vault
  economy), the items Moonbeam put in the chest, the in-world board (`/donutteams board create`, typed by Moonbeam as
  an op), and the kills behind the leaderboard (`teamlab kill`, a real player-on-player kill the plugin counts).
- **The upgrade card** shows the real console of a separate server that ran Donut Teams 1.0.1 first.
- **The GIFs** are real-time recordings (x11grab at 12 fps): the create dialog is the whole screen at half size, the
  home teleport the centre 1280×720 at half size.

## Regenerating the artwork

```bash
./gradlew build
./gradlew -p tools/smoke/fixture jar
node tools/marketing/capture.mjs                     # screenshots, GIFs and console text (about 25 minutes)
node tools/marketing/capture.mjs --upgrade-only      # just the 1.0.1 → 2.0 console
node tools/marketing/capture.mjs --scenes=base,invite,create   # just some scenes (see SCENES in scenes.mjs)
cp marketing/images/screenshots/scenery.jpg marketing/source/backdrop.jpg
python3 tools/marketing/crops.py                      # crop boxes of the menus and tooltips
node tools/smoke/compat.mjs                           # the compatibility card's data
node marketing/source/render.cjs                      # every image, or name the ones to render
```

`capture.mjs --lab` starts the server and client and takes JavaScript on `http://127.0.0.1:8899` to try a scene by
hand (the comment at the top of `scenes.mjs` shows how). `render.cjs` needs Playwright with Chromium. The fonts are Outfit and Inter (SIL Open Font License 1.1, in
`source/fonts/`).
