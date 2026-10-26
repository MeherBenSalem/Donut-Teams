package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.util.Durations;
import io.nightbeam.donutteams.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** The roster: heads sorted by rank, online status, and invite / request buttons. */
final class MembersMenu extends Menu {

    MembersMenu(DonutTeamsPlugin plugin, Player viewer) {
        super(plugin, viewer, "members");
    }

    @Override
    protected Object[] placeholders() {
        Team team = team();
        return team == null ? new Object[0] : format().team(team);
    }

    @Override
    protected void build() {
        Team team = team();
        if (team == null) {
            new NoTeamMenu(plugin, viewer).open();
            return;
        }
        TeamMember me = team.member(viewer.getUniqueId());
        MenuConfig.Item spec = item("member");
        paged(team.roster(), member -> render(spec, ItemBuilder.head(member.playerId()), memberValues(member, me)),
                (member, click) -> {
                    if (canManage(me, member)) {
                        new MemberMenu(plugin, viewer, member.playerId()).open();
                    }
                });
        if (me != null && me.has(TeamPermission.INVITE)) {
            place("invite", () -> new InvitePickerMenu(plugin, viewer).open());
            place("requests", () -> new RequestsMenu(plugin, viewer).open(), "count", teams().requests(team.id()).size());
            place("sent", () -> new SentInvitesMenu(plugin, viewer).open(), "count", teams().cache().teamInvites(team.id()).size());
        }
        back(() -> new MainMenu(plugin, viewer).open());
    }

    private Object[] memberValues(TeamMember member, TeamMember me) {
        return memberValues(plugin, member, canManage(me, member) ? messages().component("menu.manage-hint") : "");
    }

    static Object[] memberValues(DonutTeamsPlugin plugin, TeamMember member, Object manage) {
        long now = System.currentTimeMillis();
        Player online = Bukkit.getPlayer(member.playerId());
        Object status = online != null
                ? plugin.messages().component("format.online")
                : plugin.messages().component("format.offline", "ago", Durations.ago(member.lastSeenMillis(), now));
        return new Object[] {
                "player", member.name(),
                "role", plugin.teams().format().roleComponent(member.role()),
                "status", status,
                "joined", Durations.ago(member.joinedAtMillis(), now),
                "kills", member.kills(),
                "deaths", member.deaths(),
                "manage", manage};
    }

    static boolean canManage(TeamMember me, TeamMember target) {
        if (me == null || target == null || me.playerId().equals(target.playerId())) {
            return false;
        }
        return me.isOwner() || (me.has(TeamPermission.KICK) && me.role().outranks(target.role()));
    }
}
