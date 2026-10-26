package io.nightbeam.donutteams.gui;

import io.nightbeam.donutteams.DonutTeamsPlugin;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.util.ItemBuilder;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** One member: rank changes, kick, ownership and (for the owner) each permission. */
final class MemberMenu extends Menu {

    private final UUID targetId;

    MemberMenu(DonutTeamsPlugin plugin, Player viewer, UUID targetId) {
        super(plugin, viewer, "member");
        this.targetId = targetId;
    }

    @Override
    protected Object[] placeholders() {
        Team team = team();
        TeamMember target = team == null ? null : team.member(targetId);
        if (target == null) {
            return new Object[] {"player", "?"};
        }
        return MembersMenu.memberValues(plugin, target, "");
    }

    @Override
    protected void build() {
        Team team = team();
        TeamMember me = team == null ? null : team.member(viewer.getUniqueId());
        TeamMember target = team == null ? null : team.member(targetId);
        if (target == null || me == null) {
            new MembersMenu(plugin, viewer).open();
            return;
        }
        place("head", ItemBuilder.head(targetId), null);
        boolean owner = me.isOwner();
        if (owner && target.role() == TeamRole.MEMBER) {
            place("promote", () -> {
                teams().promote(viewer, target.name());
                open();
            });
        }
        if (owner && target.role() == TeamRole.OFFICER) {
            place("demote", () -> {
                teams().demote(viewer, target.name());
                open();
            });
        }
        if (MembersMenu.canManage(me, target)) {
            place("kick", () -> new ConfirmMenu(plugin, viewer, messages().raw("menu.confirm-kick"), messages().raw("menu.confirm-kick-detail"),
                    () -> teams().kick(viewer, target.name()), this::open, "player", target.name()).open());
        }
        if (owner) {
            place("transfer", () -> new ConfirmMenu(plugin, viewer, messages().raw("menu.confirm-transfer"),
                    messages().raw("menu.confirm-transfer-detail"),
                    () -> teams().transfer(viewer, target.name(), true), this::open, "player", target.name()).open());
            MenuConfig.Item spec = item("permission");
            if (spec != null && !target.isOwner()) {
                List<Integer> slots = spec.slots();
                TeamPermission[] permissions = TeamPermission.values();
                for (int i = 0; i < permissions.length && i < slots.size(); i++) {
                    TeamPermission permission = permissions[i];
                    boolean on = target.has(permission);
                    ItemStack base = new ItemStack(on ? Material.LIME_DYE : Material.GRAY_DYE);
                    set(slots.get(i), render(spec, base,
                            "perm_name", net.kyori.adventure.text.Component.text(format().permission(permission),
                                    on ? net.kyori.adventure.text.format.NamedTextColor.GREEN
                                            : net.kyori.adventure.text.format.NamedTextColor.GRAY),
                            "perm_description", messages().raw("permissions." + permission.key() + ".description"),
                            "state", format().onOffComponent(on)), (type, slot) -> {
                                teams().togglePermission(viewer, targetId, permission);
                                redraw();
                            });
                }
            }
        }
        back(() -> new MembersMenu(plugin, viewer).open());
    }
}
