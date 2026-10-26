// Boots real Paper and Folia servers across the supported range and drives Donut Teams with mineflayer bots.
// Every version runs the same scenario (teams, invites, ranks, chat, friendly fire, bank, levels, homes, chest,
// allies and enemies, stats, leaderboards and boards, join requests, menus, placeholders, admin commands) and the
// console is scanned for plugin errors. Targets marked `upgrade` first run Donut Teams 1.0.1 from Modrinth, make a
// team with bots, then swap in the new jar: the 2.0 start must keep everything.
//
// Usage (from the repository root, after building the plugin and the fixture, see README.md):
//   node tools/smoke/run.mjs                 # the whole matrix
//   node tools/smoke/run.mjs paper-26.3      # selected targets (project-version[-variant])
//
// Downloads (JDKs, server jars, plugins) are cached in $TEAMLAB_HOME (default ~/.cache/donutteams-smoke).
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import mineflayer from 'mineflayer';
import nbt from 'prismarine-nbt';
import { scenario } from './scenario.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, '..', '..');
const lab = process.env.TEAMLAB_HOME || path.join(os.homedir(), '.cache', 'donutteams-smoke');
const cache = path.join(lab, 'cache');
const PORT = Number(process.env.TEAMLAB_PORT || 25610);

// Bots speak the newest protocol mineflayer knows; newer servers get ViaVersion + ViaBackwards.
export const MATRIX = [
  { project: 'paper', version: '1.20.1', java: 17, bot: '1.20.1', economy: 'Vault' },
  { project: 'paper', version: '1.20.4', java: 17, bot: '1.20.4', economy: 'VaultUnlocked' },
  { project: 'paper', version: '1.20.6', java: 21, bot: '1.20.6', economy: 'Vault', upgrade: true },
  { project: 'paper', version: '1.21.1', java: 21, bot: '1.21.1', economy: 'VaultUnlocked' },
  { project: 'paper', version: '1.21.4', java: 21, bot: '1.21.4', economy: 'Vault' },
  { project: 'paper', version: '1.21.8', java: 21, bot: '1.21.8', economy: 'VaultUnlocked' },
  { project: 'paper', version: '1.21.11', java: 21, bot: '1.21.11', economy: 'Vault', upgrade: true, restart: true },
  { project: 'paper', version: '1.21.11', java: 21, bot: '1.21.11', economy: 'VaultUnlocked', variant: 'mariadb', restart: true },
  { project: 'paper', version: '26.1.2', java: 25, bot: '26.1', economy: 'VaultUnlocked', via: true },
  { project: 'paper', version: '26.2', java: 25, bot: '26.1', economy: 'Vault', via: true },
  { project: 'paper', version: '26.3', java: 25, bot: '26.1', economy: 'VaultUnlocked', via: true, upgrade: true },
  { project: 'folia', version: '1.20.4', java: 17, bot: '1.20.4', economy: 'VaultUnlocked' },
  { project: 'folia', version: '1.21.4', java: 21, bot: '1.21.4', economy: 'VaultUnlocked' },
  { project: 'folia', version: '1.21.11', java: 21, bot: '1.21.11', economy: 'VaultUnlocked', restart: true },
  { project: 'folia', version: '26.3', java: 25, bot: '26.1', economy: 'VaultUnlocked', via: true, upgrade: true },
];

export const targetName = (t) => `${t.project}-${t.version}${t.variant ? '-' + t.variant : ''}`;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const AGENT = { 'User-Agent': 'NightBeam-DonutTeams-smoke' };

async function fetchJson(url) {
  const response = await fetch(url, { headers: AGENT });
  if (!response.ok) throw new Error(`${url}: HTTP ${response.status}`);
  return response.json();
}

async function download(url, file) {
  if (fs.existsSync(file)) return file;
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const response = await fetch(url, { headers: AGENT });
  if (!response.ok) throw new Error(`${url}: HTTP ${response.status}`);
  fs.writeFileSync(file + '.part', Buffer.from(await response.arrayBuffer()));
  fs.renameSync(file + '.part', file);
  return file;
}

function cached(...parts) {
  const file = path.join(cache, ...parts);
  return fs.existsSync(file) ? file : null;
}

