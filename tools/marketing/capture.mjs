// Captures the listing screenshots and GIFs from a real game: a Paper server running Donut Teams and the unmodified
// vanilla Minecraft client (software rendering under Xvfb), driven through console commands and XTEST mouse and
// keyboard input. mineflayer bots are the teammates, allies and rivals.
//
// Needs: Xvfb, ImageMagick (import), ffmpeg, python3 with python-xlib, portablemc, and the smoke-test setup
// (tools/smoke, its fixture built). Run from the repository root after ./gradlew build:
//   node tools/marketing/capture.mjs              # every scene (about 25 minutes)
//   node tools/marketing/capture.mjs --lab        # start everything and take commands on http://127.0.0.1:8899
//   node tools/marketing/capture.mjs --scenes=base,invite,create   # just those scenes
import { spawn, execFileSync } from 'node:child_process';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Server, joinBot, jdk, serverJar, pluginJar, economyJar, modrinth, fixtureJar } from '../smoke/run.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, '..', '..');
const lab = process.env.TEAMLAB_HOME || path.join(os.homedir(), '.cache', 'donutteams-smoke');
const PORT = Number(process.env.CAPTURE_PORT || 25620);
const VERSION = process.env.CAPTURE_VERSION || '1.21.11';
const PLAYER = 'Moonbeam';
const DISPLAY = process.env.CAPTURE_DISPLAY || ':99';
const W = 1920;
const H = 1080;
const SCALE = 3;
const shots = path.join(repo, 'marketing', 'images', 'screenshots');
const gifs = path.join(repo, 'marketing', 'images', 'gifs');
const consoleDir = path.join(repo, 'marketing', 'source', 'console');
const python = process.env.CAPTURE_PYTHON || path.join(lab, 'venv', 'bin', 'python3');
const portablemc = process.env.PORTABLEMC || path.join(lab, 'venv', 'bin', 'portablemc');
const clientDir = process.env.CAPTURE_CLIENT_DIR || path.join(lab, 'client');

// Noon, clear sky, no command echo in chat. 1.21.11 renamed the game rules, so both spellings are sent.
const WORLD_SETUP = ['time set 6000', 'weather clear',
  'gamerule advance_time false', 'gamerule advance_weather false', 'gamerule show_advancement_messages false', 'gamerule send_command_feedback false',
  'gamerule doDaylightCycle false', 'gamerule doWeatherCycle false', 'gamerule announceAdvancements false', 'gamerule sendCommandFeedback false',
  'gamerule spawn_mobs false', 'gamerule doMobSpawning false'];

export const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
export const x = (...args) => execFileSync(python, [path.join(here, 'xinput.py'), ...args.map(String)], { env: { ...process.env, DISPLAY } });

/** Pixel centre of a slot in a chest menu with the given number of rows (GUI scale 3). */
export function slot(index, rows) {
  const left = Math.floor((W / SCALE - 176) / 2);
  const top = Math.floor((H / SCALE - (114 + rows * 18)) / 2);
  if (index >= rows * 9) {
    // The player's inventory: 3 rows, then the hotbar 4 px lower.
    const inv = index - rows * 9;
    const row = Math.floor(inv / 9);
    const y = top + 18 + rows * 18 + 13 + (row < 3 ? row * 18 : 3 * 18 + 4);
    return [(left + 8 + (inv % 9) * 18 + 8) * SCALE, (y + 8) * SCALE];
  }
  return [(left + 8 + (index % 9) * 18 + 8) * SCALE, (top + 18 + Math.floor(index / 9) * 18 + 8) * SCALE];
}

export function screenshot(name, quality = 92) {
  fs.mkdirSync(shots, { recursive: true });
  const file = path.join(shots, `${name}.jpg`);
  execFileSync('import', ['-display', DISPLAY, '-window', 'root', '-quality', String(quality), file]);
  console.log(`  captured ${path.relative(repo, file)}`);
  return file;
}

export async function hover(index, rows = 6, settle = 800) {
  const [px, py] = slot(index, rows);
  x('move', px - 9, py - 9);
  await sleep(120);
  x('move', px, py);
  await sleep(settle);
}

export async function clickSlot(index, rows = 6, settle = 900) {
  const [px, py] = slot(index, rows);
  x('click', px, py, 1);
  await sleep(settle);
}

export const park = async () => {
  x('move', 60, 60);
  await sleep(700);
};

/** Records the screen for `seconds` (x11grab); returns a promise for the finished MP4. */
export function record(name, seconds) {
  fs.mkdirSync(path.join(lab, 'capture'), { recursive: true });
  const raw = path.join(lab, 'capture', `${name}.mp4`);
  const proc = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'x11grab', '-video_size', `${W}x${H}`, '-framerate', '20', '-i', `${DISPLAY}.0`,
    '-t', String(seconds), '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '16', raw], { stdio: 'ignore' });
  return new Promise((resolve) => proc.on('exit', () => resolve(raw)));
}

/**
 * A GIF from recordings played back to back, by default the centre 1280x720 scaled to 640x360.
 * Parts: { raw, start?, length? }.
 */
