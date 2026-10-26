package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Online players without a team, to invite with one click. */
final class InvitePickerMenu extends Menu {

    InvitePickerMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "invite-picker");
    }

    @Override
    protected void build() {
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(viewer) && teams().teamByPlayer(online.getUniqueId()) == null && viewer.canSee(online)) {
                candidates.add(online);
            }
        }
        candidates.sort(Comparator.comparing(player -> player.getName().toLowerCase()));
        MenuConfig.Item spec = item("player");
        paged(candidates, target -> render(spec, ItemBuilder.head(target.getUniqueId()), "player", target.getName()),
                (target, click) -> {
                    if (target.isOnline()) {
                        teams().invite(viewer, target);
                    }
                    redraw();
                });
        back(() -> new MembersMenu(plugin, viewer).open());
    }
}