export async function jdk(major) {
  const known = cached('jdk', String(major), 'bin', 'java');
  if (known) return known;
  const dir = path.join(cache, 'jdk', String(major));
  const archive = await download(`https://api.adoptium.net/v3/binary/latest/${major}/ga/linux/x64/jdk/hotspot/normal/eclipse`, dir + '.tar.gz');
  fs.mkdirSync(dir, { recursive: true });
  await run('tar', ['-xzf', archive, '-C', dir, '--strip-components=1']);
  fs.rmSync(archive);
  return path.join(dir, 'bin', 'java');
}

export async function serverJar(project, version) {
  const known = cached('servers', `${project}-${version}.jar`);
  if (known) return known;
  const build = await fetchJson(`https://fill.papermc.io/v3/projects/${project}/versions/${version}/builds/latest`);
  return download(build.downloads['server:default'].url, path.join(cache, 'servers', `${project}-${version}.jar`));
}

/** The newest release of a Modrinth project for a loader (or a given version number). */
export async function modrinth(project, loader = 'paper', versionNumber = null) {
  const name = `${project}-${loader}${versionNumber ? '-' + versionNumber : ''}.jar`;
  const known = cached('plugins', name);
  if (known) return known;
  const versions = await fetchJson(`https://api.modrinth.com/v2/project/${project}/version?loaders=${encodeURIComponent(JSON.stringify([loader]))}`);
  const release = versionNumber ? versions.find((v) => v.version_number === versionNumber)
    : versions.find((v) => v.version_type === 'release') || versions[0];
  if (!release) throw new Error(`no ${project} ${versionNumber || ''} for ${loader}`);
  return download(release.files.find((f) => f.primary)?.url || release.files[0].url, path.join(cache, 'plugins', name));
}

export async function economyJar(name) {
  if (name === 'Vault') {
    return cached('plugins', 'Vault-1.7.3.jar')
      || download('https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar', path.join(cache, 'plugins', 'Vault-1.7.3.jar'));
  }
  return modrinth('vaultunlocked');
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: 'inherit', ...options });
    child.on('exit', (code) => (code === 0 ? resolve() : reject(new Error(`${command} exited with ${code}`))));
  });
}

export function pluginJar() {
  if (process.env.PLUGIN_JAR) return process.env.PLUGIN_JAR;
  const libs = path.join(repo, 'build', 'libs');
  const jar = fs.existsSync(libs) && fs.readdirSync(libs).filter((f) => /^DonutTeams-\d.*\.jar$/.test(f)).sort().at(-1);
  if (!jar) throw new Error('Build the plugin first: ./gradlew build');
  return path.join(libs, jar);
}

export const fixtureJar = () => path.join(here, 'fixture', 'build', 'libs', 'TeamLabFixture.jar');

/** One running server: stdin for console commands, and a line log that waitFor() scans. */
export class Server {
  constructor(dir, java, jar) {
    this.dir = dir;
    this.java = java;
    this.jar = jar;
    this.lines = [];
    this.cursor = 0;
    this.waiters = [];
  }

  start() {
    const log = fs.createWriteStream(path.join(this.dir, 'console.log'), { flags: 'a' });
    this.process = spawn(this.java, ['-Xms1G', '-Xmx3G', '-Dterminal.jline=false', '-Dterminal.ansi=false', '-Dfile.encoding=UTF-8',
      '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-Dpaper.disableChannelLimit=true', '-jar', this.jar, '--nogui'], { cwd: this.dir });
    let buffer = '';
    const onData = (chunk) => {
      log.write(chunk);
      buffer += chunk.toString('utf8');
      let index;
      while ((index = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, index).replace(/\x1b\[[0-9;]*m/g, '');
        buffer = buffer.slice(index + 1);
        this.lines.push(line);
        this.waiters = this.waiters.filter((waiter) => !waiter.test(line));
      }
    };
    this.process.stdout.on('data', onData);
    this.process.stderr.on('data', onData);
    this.exited = new Promise((resolve) => this.process.on('exit', resolve));
  }

  /** Resolves with the first line after the cursor that matches. */
  waitFor(pattern, timeout = 30_000, label = String(pattern)) {
    for (let i = this.cursor; i < this.lines.length; i++) {
      const match = this.lines[i].match(pattern);
      if (match) {
        this.cursor = i + 1;
        return Promise.resolve(match);
      }
    }
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.waiters = this.waiters.filter((waiter) => waiter !== entry);
        reject(new Error(`timed out waiting for ${label}`));
      }, timeout);
      const entry = {
        test: (line) => {
          const match = line.match(pattern);
          if (!match) return false;
          clearTimeout(timer);
          this.cursor = this.lines.length;
          resolve(match);
          return true;
        },
      };
      this.waiters.push(entry);
    });
  }

  seen(pattern) {
    return this.lines.some((line) => pattern.test(line));
  }

  mark() {
    this.cursor = this.lines.length;
  }

  command(line) {
    this.process.stdin.write(line + '\n');
  }

  /** Runs a fixture command and returns the text after "[TEAMLAB] ". */
  async lab(line, timeout = 20_000) {
    this.mark();
    this.command('teamlab ' + line);
    const word = line.split(' ')[0];
    const match = await this.waitFor(new RegExp(`\\[TEAMLAB\\] ((?:${word}|error) .*)`), timeout, `teamlab ${line}`);
    if (match[1].startsWith('error')) throw new Error(match[1]);
    return match[1];
  }

  async stop() {
    if (!this.process || this.process.exitCode !== null) return;
    this.command('stop');
    const timeout = sleep(90_000).then(() => 'timeout');
    if ((await Promise.race([this.exited, timeout])) === 'timeout') this.process.kill('SIGKILL');
  }
}

