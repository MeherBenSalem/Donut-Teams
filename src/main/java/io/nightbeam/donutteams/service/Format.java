package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.config.LevelTable;
import io.nightbeam.donutteams.config.Messages;
import io.nightbeam.donutteams.config.Settings;
import io.nightbeam.donutteams.hook.Economy;
import io.nightbeam.donutteams.model.Relation;
import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.model.TeamPermission;
import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.util.Amounts;
import io.nightbeam.donutteams.util.SlotPermissionParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Placeholders and display text shared by messages, menus and PlaceholderAPI. */
public final class Format {

    private final Supplier<Settings> settings;
    private final Messages messages;
    private final Economy economy;

    public Format(Supplier<Settings> settings, Messages messages, Economy economy) {
        this.settings = settings;
        this.messages = messages;
        this.economy = economy;
    }

    public Settings settings() {
        return settings.get();
    }

    public Messages messages() {
        return messages;
    }

    public Economy economy() {
        return economy;
    }

    public LevelTable.Level level(Team team) {
        return settings.get().levels.get(team.level());
    }

    /** Member cap: the level's cap, raised by the owner's donutteams.slots.<n>, never above max-members-cap. */
    public int maxMembers(Team team) {
        Settings s = settings.get();
        int levelCap = s.levelsEnabled ? level(team).maxMembers() : s.levels.get(1).maxMembers();
        return SlotPermissionParser.cap(levelCap, team.memberCap(), s.maxMembersCap);
    }

    public int maxHomes(Team team) {
        Settings s = settings.get();
        return s.levelsEnabled ? level(team).maxHomes() : s.levels.get(1).maxHomes();
    }

    public int chestRows(Team team) {
        Settings s = settings.get();
        return s.levelsEnabled ? level(team).chestRows() : s.levels.get(1).chestRows();
    }

    public NamedTextColor textColor(Team team) {
        NamedTextColor color = NamedTextColor.NAMES.value(team.color().tag());
        return color == null ? NamedTextColor.GOLD : color;
    }

    /** The team name in its colour. */
    public Component teamName(Team team) {
        return Component.text(team.name(), textColor(team));
    }

    /** {@code [TAG]} in the team colour, from messages.yml {@code format.tag}. */
    public Component tag(Team team) {
        return messages.component("format.tag", "tag", Component.text(team.tag(), textColor(team)));
    }

    public String money(double amount) {
        return economy.available() ? economy.format(amount) : "$" + Amounts.full(amount);
    }

    public String role(TeamRole role) {
        return messages.raw("roles." + role.key());
    }

    public Component roleComponent(TeamRole role) {
        return messages.component("roles." + role.key());
    }

    public String permission(TeamPermission permission) {
        return messages.raw("permissions." + permission.key() + ".name");
    }

    public Component relation(Relation relation) {
        return messages.component("relation-names." + relation.key());
    }

    public TextColor relationColor(Relation relation) {
        return switch (relation) {
            case TEAMMATE -> NamedTextColor.GREEN;
            case ALLY -> NamedTextColor.AQUA;
            case ENEMY -> NamedTextColor.RED;
            case NEUTRAL -> NamedTextColor.GRAY;
        };
    }

    public String onOff(boolean on) {
        return messages.raw(on ? "format.on" : "format.off");
    }

    public Component onOffComponent(boolean on) {
        return messages.component(on ? "format.on" : "format.off");
    }

    public int online(Team team) {
        int count = 0;
        for (TeamMember member : team.members()) {
            Player player = Bukkit.getPlayer(member.playerId());
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    public String kdr(Team team) {
        return String.format(Locale.ROOT, "%.2f", team.kdr());
    }

    /** The common team placeholders, as key/value pairs for {@link Messages}. */
    public Object[] team(Team team, Object... extra) {
        List<Object> values = new ArrayList<>();
        TeamMember owner = team.owner();
        LevelTable.Level next = settings.get().levels.next(team.level());
        Collections.addAll(values,
                "team", team.name(),
                "team_display", teamName(team),
                "tag", team.tag(),
                "tag_display", tag(team),
                "team_color", team.color().tag(),
                "color_name", team.color().displayName(),
                "owner", owner == null ? "?" : owner.name(),
                "members", team.size(),
                "max_members", maxMembers(team),
                "online", online(team),
                "level", team.level(),
                "max_level", settings.get().levels.max(),
                "next_level_cost", next == null ? "-" : money(next.cost()),
                "balance", money(team.balance()),
                "balance_short", Amounts.shortForm(team.balance()),
                "kills", team.kills(),
                "deaths", team.deaths(),
                "kdr", kdr(team),
                "homes", team.homes().size(),
                "max_homes", maxHomes(team),
                "allies", team.allies().size(),
                "enemies", team.enemies().size(),
                "description", team.description().isEmpty() ? messages.component("format.no-description") : team.description(),
                "motd", team.motd().isEmpty() ? messages.component("format.no-motd") : team.motd(),
                "open", messages.component(team.open() ? "format.open" : "format.invite-only"),
                "pvp", onOffComponent(team.friendlyFire()),
                "ally_pvp", onOffComponent(team.allyFriendlyFire()),
                "chest_rows", chestRows(team));
        Collections.addAll(values, extra);
        return values.toArray();
    }

    public static Object[] concat(Object[] first, Object... second) {
        Object[] out = new Object[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }
}
