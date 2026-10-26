package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.service.Format;
import io.nightbeam.donutteams.service.TopService;
import org.bukkit.entity.Player;

/** /team for a member: everything about the team in one place. */
final class MainMenu extends Menu {

    MainMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "main");
    }

    @Override
    protected Object[] placeholders() {
        Team team = team();
        if (team == null) {
            return new Object[0];
        }
        TeamMember member = team.member(viewer.getUniqueId());
        String chatMode = plugin.chat().allyToggled(viewer.getUniqueId()) ? "chat-ally"
                : plugin.chat().teamToggled(viewer.getUniqueId()) ? "chat-team" : "chat-public";
        return format().team(team,
                "role", member == null ? "" : format().roleComponent(member.role()),
                "rank_kills", String.valueOf(plugin.top().rank(team.id(), TopService.Stat.KILLS)),
                "chat_mode", messages().component("menu." + chatMode));
    }

    @Override
    protected void build() {
        Team team = team();
        if (team == null) {
            new NoTeamMenu(plugin, viewer).open();
            return;
        }
        place("info", special(item("info"), team), null);
        place("members", () -> new MembersMenu(plugin, viewer).open());
        place("homes", () -> new HomesMenu(plugin, viewer).open());
        if (plugin.bank().enabled()) {
            place("bank", () -> new BankMenu(plugin, viewer).open());
        }
        if (plugin.settings().chestEnabled) {
            place("chest", () -> plugin.chests().open(viewer));
        }
        if (plugin.settings().alliesEnabled || plugin.settings().enemiesEnabled) {
            place("relations", () -> new RelationsMenu(plugin, viewer).open());
        }
        if (plugin.settings().levelsEnabled) {
            place("level", () -> new LevelMenu(plugin, viewer).open());
        }
        place("top", () -> new TopMenu(plugin, viewer, TopService.Stat.KILLS).open());
        if (plugin.settings().chatEnabled) {
            place("chat", () -> {
                boolean ally = plugin.chat().allyToggled(viewer.getUniqueId());
                boolean teamChat = plugin.chat().teamToggled(viewer.getUniqueId());
                if (ally) {
                    plugin.chat().allyChat(viewer, null);
                } else if (teamChat && plugin.settings().allyChat && !team.allies().isEmpty()) {
                    plugin.chat().allyChat(viewer, null);
                } else {
                    plugin.chat().teamChat(viewer, null);
                }
                open();
            });
        }
        place("settings", () -> new SettingsMenu(plugin, viewer).open());
        TeamMember member = team.member(viewer.getUniqueId());
        if (member != null && member.isOwner()) {
            place("disband", () -> new ConfirmMenu(plugin, viewer,
                    messages().raw("menu.confirm-disband"), messages().raw("menu.confirm-disband-detail"),
                    () -> teams().disband(viewer, true), () -> open(), Format.concat(format().team(team))).open());
        } else {
            place("leave", () -> new ConfirmMenu(plugin, viewer,
                    messages().raw("menu.confirm-leave"), messages().raw("menu.confirm-leave-detail"),
                    () -> teams().leave(viewer), () -> open(), format().team(team)).open());
        }
    }
}