/** Plain text of a chat component: JSON text, a JSON string, or network NBT. */
export function plain(component) {
  if (component == null) return '';
  if (typeof component === 'number' || typeof component === 'boolean') return String(component);
  if (typeof component === 'string') {
    if (!/^[{\[]/.test(component.trim())) return component;
    try { return plain(JSON.parse(component)); } catch { return component; }
  }
  if (Array.isArray(component)) return component.map(plain).join('');
  if (typeof component.type === 'string' && 'value' in component) return plain(nbt.simplify(component));
  const text = component.text ?? component[''] ?? '';
  return (typeof text === 'string' ? text : plain(text)) + (Array.isArray(component.extra) ? component.extra.map(plain).join('') : '');
}

/**
 * A mineflayer bot with its chat (plain and raw JSON) and action bars recorded. Bots stand still unless
 * {@code moves} is set: through ViaBackwards, newer servers can reject an older client's movement packets.
 */
export async function joinBot(name, version, chatFile, { moves = false } = {}, port = PORT) {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port, username: name, version, auth: 'offline', checkTimeoutInterval: 120_000 });
  bot.chatLog = [];
  bot.rawLog = [];
  bot.actionBars = [];
  const record = (kind, text) => chatFile && fs.appendFileSync(chatFile, `${kind}\t${text}\n`);
  bot.on('message', (message, position) => {
    if (position === 'game_info') {
      bot.actionBars.push(message.toString());
      return;
    }
    bot.chatLog.push(message.toString());
    try {
      bot.rawLog.push(JSON.stringify(message.json));
    } catch {
      bot.rawLog.push('');
    }
    record(position, message.toString());
  });
  bot._client.on('packet', (data, meta) => {
    if (/action_bar/.test(meta.name)) {
      const text = plain(data.text);
      bot.actionBars.push(text);
      record('action_bar', text);
    }
  });
  if (!moves) {
    bot.physicsEnabled = false;
    bot.once('login', () => { bot.physicsEnabled = false; });
    const write = bot._client.write.bind(bot._client);
    bot._client.write = (packet, params) => (/^(position|position_look|look|flying)$/.test(packet) ? undefined : write(packet, params));
  }
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`${name} did not spawn`)), 120_000);
    bot.once('spawn', () => { clearTimeout(timer); resolve(); });
    bot.once('kicked', (reason) => { clearTimeout(timer); reject(new Error(`${name} kicked: ${JSON.stringify(reason)}`)); });
    bot.once('error', (error) => { clearTimeout(timer); reject(error); });
  });
  await sleep(1500);
  return bot;
}

/** Overrides on the shipped config.yml that make the scenario fast (warmup, refresh). */
function smokeConfig(target) {
  let config = fs.readFileSync(path.join(repo, 'src', 'main', 'resources', 'config.yml'), 'utf8');
  config = config.replace(/(warmup-seconds: )3/, '$12').replace(/(  refresh-seconds: )30/, '$15')
    .replace(/(board-refresh-seconds: )30/, '$15').replace(/(update-check:\n  enabled: )true/, '$1false')
    .replace(/(  dialogs: )true/, '$1false');
  if (target.variant === 'mariadb') {
    config = config.replace('type: SQLITE', 'type: MARIADB').replace('username: donutteams', 'username: donutteams')
      .replace('password: ""', `password: "${process.env.TEAMLAB_MARIADB_PASSWORD || 'teams'}"`)
      .replace('table-prefix: ""', `table-prefix: "smoke${Date.now().toString(36)}_"`);
  }
  return config;
}

