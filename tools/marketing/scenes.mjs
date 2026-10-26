// The capture scenes, in order. Every action goes through the real plugin: commands typed by the client (Moonbeam)
// or the bots, menu clicks with the mouse, and dialogs typed with the keyboard. The world, the base and the money
// for the bank are set up with console commands and the test fixture.
//
// Each scene is a function of a shared context, so `capture.mjs --lab` can rerun one by hand:
//   const m = await import(`file://${repo}/tools/marketing/scenes.mjs?${Date.now()}`);
//   globalThis.ctx ??= m.context({ server, bot, lab });
//   await m.SCENES.menus(globalThis.ctx);
import fs from 'node:fs';
import path from 'node:path';
import { x, sleep, slot, hover, clickSlot, park, screenshot, record, gif } from './capture.mjs';

const P = 'Moonbeam';

// Where each bot stands around the base centre: [dx, dz, yaw]. Nightbeam in front, Sunfire right, Ravens left.
const SPOTS = {
  Kiri: [-1.4, -0.1, -15], Ember: [2.05, -0.3, 15], Pixel: [3.55, -0.2, 30],
  Sol: [1.7, -2.6, 8], Nova: [4.05, -3.0, 22],
  Rook: [-3.8, -1.6, -30], Vex: [-1.0, -2.6, -8], Ash: [-2.1, 1.4, -35],
};

export function context({ server, bot, lab, consoleDir }) {
  const ctx = { server, bot, lab, consoleDir, P };
  ctx.say = (who, line) => server.lab(`sudo ${who} ${line}`);
  ctx.chatFree = () => sleep(11_000); // chat lines fade after ten seconds
  /** Types a chat line or command in the real client. */
  ctx.type = async (text) => {
    x('key', 't');
    await sleep(500);
    x('type', text);
    await sleep(300);
    x('key', 'Return');
    await sleep(900);
  };
  /** Teleports a player relative to the base centre. */
  ctx.tp = async (who, dx, dz, yaw = 0, pitch = 0, dy = 0) => {
    server.command(`tp ${who} ${ctx.px + dx} ${ctx.py + dy} ${ctx.pz + dz} ${yaw} ${pitch}`);
    await sleep(250);
  };
  /** Puts a bot on its spot, facing the camera (which stands at 0.5, 4.5 looking north). */
  ctx.place = (who) => ctx.tp(who, ...SPOTS[who], 0);
  /** Opens a menu with a command and waits for it to draw. */
  ctx.open = async (command) => {
    await ctx.say(P, command);
    await sleep(2500);
  };
  ctx.close = async () => {
    x('key', 'Escape');
    await sleep(800);
  };
  return ctx;
}