export function gif(parts, name, crop = '1280:720:320:180', size = '640:360') {
  fs.mkdirSync(gifs, { recursive: true });
  const file = path.join(gifs, `${name}.gif`);
  const inputs = parts.flatMap(({ raw, start = 0, length = null }) =>
    [...(start ? ['-ss', start.toFixed(2)] : []), ...(length ? ['-t', length.toFixed(2)] : []), '-i', raw]);
  const joined = parts.map((_, i) => `[${i}:v]`).join('') + `concat=n=${parts.length}:v=1:a=0,`;
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', ...inputs, '-filter_complex',
    `${joined}crop=${crop},fps=12,scale=${size}:flags=lanczos,split[a][b];[a]palettegen=max_colors=160:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle`,
    file]);
  console.log(`  recorded ${path.relative(repo, file)}`);
}

function writeClientOptions() {
  fs.mkdirSync(clientDir, { recursive: true });
  fs.writeFileSync(path.join(clientDir, 'options.txt'), [
    'version:4671', 'guiScale:3', 'renderDistance:8', 'simulationDistance:5', 'maxFps:30', 'graphicsMode:1',
    'pauseOnLostFocus:false', 'narrator:0', 'tutorialStep:none', 'skipMultiplayerWarning:true', 'joinedFirstServer:true',
    'onboardAccessibility:false', 'lang:en_us', 'gamma:1.0', 'fov:0.0', 'bobView:false', 'chatScale:1.0', 'chatWidth:1.0',
    'soundCategory_master:0.0', 'darkMojangStudiosBackground:true', 'hideServerAddress:true', 'renderClouds:"fancy"',
    'entityShadows:true', 'particles:0', 'mipmapLevels:4', 'ao:true', 'menuBackgroundBlurriness:3', 'chatLineSpacing:0.0',
  ].join('\n') + '\n');
}

