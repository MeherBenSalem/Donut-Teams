// Updates the Modrinth project page from the repository: the summary (marketing/curseforge/summary.txt), the
// description (marketing/curseforge/description.md), the icon and the gallery. Gallery images are added once, matched
// by title, so running it again only replaces the text and the icon.
//
// Env: MODRINTH_ID, MODRINTH_TOKEN (needs the "Write projects" scope), DRY_RUN=true to only print what it would do.
// Run from the repository root: node .github/scripts/modrinth-listing.mjs
import fs from 'node:fs';

const API = 'https://api.modrinth.com/v2';
const id = process.env.MODRINTH_ID;
const token = process.env.MODRINTH_TOKEN;
const dry = process.env.DRY_RUN === 'true';

const GALLERY = [
  { file: 'marketing/images/banner.png', title: 'Donut Teams 2.0', description: 'The Alliance Update', featured: true },
  { file: 'marketing/images/screenshots/menu-main.jpg', title: 'The team menu', description: 'Everything about your team in /team.' },
  { file: 'marketing/images/screenshots/menu-members.jpg', title: 'Members', description: 'Ranks, permissions and invites.' },
  { file: 'marketing/images/screenshots/chest.jpg', title: 'The team chest', description: 'One chest for the whole team.' },
  { file: 'marketing/images/screenshots/board.jpg', title: 'A top-teams board', description: 'A leaderboard that stands in your world.' },
  { file: 'marketing/images/screenshots/team-chat.jpg', title: 'Team and ally chat', description: 'With ranks and tags in every line.' },
];

async function call(method, path, body, type) {
  if (dry) {
    console.log(`dry run: ${method} ${path} (${type}, ${body.length} bytes)`);
    return;
  }
  const response = await fetch(API + path, { method, headers: { Authorization: token, 'Content-Type': type }, body });
  if (!response.ok) {
    throw new Error(`${method} ${path}: ${response.status} ${(await response.text()).slice(0, 300)}`);
  }
  console.log(`${method} ${path}: ${response.status}`);
}

if (!id || (!token && !dry)) {
  throw new Error('MODRINTH_ID and MODRINTH_TOKEN are needed');
}
const project = await (await fetch(`${API}/project/${id}`)).json();
const summary = fs.readFileSync('marketing/curseforge/summary.txt', 'utf8').trim();
const body = fs.readFileSync('marketing/curseforge/description.md', 'utf8');
if (summary.length > 256) {
  throw new Error(`the summary is ${summary.length} characters; Modrinth allows 256`);
}

await call('PATCH', `/project/${id}`, JSON.stringify({ description: summary, body }), 'application/json');
await call('PATCH', `/project/${id}/icon?ext=png`, fs.readFileSync('marketing/images/icon.png'), 'image/png');

const present = new Set((project.gallery || []).map((image) => image.title));
for (const [ordering, image] of GALLERY.entries()) {
  if (present.has(image.title)) {
    console.log(`gallery already has "${image.title}"`);
    continue;
  }
  const ext = image.file.endsWith('.png') ? 'png' : 'jpeg';
  const query = new URLSearchParams({
    ext, featured: String(Boolean(image.featured)), title: image.title, description: image.description, ordering: String(ordering),
  });
  await call('POST', `/project/${id}/gallery?${query}`, fs.readFileSync(image.file), `image/${ext}`);
}
console.log(`https://modrinth.com/plugin/${project.slug} updated${dry ? ' (dry run)' : ''}`);