export const SCENES = {
  // ── The base: a camp in a cherry grove with team banners ─────────────────────────────────────────────────────
  async base(ctx) {
    const { server } = ctx;
    server.mark();
    server.command('locate biome minecraft:cherry_grove');
    const located = await server.waitFor(/cherry_grove is at \[(-?\d+), [^,]+, (-?\d+)\]/, 120_000, 'locate');
    const px = Number(located[1]);
    const pz = Number(located[2]);
    server.command(`spreadplayers ${px} ${pz} 0 1 false ${P}`);
    await sleep(2500);
    const surface = await server.lab(`pos ${P}`);
    const py = Math.floor(Number(surface.split(' ')[4]));
    Object.assign(ctx, { px, py, pz });
    server.command(`forceload add ${px - 16} ${pz - 16} ${px + 16} ${pz + 16}`);
    await sleep(4000);
    for (const command of [
      `fill ${px - 7} ${py} ${pz - 7} ${px + 7} ${py + 9} ${pz + 7} air`,
      `fill ${px - 7} ${py - 3} ${pz - 7} ${px + 7} ${py - 2} ${pz + 7} dirt`,
      `fill ${px - 7} ${py - 1} ${pz - 7} ${px + 7} ${py - 1} ${pz + 7} grass_block`,
      `fill ${px - 5} ${py - 1} ${pz - 5} ${px + 5} ${py - 1} ${pz + 5} stripped_cherry_wood`,
      `fill ${px - 4} ${py - 1} ${pz - 4} ${px + 4} ${py - 1} ${pz + 4} cherry_planks`,
      `fill ${px - 1} ${py - 1} ${pz - 1} ${px + 1} ${py - 1} ${pz + 1} polished_andesite`,
      `setblock ${px} ${py} ${pz} light_blue_banner[rotation=8]`,
      ...[[-5, -5], [5, -5], [-5, 5], [5, 5]].flatMap(([dx, dz]) => [
        `fill ${px + dx} ${py} ${pz + dz} ${px + dx} ${py + 2} ${pz + dz} stripped_cherry_log`,
        `setblock ${px + dx} ${py + 3} ${pz + dz} lantern`,
      ]),
      `fill ${px - 5} ${py} ${pz} ${px - 5} ${py + 1} ${pz} stripped_cherry_log`,
      `fill ${px + 5} ${py} ${pz} ${px + 5} ${py + 1} ${pz} stripped_cherry_log`,
      `setblock ${px - 4} ${py + 1} ${pz} light_blue_wall_banner[facing=east]`,
      `setblock ${px + 4} ${py + 1} ${pz} light_blue_wall_banner[facing=west]`,
      `setblock ${px} ${py} ${pz - 4} ender_chest[facing=south]`,
      `setblock ${px - 2} ${py} ${pz - 4} barrel[facing=up]`,
      `setblock ${px + 2} ${py} ${pz - 4} crafting_table`,
    ]) server.command(command);
    await sleep(1500);
  },

  // The scenery behind every card: the base from above, HUD hidden.
  async scenery(ctx) {
    const { server, tp } = ctx;
    server.command(`gamemode spectator ${P}`);
    await tp(P, 9, 9, 135, 28, 5);
    await sleep(45_000);
    x('key', 'F1');
    await sleep(1500);
    screenshot('scenery');
    x('key', 'F1');
    server.command(`gamemode survival ${P}`);
    await tp(P, 0.5, 4.5, 180, 10);
    await sleep(5000);
  },

  // ── An invite before Moonbeam has a team ─────────────────────────────────────────────────────────────────────
  async invite(ctx) {
    const { bot, say, tp, type, chatFree } = ctx;
    await tp(P, 0.5, 4.5, 180, 10);
    await bot('Rook');
    await bot('Vex');
    await ctx.place('Rook');
    await ctx.place('Vex');
    await say('Rook', 'team create Ravens RVN');
    await say('Rook', 'team color red');
    await say('Rook', 'team invite Vex');
    await say('Vex', 'team join Ravens');
    await chatFree();
    await say('Rook', `team invite ${P}`);
    await sleep(1500);
    screenshot('invite-received');
    await type('/team decline Ravens');
    await chatFree();
  },

  // ── The "no team" menu and the create dialog ─────────────────────────────────────────────────────────────────
  async create(ctx) {
    const { server, open } = ctx;
    await open('team');
    await park();
    screenshot('menu-no-team');
    await hover(11, 3);
    screenshot('menu-no-team-create');
    const clip = record('create', 14);
    await sleep(800);
    await clickSlot(11, 3, 2500);
    screenshot('dialog-create');
    // The dialog: click the name box, type, then the tag box.
    x('click', ...ctx.dialog.name);
    await sleep(300);
    x('type', 'Nightbeam');
    await sleep(500);
    x('click', ...ctx.dialog.tag);
    await sleep(300);
    x('type', 'NB');
    await sleep(800);
    await park();
    screenshot('dialog-create-filled');
    // Submit from the keyboard (Tab to the Create button): a mouse click would turn the camera as the dialog closes.
    server.mark();
    x('key', 'Tab');
    await sleep(250);
    x('key', 'Return');
    await server.waitFor(/event create Moonbeam Nightbeam NB/, 20_000, 'create');
    await sleep(1500);
    await hover(4, 6, 2500);
    screenshot('created-menu');
    await ctx.close();
    await ctx.tp(P, 0.5, 4.5, 180, 10);
    await sleep(600);
    screenshot('created');
    // The whole screen at half size: the dialog fills it from top to bottom.
    gif([{ raw: await clip, start: 0.3 }], 'create', '1920:1080:0:0', '960:540');
  },

  // ── Members ──────────────────────────────────────────────────────────────────────────────────────────────────
  async members(ctx) {
    const { server, bot, say, tp, chatFree } = ctx;
    const names = ['Kiri', 'Pixel', 'Ember'];
    for (const name of names) {
      await bot(name);
      await ctx.place(name);
    }
    await say(P, 'team color aqua');
    await say(P, 'team description Night owls of the cherry grove');
    await server.lab(`hold ${P} LIGHT_BLUE_BANNER`);
    await say(P, 'team icon');
    await server.lab(`hold ${P} AIR`);
    await chatFree();
    for (const name of names) {
      await say(P, `team invite ${name}`);
      await sleep(500);
      await say(name, 'team join Nightbeam');
      await sleep(500);
    }
    await say(P, 'team promote Kiri');
    await say(P, 'team motd Raid the end city at nine!');
    await sleep(1500);
    screenshot('joined-chat');
    await chatFree();
  },

  // ── The bank and a level ─────────────────────────────────────────────────────────────────────────────────────
  async bank(ctx) {
    const { server, say, chatFree } = ctx;
    await server.lab(`money ${P} 250000`);
    for (const [name, money] of [['Kiri', 60000], ['Pixel', 25000], ['Ember', 15000]]) await server.lab(`money ${name} ${money}`);
    await say(P, 'team deposit 120k');
    await say('Kiri', 'team deposit 45k');
    await say('Pixel', 'team deposit 20k');
    await say('Ember', 'team deposit 12.5k');
    await chatFree();
    await say(P, 'team level up');
    await sleep(1500);
    screenshot('level-up');
    await say('Kiri', 'team deposit 10k');
    await chatFree();
  },

  // ── Allies and enemies ───────────────────────────────────────────────────────────────────────────────────────
  async relations(ctx) {
    const { bot, say, tp, type, chatFree } = ctx;
    await bot('Sol');
    await bot('Nova');
    await ctx.place('Sol');
    await ctx.place('Nova');
    await say('Sol', 'team create Sunfire SUN');
    await say('Sol', 'team color gold');
    await say('Sol', 'team invite Nova');
    await say('Nova', 'team join Sunfire');
    await chatFree();
    await say('Sol', 'team ally Nightbeam');
    await sleep(1500);
    screenshot('ally-request');
    await type('/team ally Sunfire');
    await sleep(1200);
    await say('Rook', 'team enemy Nightbeam');
    await sleep(1500);
    screenshot('alliance-war');
    await chatFree();
  },

  // Kills for the leaderboard: Nightbeam beat the Ravens.
  async kills(ctx) {
    const { server, tp, chatFree } = ctx;
    for (const [killer, victim] of [['Kiri', 'Vex'], ['Ember', 'Rook'], ['Kiri', 'Rook'], ['Pixel', 'Vex'], ['Sol', 'Vex'], ['Rook', 'Pixel']]) {
      await server.lab(`kill ${killer} ${victim}`);
      await sleep(1200);
    }
    await sleep(2000);
    for (const name of ['Rook', 'Vex', 'Pixel']) await ctx.place(name);
    await chatFree();
  },

  // ── Team chat ────────────────────────────────────────────────────────────────────────────────────────────────
  async chat(ctx) {
    const { say, type, chatFree } = ctx;
    await say('Kiri', 'tc Base is ready, everyone home!');
    await sleep(700);
    await say('Pixel', 'tc on my way');
    await sleep(700);
    await type('@nice work team');
    await say('Sol', 'ac Sunfire is with you tonight');
    await sleep(700);
    await say('Ember', 'tc bringing the elytras');
    await sleep(1200);
    screenshot('team-chat');
    await chatFree();
  },

  // ── The menus ────────────────────────────────────────────────────────────────────────────────────────────────
  async menus(ctx) {
    const { open, close } = ctx;
    await open('team');
    await park();
    screenshot('menu-main');
    await hover(4, 6, 1200);
    screenshot('menu-main-info');
    await clickSlot(19, 6, 2500);
    await park();
    screenshot('menu-members');
    await hover(11, 6, 1200);
    screenshot('menu-members-hover');
    await clickSlot(11, 6, 2500);
    await park();
    screenshot('menu-member');
    await hover(30, 6, 1200);
    screenshot('menu-member-perm');
    await close();

    await open('team bank');
    await hover(31, 5, 1500);
    screenshot('menu-bank');
    await close();

    await open('team level');
    await hover(12, 4, 1500);
    screenshot('menu-level');
    await close();

    await open('team relations');
    await hover(10, 6, 1200);
    screenshot('menu-relations');
    await close();

    await open('team top');
    await hover(19, 6, 1200);
    screenshot('menu-top');
    await close();

    await open('team settings');
    await park();
    screenshot('menu-settings');
    await close();
  },

  // The team chest, filled by shift-clicking stacks from the inventory.
  async chest(ctx) {
    const { server, open, close } = ctx;
    server.command(`clear ${P}`);
    // 15 stacks: the hotbar fills first, then the first inventory row.
    for (const [item, amount] of [['DIAMOND', 64], ['DIAMOND', 37], ['NETHERITE_INGOT', 6], ['ENCHANTED_GOLDEN_APPLE', 3],
      ['GOLDEN_APPLE', 32], ['ENDER_PEARL', 16], ['EMERALD_BLOCK', 24], ['ELYTRA', 1], ['TOTEM_OF_UNDYING', 1],
      ['TOTEM_OF_UNDYING', 1], ['FIREWORK_ROCKET', 64], ['EXPERIENCE_BOTTLE', 64], ['NETHERITE_SWORD', 1],
      ['NETHERITE_PICKAXE', 1], ['PURPLE_SHULKER_BOX', 1]]) {
      await server.lab(`give ${P} ${item} ${amount}`);
    }
    await sleep(800);
    await open('team chest');
    const rows = 2;
    for (const inv of [27, 28, 29, 30, 31, 32, 33, 34, 35, 0, 1, 2, 3, 4, 5]) {
      x('shiftclick', ...slot(rows * 9 + inv, rows));
      await sleep(300);
    }
    await park();
    screenshot('chest');
    await close();
  },

  // ── Homes: set one at the base, fly away, come back ──────────────────────────────────────────────────────────
  async homes(ctx) {
    const { server, say, tp, chatFree } = ctx;
    await tp(P, 0.5, 4.5, 180, 8);
    await sleep(1500);
    await say(P, 'team sethome base');
    await chatFree();
    server.command(`tp ${P} ${ctx.px + 70} ${ctx.py + 30} ${ctx.pz + 70} 45 10`);
    server.command(`effect give ${P} minecraft:slow_falling 60 0 true`);
    await sleep(30_000);
    server.mark();
    const clip = record('home', 9);
    await sleep(300);
    await say(P, 'team home base');
    await sleep(1300);
    screenshot('home-warmup');
    await server.waitFor(/event home Moonbeam Nightbeam base/, 15_000, 'home');
    const raw = await clip;
    await sleep(2500);
    screenshot('home-landed');
    gif([{ raw }], 'home');
  },

  // ── The top-teams board in the world ─────────────────────────────────────────────────────────────────────────
  async board(ctx) {
    const { server, say, tp } = ctx;
    // Made by an admin standing at the team banner: the board floats over it.
    server.command(`op ${P}`);
    await tp(P, 0.5, 0.5, 180, 0);
    await sleep(1000);
    await say(P, 'donutteams board create spawn kills');
    server.command(`deop ${P}`);
    await sleep(6000);
    await tp(P, 0.5, 5.0, 180, -20);
    await sleep(8000);
    x('key', 'F1');
    await sleep(1500);
    screenshot('board');
    x('key', 'F1');
  },

  // ── Info in chat, and a join request ─────────────────────────────────────────────────────────────────────────
  async info(ctx) {
    const { bot, say, tp, chatFree } = ctx;
    await tp(P, 0.5, 4.5, 180, 10);
    await chatFree();
    await say(P, 'team info');
    await sleep(1500);
    screenshot('info-chat');
    await chatFree();
    await bot('Ash');
    await ctx.place('Ash');
    await say('Ash', 'team request Nightbeam');
    await sleep(1500);
    screenshot('request-chat');
  },

  // ── The console report ───────────────────────────────────────────────────────────────────────────────────────
  async report(ctx) {
    const { server } = ctx;
    const from = server.lines.length;
    server.command('donutteams version');
    server.command('donutteams info');
    await sleep(2500);
    const report = server.lines.slice(from).map((line) => line.replace(/^\[[^\]]*INFO\]:? ?/, '')).filter((line) => line.trim());
    fs.mkdirSync(ctx.consoleDir, { recursive: true });
    fs.writeFileSync(path.join(ctx.consoleDir, 'admin-info.txt'), report.join('\n') + '\n');
  },
};

/** Where the create dialog's boxes and button are, at GUI scale 3 (measured in --lab). */
export const DIALOG = { name: [960, 366], tag: [960, 495] };

export async function scenes(options) {
  const { server } = options;
  const ctx = context(options);
  ctx.dialog = DIALOG;
  for (const [name, scene] of Object.entries(SCENES)) {
    if (options.only && !options.only.includes(name)) continue;
    console.log(`scene ${name}`);
    // Villagers and bees from the grove wander into shots; boards are text displays and stay.
    if (ctx.px !== undefined) server.command(`execute at ${P} run kill @e[type=!player,type=!text_display,distance=..64]`);
    await scene(ctx);
  }
}
