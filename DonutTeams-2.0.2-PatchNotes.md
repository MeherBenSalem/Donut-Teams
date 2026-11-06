# Donut Teams 2.0.2

## Fixed

* **A team didn't see the teams that declared it an enemy.** Declaring an enemy is one-sided: only the declaring team
  stores it. The Allies & enemies menu of the other team only listed its own declarations, so the war never showed
  there. That menu now also lists teams that declared you an enemy ("They declared you an enemy"); clicking one
  opens its profile. Only the declaring team can make peace, as before. The new menu item is `enemy-of-us` in
  `menus.yml`; menus.yml files from older versions use the `enemy` item for it.

No storage or API changes. 2.0.1 data loads as before.
