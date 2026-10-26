// The Donut Teams smoke scenario, run once per server in the matrix. See README.md for what each step proves.
import { plain } from './run.mjs';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** Waits for a chat line on the bot after the last one matched. */
async function chat(bot, pattern, timeout = 20_000) {
  const deadline = Date.now() + timeout;
  bot.seenChat = bot.seenChat || 0;
  while (Date.now() < deadline) {
    for (let i = bot.seenChat; i < bot.chatLog.length; i++) {
      const match = bot.chatLog[i].match(pattern);
      if (match) {
        bot.seenChat = i + 1;
        return { match, raw: bot.rawLog[i] || '' };
      }
    }
    await sleep(100);
  }
  throw new Error(`${bot.username} never saw ${pattern} (last: ${JSON.stringify(bot.chatLog.slice(-3))})`);
}

/** Skips everything the bot has seen so far. */
const forget = (bot) => {
  bot.seenChat = bot.chatLog.length;
};

/** Runs an action and resolves with the window it opens. */
async function opens(bot, action, timeout = 15_000) {
  const opened = new Promise((resolve) => bot.once('windowOpen', resolve));
  await action();
  return Promise.race([opened, sleep(timeout).then(() => { throw new Error(`${bot.username}: no window opened`); })]);
}

const title = (window) => plain(window.title);
const itemName = (item) => (item ? plain(item.customName ?? item.displayName ?? '') : '');

/** "team Nightbeam tag=NB owner=Bob members=Bob,Cara level=1 ..." as an object. */
function parseTeam(line) {
  if (/ none$/.test(line)) return null;
  const out = { name: line.split(' ')[1] };
  for (const [, key, value] of line.matchAll(/(\w+)=(\S*)/g)) out[key] = value;
  out.memberList = out.members ? out.members.split(',').filter(Boolean) : [];
  return out;
}

