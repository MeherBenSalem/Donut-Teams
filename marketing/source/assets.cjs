// The listing artwork. Each entry returns { html, width, height, transparent?, file? } for render.cjs.
// Crops are pixel boxes [x, y, w, h] of the 1920x1080 captures in images/screenshots/ (GUI scale 3). Menus and their
// tooltips are measured by tools/marketing/crops.py into crops.json ("<name>-tip" is a tooltip alone); chat lines,
// titles and dialogs sit at fixed places on screen and are listed here.
const fs = require('fs');
const path = require('path');
const r = require('./render.cjs');

const HEADERS = {
  menu: 'The Team Menu', ranks: 'Ranks', allies: 'Allies & Enemies', bank: 'Bank & Levels', chest: 'Team Chest',
  homes: 'Team Homes', joining: 'Joining', chat: 'Chat', leaderboards: 'Leaderboards', yours: 'Make It Yours',
  integrations: 'Integrations', upgrade: 'Upgrading from 1.x', compatibility: 'Compatibility',
  commands: 'Commands & Permissions', faq: 'Questions', gallery: 'Gallery',
};

/** Chat lines at GUI scale 3: the newest line ends at y = 962 (the hearts start below), each line is 27 px, the chat box
 * is 996 px wide. */
const chat = (lines, width = 996, bottom = 962) => [0, bottom - lines * 27 - 3, width, lines * 27 + 3];

const MEASURED = path.join(__dirname, 'crops.json');
const CROP = Object.assign({
  'invite-received-line': chat(2),
  'joined-chat-lines': chat(9),
  'level-up-lines': chat(2),
  'ally-request-line': chat(1),
  'alliance-war-lines': chat(3),
  'team-chat-lines': chat(5),
  'request-line': chat(1),
  'info-lines': chat(8, 700),
  'created-lines': chat(3),
  'home-warmup-scene': [0, 0, 1920, 1080],
  'home-landed-scene': [0, 0, 1920, 1080],
  'board-scene': [240, 260, 1440, 820],
  'dialog-create-filled': [540, 16, 840, 530],
}, fs.existsSync(MEASURED) ? JSON.parse(fs.readFileSync(MEASURED, 'utf8')) : {});
const FILES = {
  'invite-received-line': 'invite-received', 'joined-chat-lines': 'joined-chat', 'level-up-lines': 'level-up',
  'ally-request-line': 'ally-request', 'alliance-war-lines': 'alliance-war', 'team-chat-lines': 'team-chat',
  'request-line': 'request-chat', 'info-lines': 'info-chat',
  'home-warmup-scene': 'home-warmup', 'home-landed-scene': 'home-landed', 'board-scene': 'board', 'created-lines': 'created',
};

/** Width and height of a crop scaled to fit maxW x maxH. */
const fitted = (name, maxW, maxH) => {
  const box = CROP[name];
  if (!box) throw new Error(`no crop for ${name}`);
  const width = Math.round(Math.min(maxW, maxH * box[2] / box[3]));
  return [width, Math.round(width * box[3] / box[2])];
};
const fileOf = (name) => (FILES[name] || name.replace(/-tip$/, '')) + '.jpg';

/** A framed crop of a capture, as wide as possible within maxW x maxH. */
const shotArt = (name, maxW, maxH, style = '') => r.framed(r.region(fileOf(name), ...CROP[name], fitted(name, maxW, maxH)[0]), style);

// The card art area is 850 x 780. main() sits top-left, inset() bottom-right; each label is pinned to its frame.
const ART_W = 850;
const ART_H = 780;
// A label overlaps the top of a menu (its title bar), but sits wholly above chat lines, tooltips and short crops.
const labelAbove = (name, h) => (/-lines?$|-tip$/.test(name) || h < 200 ? 50 : 22);
const main = (name, maxW, maxH, top, text, kind = '', left = 0) =>
  `${shotArt(name, maxW, maxH, `position:absolute;left:${left}px;top:${top}px`)}${text ? r.label(text, `left:${left + 24}px;top:${top - labelAbove(name, fitted(name, maxW, maxH)[1])}px`, kind) : ''}`;