/** The shipped config.yml with what the shots show: a title countdown, quick leaderboards, native dialogs. */
function captureConfig() {
  let config = fs.readFileSync(path.join(repo, 'src', 'main', 'resources', 'config.yml'), 'utf8');
  config = config.replace(/(  refresh-seconds: )30/, '$15').replace(/(board-refresh-seconds: )30/, '$15')
    .replace(/(update-check:\n  enabled: )true/, '$1false').replace(/(chat:\n  enabled: true\n  ally-chat: true\n  # .*\n  quick-prefix: )""/, '$1"@"');
  return config;
}

async function prepareServer(java) {
  const dir = path.join(lab, 'capture', 'server');
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(path.join(dir, 'plugins', 'DonutTeams'), { recursive: true });
  fs.writeFileSync(path.join(dir, 'eula.txt'), 'eula=true\n');
  fs.writeFileSync(path.join(dir, 'server.properties'), [
    'online-mode=false', 'white-list=false', 'enforce-secure-profile=false', `server-port=${PORT}`, 'level-seed=8675309',
    'difficulty=peaceful', 'spawn-monsters=false', 'view-distance=8', 'simulation-distance=5', 'spawn-protection=0',
    'motd=Donut Teams', 'gamemode=survival', 'allow-flight=true', 'pvp=true',
  ].join('\n') + '\n');
  for (const jar of [pluginJar(), fixtureJar(), await economyJar('Vault'), await modrinth('placeholderapi')]) {
    fs.copyFileSync(jar, path.join(dir, 'plugins', path.basename(jar)));
  }
  fs.writeFileSync(path.join(dir, 'plugins', 'DonutTeams', 'config.yml'), captureConfig());
  return { dir, server: new Server(dir, java, await serverJar('paper', VERSION)) };
}

/** Starts the server, Xvfb and the client; resolves once the client has joined. */
export async function boot() {
  const java = await jdk(21);
  const { server } = await prepareServer(java);
  server.start();
  await server.waitFor(/Done \([0-9.,]+s\)!/, 300_000, 'server start');
  for (const command of WORLD_SETUP) server.command(command);
  const xvfb = spawn('Xvfb', [DISPLAY, '-screen', '0', `${W}x${H}x24`, '-nolisten', 'tcp'], { stdio: 'ignore' });
  await sleep(1500);
  writeClientOptions();
  const client = spawn(portablemc, ['--main-dir', clientDir, 'start', `mojang:${VERSION}`, '-u', PLAYER, '--join-server', '127.0.0.1',
    '--join-server-port', String(PORT), '--resolution', `${W}x${H}`, '--jvm', java, '--jvm-arg=-Xmx3G'],
  { env: { ...process.env, DISPLAY, LIBGL_ALWAYS_SOFTWARE: '1', __GL_SYNC_TO_VBLANK: '0' }, stdio: ['ignore', 'pipe', 'pipe'] });
  fs.mkdirSync(path.join(lab, 'capture'), { recursive: true });
  const clientLog = fs.createWriteStream(path.join(lab, 'capture', 'client.log'));
  client.stdout.pipe(clientLog);
  client.stderr.pipe(clientLog);
  await server.waitFor(new RegExp(`${PLAYER} joined the game`), 600_000, 'client join');
  console.log('client joined');
  const stop = async () => {
    client.kill('SIGTERM');
    await sleep(2000);
    await server.stop();
    xvfb.kill('SIGTERM');
  };
  return { server, stop };
}

/**
 * A separate server that runs Donut Teams 1.0.1 first (two bots make a team with a home), then 2.0: the console of
 * that first 2.0 start is the upgrade card.
 */
async function upgradeConsole(java) {
  const dir = path.join(lab, 'capture', 'upgrade');
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(path.join(dir, 'plugins'), { recursive: true });
  fs.writeFileSync(path.join(dir, 'eula.txt'), 'eula=true\n');
  fs.writeFileSync(path.join(dir, 'server.properties'), ['online-mode=false', 'white-list=false', `server-port=${PORT}`,
    'level-type=minecraft\\:flat', 'generate-structures=false', 'view-distance=3'].join('\n') + '\n');
  const legacy = await modrinth('W75KfqfW', 'paper', '1.0.1');
  fs.copyFileSync(legacy, path.join(dir, 'plugins', 'DonutTeams-1.0.1.jar'));
  fs.copyFileSync(await economyJar('Vault'), path.join(dir, 'plugins', 'Vault.jar'));
  const jar = await serverJar('paper', VERSION);
  let server = new Server(dir, java, jar);
  server.start();
  await server.waitFor(/Done \([0-9.,]+s\)!/, 300_000, 'legacy start');
  const owner = await joinBot('Moonbeam', VERSION, null, {}, PORT);
  const mate = await joinBot('Kiri', VERSION, null, {}, PORT);
  owner.chat('/team create Nightbeam NB');
  await sleep(1500);
  owner.chat('/team invite Kiri');
  await sleep(1000);
  mate.chat('/team join Nightbeam');
  await sleep(1000);
  owner.chat('/team sethome');
  await sleep(1500);
  owner.quit();
  mate.quit();
  await server.stop();
  fs.rmSync(path.join(dir, 'plugins', 'DonutTeams-1.0.1.jar'));
  fs.copyFileSync(pluginJar(), path.join(dir, 'plugins', path.basename(pluginJar())));
  server = new Server(dir, java, jar);
  server.start();
  await server.waitFor(/Donut Teams 2\.\d+\.\d+ enabled on/, 300_000, 'upgrade start');
  await sleep(1000);
  const lines = server.lines.filter((line) => /\[DonutTeams\]/.test(line) && !/Loading server plugin|Enabling DonutTeams|Hikari/.test(line))
    .map((line) => line.replace(/^\[[^\]]*(INFO|WARN)\]:? ?/, ''));
  await server.stop();
  fs.mkdirSync(consoleDir, { recursive: true });
  fs.writeFileSync(path.join(consoleDir, 'upgrade.txt'), lines.join('\n') + '\n');
  console.log(`  upgrade console: ${lines.length} lines`);
}

const bots = new Map();
export async function bot(server, name) {
  if (bots.has(name)) return bots.get(name);
  const b = await joinBot(name, VERSION, null, {}, PORT);
  bots.set(name, b);
  return b;
}

/** Interactive lab: POST /eval with JavaScript (an async body) to drive the running game. */
async function labMode(server) {
  const context = { server, x, sleep, slot, hover, clickSlot, park, screenshot, record, gif, bot: (name) => bot(server, name), bots, fs, path, repo, lab };
  const AsyncFunction = Object.getPrototypeOf(async () => {}).constructor;
  http.createServer((request, response) => {
    let body = '';
    request.on('data', (chunk) => { body += chunk; });
    request.on('end', async () => {
      try {
        const fn = new AsyncFunction(...Object.keys(context), body);
        const result = await fn(...Object.values(context));
        response.end(JSON.stringify(result ?? 'ok') + '\n');
      } catch (error) {
        response.statusCode = 500;
        response.end(String(error.stack || error) + '\n');
      }
    });
  }).listen(8899, '127.0.0.1');
  console.log('lab ready on http://127.0.0.1:8899');
  await new Promise(() => {});
}

async function main() {
  // --scenes=base,invite,create reruns only those scenes (in their usual order) and skips the upgrade console.
  const only = process.argv.find((arg) => arg.startsWith('--scenes='))?.slice('--scenes='.length).split(',');
  if (process.argv.includes('--upgrade-only') || (!process.argv.includes('--lab') && !only)) {
    await upgradeConsole(await jdk(21));
    if (process.argv.includes('--upgrade-only')) {
      return;
    }
  }
  const { server, stop } = await boot();
  try {
    if (process.argv.includes('--lab')) {
      await labMode(server);
    } else {
      const { scenes } = await import('./scenes.mjs');
      await scenes({ server, bot: (name) => bot(server, name), lab, consoleDir, only });
    }
  } finally {
    for (const b of bots.values()) {
      try { b.quit(); } catch { /* gone */ }
    }
    await stop();
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}