function prepare(target, jars) {
  const dir = path.join(lab, 'servers', targetName(target));
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(path.join(dir, 'plugins'), { recursive: true });
  fs.writeFileSync(path.join(dir, 'eula.txt'), 'eula=true\n');
  fs.writeFileSync(path.join(dir, 'server.properties'), [
    'online-mode=false', 'white-list=false', 'enforce-secure-profile=false', `server-port=${PORT}`, 'level-seed=donutteams-lab',
    'spawn-protection=0', 'view-distance=3', 'simulation-distance=3', 'allow-flight=true', 'motd=Donut Teams smoke',
    'difficulty=peaceful', 'spawn-monsters=false', 'level-type=minecraft\\:flat', 'generate-structures=false', 'pvp=true',
  ].join('\n') + '\n');
  for (const jar of jars) fs.copyFileSync(jar, path.join(dir, 'plugins', path.basename(jar)));
  return dir;
}

/** Plugin errors in the console: severe/error lines from the plugin, stack frames in its packages, linkage errors. */
export function pluginErrors(lines) {
  const problems = [];
  lines.forEach((line, index) => {
    const ours = /\[DonutTeams\]/.test(line) && /(ERROR|SEVERE|WARN)/.test(line);
    const frame = /at io\.nightbeam\.donutteams\./.test(line);
    const load = /Could not load '.*DonutTeams|Error occurred while enabling DonutTeams|NoSuchMethodError|NoClassDefFoundError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError/.test(line);
    if (ours || load || (frame && !problems.includes(lines[index - 1]))) problems.push(line);
  });
  return problems;
}

/** Donut Teams 1.0.1 makes a team the way a real server would have, before the 2.0 jar replaces it. */
async function legacyPhase(target, dir, java, jar, botVersion, result) {
  const legacy = await modrinth('W75KfqfW', target.project === 'folia' ? 'folia' : 'paper', '1.0.1');
  const plugins = path.join(dir, 'plugins');
  fs.copyFileSync(legacy, path.join(plugins, 'DonutTeams-1.0.1.jar'));
  const server = new Server(dir, java, jar);
  server.start();
  const bots = [];
  try {
    await server.waitFor(/Done \([0-9.,]+s\)!/, 300_000, 'legacy server start');
    if (!server.seen(/Donut Teams 1\.0\.1 enabled/)) throw new Error('Donut Teams 1.0.1 did not enable');
    const alice = await joinBot('Alice', botVersion, path.join(dir, 'chat-legacy-Alice.log'));
    bots.push(alice);
    const hank = await joinBot('Hank', botVersion, path.join(dir, 'chat-legacy-Hank.log'));
    bots.push(hank);
    server.command('op Alice');
    await sleep(500);
    alice.chat('/team create Legacy LGC');
    await sleep(1500);
    alice.chat('/team invite Hank');
    await sleep(1000);
    hank.chat('/team join Legacy');
    await sleep(1000);
    alice.chat('/team sethome');
    await sleep(800);
    alice.chat('/team pvp');
    await sleep(1500);
    result.steps.push({ label: '1.0.1 install made a team', ok: true, detail: 'Legacy with Alice and Hank, a home, friendly fire on' });
  } catch (error) {
    result.steps.push({ label: '1.0.1 install made a team', ok: false, detail: error.message });
  } finally {
    for (const bot of bots) {
      try { bot.quit(); } catch { /* gone */ }
    }
    await server.stop();
    fs.rmSync(path.join(plugins, 'DonutTeams-1.0.1.jar'));
  }
  return server.lines.length;
}