const inset = (name, maxW, maxH, bottom, text, kind = 'gold', right = -20) => {
  const [w, h] = fitted(name, maxW, maxH);
  const left = ART_W - right - w;
  const top = ART_H - bottom - h;
  const labelTop = top - labelAbove(name, h);
  return `${shotArt(name, maxW, maxH, `position:absolute;left:${left}px;top:${top}px`)}${text ? r.label(text, `left:${left + 24}px;top:${labelTop}px`, kind) : ''}`;
};
/** Any crop at an exact place in the art area. */
const at = (name, maxW, maxH, left, top, text, kind = '') =>
  `${shotArt(name, maxW, maxH, `position:absolute;left:${left}px;top:${top}px`)}${text ? r.label(text, `left:${left + 24}px;top:${top - 22}px`, kind) : ''}`;

// Card art is built only when that card renders, so one missing capture does not stop the others.
const card = (key, options) => () => ({ width: 1600, height: 900, file: `card-${key}.png`, html: r.card(options()) });
const command = (text) => `<span style="color:var(--gold-hi)">${r.escapeHtml(text)}</span>`;
const comment = (text) => `<span style="color:var(--faint)">${r.escapeHtml(text)}</span>`;

const assets = {
  icon: () => ({ width: 512, height: 512, html: r.page(512, 512, `
    <div style="position:absolute;inset:0;background:radial-gradient(circle at 50% 42%,#173a35 0%,#0f2023 58%,#08100f 100%)"></div>
    ${r.sprinkles(26, 0.55)}
    <div style="position:absolute;left:46px;top:46px;width:420px;height:420px;border-radius:50%;
      background:radial-gradient(circle,rgba(73,214,160,.32) 0%,rgba(73,214,160,0) 66%)"></div>
    <div style="position:absolute;left:64px;top:56px;filter:drop-shadow(0 18px 26px rgba(0,0,0,.6))">${r.donutSvg(384)}</div>
    <div style="position:absolute;right:30px;bottom:40px;display:flex;align-items:center;gap:10px;padding:12px 22px 11px 20px;border-radius:999px;
      background:linear-gradient(180deg,#ffd76a,#f0a92e);box-shadow:0 10px 26px rgba(0,0,0,.55),inset 0 -3px 0 rgba(120,70,0,.35)">
      <span style="font-family:DoDisplay;font-weight:900;font-size:40px;line-height:1;color:#3b2203;letter-spacing:.04em">TEAMS</span></div>`) }),

  divider: () => ({ width: 1400, height: 40, transparent: true, html: r.divider() }),

  banner: () => ({ width: 1800, height: 600, html: r.page(1800, 600, `
    ${r.backdrop(0.5, 6)}
    ${r.sprinkles(60, 0.4)}
    <div style="position:absolute;left:900px;top:-40px;width:1100px;height:700px;perspective:1700px">
      <div style="transform:rotateY(-17deg) rotateX(4deg) translateZ(0);transform-origin:left center;margin-top:52px">
        ${shotArt('menu-main', 760, 480, 'display:inline-block')}
      </div>
    </div>
    <div style="position:absolute;inset:0;background:linear-gradient(90deg,rgba(9,14,15,.95) 0%,rgba(9,14,15,.86) 40%,rgba(9,14,15,.15) 62%,rgba(9,14,15,0) 100%)"></div>
    <div style="position:absolute;left:90px;top:0;bottom:0;width:840px;display:flex;flex-direction:column;justify-content:center;z-index:2">
      <div class="kicker"><span class="dot"></span>${r.RELEASE.version} · ${r.RELEASE.name}</div>
      <div style="display:flex;align-items:center;gap:26px;margin:22px 0 6px">
        <div style="width:118px;height:118px;filter:drop-shadow(0 12px 20px rgba(0,0,0,.6))">${r.donutSvg(118)}</div>
        <div class="display" style="font-size:104px;line-height:.92">Donut<br><span class="grad">Teams</span></div>
      </div>
      <div class="rule" style="width:72%;margin:26px 0 24px"></div>
      <div style="font-size:31px;line-height:1.35;color:var(--text)">Ranks, allies, a bank, a chest and homes.<br>Level up and top the board together. <b class="hi">Free.</b></div>
      <div style="margin-top:30px;display:flex;align-items:center">
        <span class="pill"><i></i>Paper</span><span class="pill"><i></i>Folia</span><span class="pill"><i></i>Purpur</span>
        <span style="font-family:DoDisplay;font-weight:700;font-size:26px;color:var(--gold-hi);margin-left:12px">${r.RANGE}</span>
      </div>
    </div>`) }),

  menu: card('menu', () => ({
    kicker: 'The Team Menu', title: 'Your whole team in one menu',
    points: [
      'Members, homes, bank, chest, allies, levels and the leaderboard',
      'Every title, slot, item and line in <code class="inline">menus.yml</code>',
      '<b class="hi">Native dialogs</b> on 1.21.7+ instead of typing in chat',
      'And a command for everything, with tab completion',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('menu-main', 600, 760, 40, '/team')}
      ${inset('menu-main-info-tip', 380, 420, 40, 'Team info', 'gold')}
    </div>`,
  })),

  create: card('create', () => ({
    kicker: 'Creating a Team', title: 'A team in seconds',
    points: [
      'Click <b class="hi">Create a team</b>, type a name and a tag',
      'A real text box on 1.21.7+; a chat prompt before that',
      'Name rules, blocked words and an optional cost',
      'The whole server hears about it',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('dialog-create-filled', 850, 560, 40, 'The create dialog')}
      ${inset('created-lines', 820, 80, 30, 'Founded')}
    </div>`,
  })),

  ranks: card('ranks', () => ({
    kicker: 'Ranks', title: 'Owner, officers, members',
    points: [
      '<b class="hi">Twelve permissions</b> per member, one click each',
      'Rank defaults in <code class="inline">config.yml</code>',
      'Only higher ranks manage lower ones',
      '<code class="inline">/team promote</code>, <code class="inline">demote</code>, <code class="inline">transfer</code>',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('menu-member', 560, 740, 40, 'A member')}
      ${inset('menu-member-perm-tip', 360, 300, 60, 'One permission')}
    </div>`,
  })),

  allies: card('allies', () => ({
    kicker: 'Allies & Enemies', title: 'Make friends. Make enemies.',
    points: [
      'Ally requests with a <b class="hi">clickable accept</b>',
      'War declarations, and peace',
      '<b class="hi">Ally chat</b>; allies can\'t hurt each other',
      'Relation colours for TAB and name tags (PlaceholderAPI)',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('menu-relations', 560, 640, 40, '/team relations')}
      ${inset('ally-request-line', 820, 60, 170, 'An ally request', 'sky')}
      ${inset('alliance-war-lines', 820, 120, 0, 'Alliance and war', 'red')}
    </div>`,
  })),

  bank: card('bank', () => ({
    kicker: 'Bank & Levels', title: 'Save up, level up',
    points: [
      '<code class="inline">/team deposit 2.5k</code> or one click in the menu',
      'History: who put in or took out what',
      '<b class="hi">Levels</b>: more members, homes and chest rows',
      'Vault or VaultUnlocked',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('menu-bank', 560, 600, 40, '/team bank')}
      ${inset('menu-level-tip', 360, 360, 30, 'The next level', 'gold')}
    </div>`,
  })),

  chest: card('chest', () => ({
    kicker: 'Team Chest', title: 'One chest for the whole team',
    points: [
      '<b class="hi">One to six rows</b>, set by the team level',
      'Several members at once on Paper; one at a time on Folia',
      'Saved on close and every minute, in Paper\'s versioned format',
      'On disband, the items go to the owner',
    ],
    art: `<div style="position:relative;height:780px;display:flex;flex-direction:column;justify-content:center">
      ${shotArt('chest', 850, 560, '')}
      ${r.label('/team chest', 'left:24px;top:calc(50% - 160px)')}
    </div>`,
  })),

  homes: card('homes', () => ({
    kicker: 'Team Homes', title: 'Home, together',
    points: [
      '<b class="hi">Named homes</b>: as many as the level allows',
      'A title countdown; moving or damage cancels',
      'Cooldown, sounds, particles, disabled worlds',
      'A homes menu to pick one',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('home-warmup-scene', 850, 480, 40, 'The warmup')}
      ${inset('home-landed-scene', 430, 250, 0, 'Back at base')}
    </div>`,
  })),

  joining: card('joining', () => ({
    kicker: 'Joining', title: 'Join with one click',
    points: [
      'Invites with <b class="hi">[Accept]</b> and <b class="hi">[Decline]</b>',
      '<b class="hi">Join requests</b> with buttons for officers',
      '<b class="hi">Open teams</b>: anyone can join',
      'Invite from a menu of online players',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('menu-members', 560, 600, 40, 'Members')}
      ${inset('invite-received-line', 820, 60, 170, 'An invite', 'gold')}
      ${inset('request-line', 820, 60, 30, 'A join request', 'sky')}
    </div>`,
  })),

  chat: card('chat', () => ({
    kicker: 'Chat', title: 'Talk to your team',
    points: [
      '<code class="inline">/tc</code> and <code class="inline">/ac</code>, a toggle, or a quick prefix',
      'Ranks and tags in every line',
      'Online and offline notices, a message of the day',
      'Chat spy for staff',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('team-chat-lines', 850, 220, 120, 'Team and ally chat')}
      ${inset('joined-chat-lines', 850, 260, 40, 'Your team grows', 'sky')}
    </div>`,
  })),

  leaderboards: card('leaderboards', () => ({
    kicker: 'Leaderboards', title: 'Top the board',
    points: [
      'Kills, deaths and KDR for every team',
      '<code class="inline">/team top</code> by kills, bank, level or members',
      '<b class="hi">Boards in your world</b>, no hologram plugin',
      'Top-list placeholders for scoreboards',
    ],
    art: `<div style="position:relative;height:780px">
      ${main('board-scene', 850, 500, 40, 'A board at the base')}
      ${inset('menu-top-tip', 380, 300, 0, '/team top', 'gold')}
    </div>`,
  })),

  yours: card('yours', () => ({
    kicker: 'Make It Yours', title: 'Your server, your rules',
    points: [
      'Team <b class="hi">colour, icon, tag, description</b> and MOTD',
      'Levels, ranks, costs and limits in <code class="inline">config.yml</code>',
      'Every message in MiniMessage or <code class="inline">&</code> codes',
      'Every menu in <code class="inline">menus.yml</code>',
    ],
    art: `<div style="position:relative;height:780px">
      <div style="position:absolute;left:0;top:10px">${r.codePanel('config.yml', r.yaml(r.yamlSection('src/main/resources/config.yml', 'levels')
        .filter((line) => !line.trim().startsWith('#'))), { width: 760, size: 18 })}</div>
      ${inset('menu-settings', 520, 420, 0, '/team settings', 'gold')}
    </div>`,
  })),

  integrations: card('integrations', () => ({
    kicker: 'Integrations', title: 'Plays well with others',
    points: [
      '<b class="hi">PlaceholderAPI</b>, with relational placeholders',
      '<b class="hi">LuckPerms</b> contexts by team and rank',
      '<b class="hi">Vault</b>, MariaDB and MySQL',
      'An API with eleven cancellable events',
    ],
    art: (() => {
      const papi = ['%donutteams_tag_formatted%', '%donutteams_role%', '%donutteams_top_kills_1_name%',
        '%rel_donutteams_relation_color%'].map((p) => `<span style="color:var(--gold-hi)">${p}</span>`).join('\n');
      const api = `<span style="color:var(--accent-hi)">@EventHandler</span>
<span style="color:var(--accent-hi)">public void</span> onWar(TeamRelationChangeEvent event) {
    <span style="color:var(--accent-hi)">if</span> (event.getTo() == Relation.ENEMY &amp;&amp; peaceWeek())
        event.setCancelled(<span style="color:var(--accent-hi)">true</span>);
}`;
      const badge = (name) => `<span class="pill" style="margin:0 10px 12px 0"><i></i>${name}</span>`;
      return `<div style="position:relative;height:780px;display:flex;flex-direction:column;justify-content:center;gap:24px">
        <div style="display:flex;flex-wrap:wrap;width:820px">${['PlaceholderAPI', 'LuckPerms', 'Vault', 'VaultUnlocked', 'MariaDB', 'MySQL', 'DonutCore'].map(badge).join('')}</div>
        ${r.codePanel('PlaceholderAPI', papi, { width: 600, size: 19 })}
        ${r.codePanel('Developer API', api, { width: 820, size: 18 })}
      </div>`;
    })(),
  })),

  upgrade: card('upgrade', () => ({
    kicker: 'Upgrading from 1.x', title: 'Upgrade in place',
    points: [
      'Teams, members, invites and homes kept',
      'Your config values carried over',
      'Old files kept as <code class="inline">config-1.x.yml</code>',
      'Nothing to do but replace the jar',
    ],
    art: `<div style="position:relative;height:780px;display:flex;flex-direction:column;justify-content:center">
      ${r.codePanel('Console · first start of 2.0 on a 1.0.1 server', r.consoleText('upgrade.txt').map(r.escapeHtml).join('\n'), { width: 850, size: 16, wrap: true })}
    </div>`,
  })),

  compatibility: card('compatibility', () => ({
    kicker: 'Compatibility', title: 'Tested from 1.20 to 26.3',
    points: [
      'Paper, Folia and Purpur, one jar',
      `Minecraft ${r.RANGE}`,
      'Bots play every feature on each server shown',
      'SQLite and MariaDB; an upgrade from 1.0.1',
    ],
    art: (() => {
      const data = r.compat();
      const targets = data ? data.targets : [];
      const tile = (t) => `<div class="panel" style="padding:12px 16px 10px;border-color:${t.ok ? 'rgba(73,214,160,.5)' : 'rgba(255,107,107,.6)'}">
        <div style="font-family:DoDisplay;font-weight:700;font-size:13px;letter-spacing:.16em;text-transform:uppercase;color:var(--dim)">${t.project}${t.variant ? ' · ' + t.variant : ''}</div>
        <div style="font-family:DoDisplay;font-weight:800;font-size:28px;color:var(--hi);line-height:1.1">${t.version}</div>
        <div style="font-size:15px;color:${t.ok ? 'var(--mint)' : 'var(--red)'};font-weight:600;margin-top:3px">${t.ok ? '✔' : '✖'} ${t.passed}/${t.steps} · Java ${t.java}${t.upgrade ? ' · from 1.0.1' : ''}</div></div>`;
      return `<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;width:850px">${targets.map(tile).join('')}</div>
        <div style="margin-top:16px;font-size:18px;color:var(--dim)">Donut Teams ${data ? data.plugin : ''} · automated run of tools/smoke on ${data ? data.date : ''}</div>`;
    })(),
  })),
};

void command;
void comment;
void at;
for (const [key, title] of Object.entries(HEADERS)) {
  assets[`header-${key}`] = () => ({ width: 1400, height: 120, transparent: true, html: r.header(title) });
}
module.exports = assets;