export async function scenario({ server: current, target, restart, joinBot, step }) {
  let server = current();
  const check = async (label, body) => {
    try {
      const detail = await body();
      step(label, true, typeof detail === 'string' ? detail : '');
    } catch (error) {
      step(label, false, error.message);
    }
  };
  const team = async (name) => parseTeam(await server.lab(`team ${name}`));
  const expectTeam = async (name, expected) => {
    const t = await team(name);
    if (!t) throw new Error(`no team ${name}`);
    for (const [key, value] of Object.entries(expected)) {
      if (String(t[key]) !== String(value)) throw new Error(`${name}.${key} is ${t[key]}, expected ${value}`);
    }
    return t;
  };
  const papi = async (who, text) => (await server.lab(`papi ${who} ${text}`)).replace(/^papi \S+ /, '');
  const rel = async (one, two, text) => (await server.lab(`rel ${one} ${two} ${text}`)).replace(/^rel \S+ \S+ /, '');
  const pos = async (who) => {
    const m = (await server.lab(`pos ${who}`)).match(/^pos \S+ (\S+) (-?[\d.]+) (-?[\d.]+) (-?[\d.]+)/);
    return { world: m[1], x: Number(m[2]), y: Number(m[3]), z: Number(m[4]) };
  };
  const consoleCommand = async (command, pattern, label = command) => {
    server.mark();
    server.command(command);
    return server.waitFor(pattern, 20_000, label);
  };

  // 1. Start-up (and the 1.0.1 upgrade on upgrade targets).
  await check('plugin enables', async () => {
    const line = server.lines.find((l) => /Donut Teams 2\.\d+\.\d+ enabled on/.test(l));
    if (!line) throw new Error('no enable line');
    const platform = target.project === 'folia' ? /enabled on Folia/ : /enabled on (Paper|Purpur)/;
    if (!platform.test(line)) throw new Error(line);
    const storage = target.variant === 'mariadb' ? /storage MariaDB/ : /storage SQLite/;
    if (!storage.test(line)) throw new Error(`storage: ${line}`);
    if (!/economy TeamLabEconomy/.test(line)) throw new Error(`economy: ${line}`);
    return line.replace(/.*enabled on /, '');
  });
  if (target.upgrade) {
    await check('1.0.1 data upgraded', async () => {
      if (!server.seen(/Upgrading the 1\.x database to 2\.0/)) throw new Error('no database upgrade line');
      if (!server.seen(/Upgraded config\.yml from 1\.x/)) throw new Error('no config upgrade line');
      if (!/exists=true/.test(await server.lab('file config-1.x.yml'))) throw new Error('no config-1.x.yml backup');
      if (!/exists=true/.test(await server.lab('file messages-1.x.yml'))) throw new Error('no messages-1.x.yml backup');
      const legacy = await expectTeam('Legacy', { owner: 'Alice', ff: 'true', home: 'world', level: 1 });
      if (legacy.members !== 'Alice,Hank') throw new Error(`members ${legacy.members}`);
      return 'team, members, home and friendly fire kept';
    });
  }

  // 2. Creating a team and the menu.
  const alice = await joinBot('Alice');
  const bob = await joinBot('Bob');
  if (!target.upgrade) {
    alice.chat('/team create Legacy LGC');
    await chat(alice, /You founded Legacy/);
  }
  await check('create a team', async () => {
    server.mark();
    bob.chat('/team create Nightbeam NB');
    await chat(bob, /You founded Nightbeam \[NB\]/);
    await server.waitFor(/\[TEAMLAB\] event create Bob Nightbeam NB/, 10_000, 'create event');
    await expectTeam('Nightbeam', { owner: 'Bob', members: 'Bob', level: 1, tag: 'NB' });
    return 'Nightbeam [NB]';
  });
  await check('team menu opens and navigates', async () => {
    const main = await opens(bob, () => bob.chat('/team'));
    if (!/Team · Nightbeam/.test(title(main))) throw new Error(`title "${title(main)}"`);
    const icon = main.slots[4]?.name;
    if (!/banner$/.test(icon || '')) throw new Error(`icon ${icon}`);
    if (main.slots[19]?.name !== 'player_head') throw new Error(`members button ${main.slots[19]?.name}`);
    if (main.slots[0]?.name !== 'gray_stained_glass_pane') throw new Error(`filler ${main.slots[0]?.name}`);
    const members = await opens(bob, () => bob.clickWindow(19, 0, 0));
    if (!/Members/.test(title(members))) throw new Error(`members title "${title(members)}"`);
    if (members.slots[10]?.name !== 'player_head') throw new Error(`roster ${members.slots[10]?.name}`);
    bob.closeWindow(members);
    return `${title(main)} → ${title(members)}`;
  });

  // 3. Invites, ranks, kicking.
  const cara = await joinBot('Cara');
  await check('invite with clickable buttons', async () => {
    bob.chat('/team invite Cara');
    const invite = await chat(cara, /invited you to Nightbeam/);
    if (!invite.raw.includes('/team join Nightbeam')) throw new Error('no clickable accept button');
    cara.chat('/team join Nightbeam');
    await chat(cara, /Welcome to Nightbeam/);
    await chat(bob, /Cara joined the team/);
    await expectTeam('Nightbeam', { members: 'Bob,Cara' });
    return 'accept button runs /team join Nightbeam';
  });
  await check('promote to officer', async () => {
    server.mark();
    bob.chat('/team promote Cara');
    await chat(bob, /Cara is now Officer/);
    await server.waitFor(/\[TEAMLAB\] event role Nightbeam MEMBER>OFFICER/, 10_000, 'role event');
  });
  const dan = await joinBot('Dan');
  await check('officer invites and kicks', async () => {
    cara.chat('/team invite Dan');
    await chat(dan, /invited you to Nightbeam/);
    dan.chat('/team join Nightbeam');
    await chat(dan, /Welcome to Nightbeam/);
    dan.chat('/team chat');
    await chat(dan, /Team chat ON/);
    cara.chat('/team kick Dan');
    await chat(dan, /You were kicked from Nightbeam/);
    await expectTeam('Nightbeam', { members: 'Bob,Cara' });
  });
  await check('team chat reaches only the team', async () => {
    forget(dan);
    cara.chat('/tc hello team');
    await chat(bob, /\[Team\] Officer Cara: hello team/);
    await sleep(1000);
    if (dan.chatLog.slice(dan.seenChat).some((line) => /hello team/.test(line))) throw new Error('Dan saw team chat');
  });
  await check('kicked player with team chat on can still chat publicly', async () => {
    dan.chat('public hello');
    await chat(bob, /Dan.*public hello/);
    return '1.x swallowed these messages';
  });

  // 4. Friendly fire.
  await check('friendly fire off protects teammates', async () => {
    await sleep(2000);
    const hit = await server.lab('hit Bob Cara 2');
    const [, before, after] = hit.match(/before=([\d.]+) after=([\d.]+)/);
    if (before !== after) throw new Error(hit);
    bob.chat('/team pvp');
    await chat(bob, /Friendly fire is now ON/);
    const hurt = await server.lab('hit Bob Cara 2');
    const [, b2, a2] = hurt.match(/before=([\d.]+) after=([\d.]+)/);
    if (Number(a2) >= Number(b2)) throw new Error(`with friendly fire on: ${hurt}`);
    bob.chat('/team pvp');
    await chat(bob, /Friendly fire is now OFF/);
    return `blocked, then ${b2} → ${a2} with it on`;
  });

  // 5. Bank and levels.
  await check('bank deposit and withdraw', async () => {
    await server.lab('money Bob 50000');
    server.mark();
    bob.chat('/team deposit 30k');
    await chat(bob, /deposited \$30,000\.00/);
    await server.waitFor(/\[TEAMLAB\] event bank Nightbeam DEPOSIT 30000/, 10_000, 'bank event');
    bob.chat('/team withdraw 5000');
    await chat(bob, /withdrew \$5,000\.00/);
    await expectTeam('Nightbeam', { balance: '25000.00' });
    const balance = await server.lab('balance Bob');
    if (!/ 25000\.0$/.test(balance)) throw new Error(balance);
    return 'team $25,000, player $25,000';
  });
  await check('level up from the bank', async () => {
    server.mark();
    bob.chat('/team level up');
    await chat(bob, /LEVEL UP!/);
    await server.waitFor(/\[TEAMLAB\] event level Nightbeam 1>2/, 10_000, 'level event');
    await expectTeam('Nightbeam', { level: 2, balance: '0.00' });
    await chat(bob, /Now: 10 members · 2 homes · 2 chest rows/);
  });

  // 6. Homes.
  let home = null;
  await check('set a home and teleport to it', async () => {
    home = await pos('Bob');
    bob.chat('/team sethome base');
    await chat(bob, /set the team home base/);
    await server.lab(`tp Cara ${home.world} ${home.x + 40} ${home.y} ${home.z + 40}`);
    await sleep(1500);
    server.mark();
    forget(cara);
    cara.chat('/team home base');
    await chat(cara, /Teleporting to base in 2s/);
    await server.waitFor(/\[TEAMLAB\] event home Cara Nightbeam base/, 15_000, 'home event');
    await chat(cara, /Welcome to base/);
    const there = await pos('Cara');
    const distance = Math.hypot(there.x - home.x, there.z - home.z);
    if (distance > 1.5) throw new Error(`Cara is ${distance.toFixed(1)} blocks from the home`);
    return `landed ${distance.toFixed(2)} blocks from it`;
  });
  await check('warmup cancels on damage and on teleport', async () => {
    await sleep(1000);
    cara.chat('/team home base');
    await chat(cara, /Teleporting to base/);
    await server.lab('damage Cara 1');
    await chat(cara, /cancelled: you took damage/);
    cara.chat('/team home');
    await chat(cara, /Teleporting to base/);
    await server.lab(`tp Cara ${home.world} ${home.x + 20} ${home.y} ${home.z}`);
    await chat(cara, /cancelled: you moved/);
  });

  // 7. The team chest.
  await check('team chest keeps items', async () => {
    await server.lab('give Bob DIAMOND 5');
    await sleep(500);
    const chest = await opens(bob, () => bob.chat('/team chest'));
    if (!/Team Chest/.test(title(chest))) throw new Error(`title "${title(chest)}"`);
    if (chest.inventoryStart !== 18) throw new Error(`level 2 chest should have 2 rows, has ${chest.inventoryStart / 9}`);
    const from = chest.slots.findIndex((item, index) => index >= chest.inventoryStart && item?.name === 'diamond');
    if (from < 0) throw new Error('no diamond in the bot inventory');
    await bob.clickWindow(from, 0, 0);
    await sleep(300);
    await bob.clickWindow(0, 0, 0);
    await sleep(500);
    bob.closeWindow(chest);
    await sleep(1500);
    const left = await server.lab('count Bob DIAMOND');
    if (!/ 0$/.test(left)) throw new Error(`items not moved: ${left}`);
    const again = await opens(bob, () => bob.chat('/team chest'));
    const stored = again.slots[0];
    bob.closeWindow(again);
    if (stored?.name !== 'diamond' || stored.count !== 5) throw new Error(`slot 0 is ${stored?.name} x${stored?.count}`);
    return '5 diamonds stored';
  });

  // 8. Allies and enemies.
  await check('alliance by request and accept', async () => {
    server.mark();
    alice.chat('/team ally Nightbeam');
    const request = await chat(bob, /Legacy wants to be allies/);
    if (!request.raw.includes('/team ally Legacy')) throw new Error('no clickable ally button');
    bob.chat('/team ally Legacy');
    await chat(alice, /ALLIANCE! You are now allied with Nightbeam/);
    await server.waitFor(/\[TEAMLAB\] event relation Nightbeam Legacy NEUTRAL>ALLY/, 10_000, 'relation event');
    await expectTeam('Nightbeam', { allies: 'Legacy' });
  });
  await check('ally chat and ally protection', async () => {
    alice.chat('/ac hi allies');
    await chat(bob, /\[Ally\] \[LGC\] Alice: hi allies/);
    const hit = await server.lab('hit Alice Bob 2');
    const [, before, after] = hit.match(/before=([\d.]+) after=([\d.]+)/);
    if (before !== after) throw new Error(`ally was hurt: ${hit}`);
  });
  await check('placeholders, including relational ones', async () => {
    const own = await papi('Bob', '%donutteams_tag%|%donutteams_role%|%donutteams_level%|%donutteams_members%');
    if (own !== 'NB|Owner|2|2') throw new Error(`own ${own}`);
    const ally = await rel('Bob', 'Alice', '%rel_donutteams_relation%');
    const mate = await rel('Bob', 'Cara', '%rel_donutteams_relation%');
    if (ally !== 'Ally' || mate !== 'Teammate') throw new Error(`relations ${ally}/${mate}`);
    return `${own}, Alice=${ally}, Cara=${mate}`;
  });
  const eve = await joinBot('Eve');
  await check('enemy declaration and kill stats', async () => {
    eve.chat('/team create Rivals RV');
    await chat(eve, /You founded Rivals/);
    eve.chat('/team enemy Nightbeam');
    await chat(bob, /Rivals declared your team an enemy/);
    const enemy = await rel('Bob', 'Eve', '%rel_donutteams_relation%');
    if (enemy !== 'Enemy') throw new Error(`relation ${enemy}`);
    await sleep(1500);
    await server.lab('kill Eve Bob');
    await sleep(2500);
    await expectTeam('Rivals', { kills: 1, enemies: 'Nightbeam' });
    await expectTeam('Nightbeam', { deaths: 1 });
    return 'Rivals 1 kill, Nightbeam 1 death';
  });
  await check('leaderboard placeholder and menu', async () => {
    await sleep(6000);
    const first = await papi('Bob', '%donutteams_top_kills_1_name%');
    if (first !== 'Rivals') throw new Error(`top kills #1 is ${first}`);
    const top = await opens(bob, () => bob.chat('/team top'));
    const name = itemName(top.slots[19]);
    bob.closeWindow(top);
    if (!/Rivals/.test(name)) throw new Error(`first entry "${name}"`);
    return `#1 ${first}`;
  });
  await check('top teams board in the world', async () => {
    await server.lab('perm Bob donutteams.admin true');
    const at = await pos('Bob');
    bob.chat('/donutteams board create lab kills');
    await chat(bob, /Board lab created/);
    await sleep(2500);
    const displays = await server.lab(`displays ${at.world} ${at.x} ${at.y + 2.2} ${at.z}`);
    if (!/^displays 1 /.test(displays) || !/TOP TEAMS/.test(displays) || !/Rivals/.test(displays)) throw new Error(displays);
    bob.chat('/donutteams board delete lab');
    await chat(bob, /Board lab removed/);
    return displays.slice(11, 70);
  });

  // 9. Requests, open teams, menu buttons, settings.
  const frank = await joinBot('Frank');
  await check('join request accepted by the owner', async () => {
    frank.chat('/team request Nightbeam');
    const request = await chat(bob, /Frank wants to join the team/);
    if (!request.raw.includes('/team accept Frank')) throw new Error('no clickable accept button');
    bob.chat('/team accept Frank');
    await chat(frank, /Welcome to Nightbeam/);
  });
  const gus = await joinBot('Gus');
  await check('open team needs no invite', async () => {
    bob.chat('/team open');
    await chat(bob, /opened the team/);
    gus.chat('/team join Nightbeam');
    await chat(gus, /Welcome to Nightbeam/);
    const t = await team('Nightbeam');
    if (t.memberList.length !== 4) throw new Error(`members ${t.members}`);
    return t.members;
  });
  await check('bank menu button deposits', async () => {
    await server.lab('money Bob 1000');
    const main = await opens(bob, () => bob.chat('/team'));
    const bank = await opens(bob, () => bob.clickWindow(23, 0, 0));
    if (!/Team bank/.test(title(bank))) throw new Error(`title "${title(bank)}" (main was "${title(main)}")`);
    await bob.clickWindow(19, 0, 0);
    await sleep(1500);
    try { bob.closeWindow(bob.currentWindow); } catch { /* closed */ }
    await expectTeam('Nightbeam', { balance: '100.00' });
    return 'deposit 100 button';
  });
  await check('team settings', async () => {
    bob.chat('/team color aqua');
    await chat(bob, /Team colour set/);
    bob.chat('/team description Night owls');
    await chat(bob, /Description updated/);
    await server.lab('hold Bob WHITE_BANNER');
    bob.chat('/team icon');
    await chat(bob, /Team icon set to white banner/);
    bob.chat('/team motd Raid at nine');
    await chat(cara, /set the message of the day: Raid at nine/);
    await expectTeam('Nightbeam', { color: 'aqua', open: 'true' });
  });
  await check('ownership transfer', async () => {
    server.mark();
    bob.chat('/team transfer Cara confirm');
    await chat(cara, /Cara is the new owner of Nightbeam/);
    await expectTeam('Nightbeam', { owner: 'Cara' });
  });

  // 10. Restart: everything comes back from the database.
  let owner = cara;
  if (target.restart) {
    await check('data survives a restart', async () => {
      server = await restart();
      await expectTeam('Nightbeam', { owner: 'Cara', level: 2, allies: 'Legacy', deaths: 1, color: 'aqua', balance: '100.00' });
      await expectTeam('Rivals', { kills: 1, enemies: 'Nightbeam' });
      owner = await joinBot('Cara');
      const chest = await opens(owner, () => owner.chat('/team chest'));
      const stored = chest.slots[0];
      owner.closeWindow(chest);
      if (stored?.name !== 'diamond' || stored.count !== 5) throw new Error(`chest slot 0 is ${stored?.name} x${stored?.count}`);
      return 'teams, relations, stats, bank and chest';
    });
  }

  // 11. Disbanding pays out; admin commands.
  await check('disband returns the bank and the chest', async () => {
    await server.lab('money Cara 0');
    await sleep(1000);
    server.mark();
    owner.chat('/team disband confirm');
    await chat(owner, /The team Nightbeam was disbanded/);
    await server.waitFor(/\[TEAMLAB\] event disband Nightbeam admin=false/, 10_000, 'disband event');
    await sleep(2500);
    if (await team('Nightbeam')) throw new Error('team still exists');
    const balance = await server.lab('balance Cara');
    if (!/ 100\.0$/.test(balance)) throw new Error(`bank not paid out: ${balance}`);
    const diamonds = await server.lab('count Cara DIAMOND');
    if (!/ 5$/.test(diamonds)) throw new Error(`chest not returned: ${diamonds}`);
    return '$100 and 5 diamonds to the owner';
  });
  await check('admin commands', async () => {
    await consoleCommand('donutteams info', /Teams \d+ · members \d+/);
    await consoleCommand('donutteams disband Rivals', /Disbanded Rivals/);
    if (await team('Rivals')) throw new Error('Rivals still exists');
    await consoleCommand('donutteams reload', /Donut Teams reloaded/);
    await consoleCommand('donutteams version', /Donut Teams 2\.\d+\.\d+ on/);
  });
}
