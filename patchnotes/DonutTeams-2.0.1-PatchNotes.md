# Donut Teams 2.0.1

## Fixed

* **Spamming menu buttons could lag the server.** Every click in a Donut Teams menu ran its action straight away, so
  an autoclicker on any button made the server rebuild menus and save data hundreds of times a second and raised
  MSPT. Menu clicks are now limited per player: clicks closer together than `menus.click-cooldown-ms` (default
  `150`, about seven a second) are ignored. Normal clicking is unaffected. Set it to `0` to turn the limit off.
  Existing `config.yml` files get the new key on the next start.

No other gameplay, storage or API changes. 2.0.0 data loads as before.