async function runTarget(target) {
  const name = targetName(target);
  const started = Date.now();
  const java = await jdk(target.java);
  const jars = [fixtureJar(), await economyJar(target.economy), cached('plugins', 'PlaceholderAPI.jar') || await modrinth('placeholderapi')];
  if (target.via) {
    jars.push(await modrinth('viaversion'));
    jars.push(await modrinth('viabackwards'));
  }
  const dir = prepare(target, jars);
  const jar = await serverJar(target.project, target.version);
  const result = { target: name, java: target.java, bot: target.bot, economy: target.economy, storage: target.variant === 'mariadb' ? 'MariaDB' : 'SQLite',
    upgrade: Boolean(target.upgrade), steps: [], errors: [] };
  if (target.upgrade) {
    await legacyPhase(target, dir, java, jar, target.bot, result);
  }
  fs.copyFileSync(pluginJar(), path.join(dir, 'plugins', path.basename(pluginJar())));
  const teamsFolder = path.join(dir, 'plugins', 'DonutTeams');
  fs.mkdirSync(teamsFolder, { recursive: true });
  if (!target.upgrade) {
    fs.writeFileSync(path.join(teamsFolder, 'config.yml'), smokeConfig(target));
  }
  let server = new Server(dir, java, jar);
  server.start();
  const bots = [];
  const restart = async () => {
    for (const bot of bots.splice(0)) {
      try { bot.quit(); } catch { /* gone */ }
    }
    await server.stop();
    const before = server.lines;
    server = new Server(dir, java, jar);
    server.lines = [...before, '--- restart ---'];
    server.cursor = server.lines.length;
    server.start();
    await server.waitFor(/Done \([0-9.,]+s\)!/, 300_000, 'server restart');
    await server.waitFor(/\[TEAMLAB\] api listening/, 30_000, 'fixture API listener');
    return server;
  };
  try {
    await server.waitFor(/Done \([0-9.,]+s\)!/, 300_000, 'server start');
    await server.waitFor(/\[TEAMLAB\] api (listening|absent)/, 30_000, 'fixture API listener');
    if (target.upgrade) {
      // The upgraded config keeps the 1.x values; speed up what the scenario waits on.
      for (const [key, value] of [['homes.warmup-seconds', 2], ['leaderboard.refresh-seconds', 5],
        ['leaderboard.board-refresh-seconds', 5], ['update-check.enabled', false], ['menus.dialogs', false]]) {
        await server.lab(`set config.yml ${key} ${value}`);
      }
      server.command('donutteams reload');
      await server.waitFor(/reloaded/, 20_000, 'reload');
    }
    await scenario({
      server: () => server, target, restart,
      joinBot: async (botName, options) => {
        const bot = await joinBot(botName, target.bot, path.join(dir, `chat-${botName}.log`), options);
        bots.push(bot);
        return bot;
      },
      step: (label, ok, detail = '') => {
        result.steps.push({ label, ok, detail });
        console.log(`  ${ok ? 'PASS' : 'FAIL'} ${label}${detail ? ` (${detail})` : ''}`);
      },
    });
  } catch (error) {
    result.steps.push({ label: 'scenario', ok: false, detail: error.message });
    console.log(`  FAIL scenario (${error.message})`);
  } finally {
    for (const bot of bots) {
      try { bot.quit(); } catch { /* already gone */ }
    }
    await server.stop();
  }
  result.errors = pluginErrors(server.lines);
  result.ok = result.steps.length > 0 && result.steps.every((s) => s.ok) && result.errors.length === 0;
  result.seconds = Math.round((Date.now() - started) / 1000);
  return result;
}

async function main() {
  const wanted = process.argv.slice(2);
  const targets = MATRIX.filter((t) => wanted.length === 0 || wanted.includes(targetName(t)));
  const results = [];
  for (const target of targets) {
    console.log(`\n== ${targetName(target)} (Java ${target.java}, bot ${target.bot}${target.via ? ' via ViaBackwards' : ''}, ${target.economy}${target.upgrade ? ', from 1.0.1' : ''})`);
    const result = await runTarget(target);
    for (const error of result.errors.slice(0, 20)) console.log(`  ERROR ${error}`);
    console.log(`  ${result.ok ? 'PASS' : 'FAIL'} in ${result.seconds}s`);
    results.push(result);
  }
  fs.mkdirSync(path.join(lab, 'reports'), { recursive: true });
  const report = path.join(lab, 'reports', `smoke-${new Date().toISOString().replace(/[:.]/g, '-')}.json`);
  fs.writeFileSync(report, JSON.stringify(results, null, 2));
  console.log('\n| Server | Java | Storage | From 1.0.1 | Steps | Plugin errors | Result |\n| --- | --- | --- | --- | --- | --- | --- |');
  for (const r of results) {
    console.log(`| ${r.target} | ${r.java} | ${r.storage} | ${r.upgrade ? 'yes' : '-'} | ${r.steps.filter((s) => s.ok).length}/${r.steps.length} | ${r.errors.length} | ${r.ok ? 'PASS' : 'FAIL'} |`);
  }
  console.log(`\nReport: ${report}`);
  process.exit(results.every((r) => r.ok) ? 0 : 1);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}
