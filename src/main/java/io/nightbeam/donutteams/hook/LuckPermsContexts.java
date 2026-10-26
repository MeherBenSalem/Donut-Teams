package io.nightbeam.donutteams.hook;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.service.TeamService;
import java.util.Locale;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * LuckPerms contexts, so permissions can depend on a player's team: {@code donutteams:has-team=true},
 * {@code donutteams:role=owner|officer|member|none} and {@code donutteams:team=<name>}. Only loaded when LuckPerms
 * is installed; reads the thread-safe team cache only.
 */
public final class LuckPermsContexts implements ContextCalculator<Player> {

    private final TeamService teams;
    private LuckPerms luckPerms;

    private LuckPermsContexts(TeamService teams) {
        this.teams = teams;
    }

    /** Registers the calculator; returns a handle that unregisters it. */
    public static Runnable register(TeamService teams) {
        LuckPermsContexts calculator = new LuckPermsContexts(teams);
        calculator.luckPerms = LuckPermsProvider.get();
        calculator.luckPerms.getContextManager().registerCalculator(calculator);
        return () -> calculator.luckPerms.getContextManager().unregisterCalculator(calculator);
    }

    @Override
    public void calculate(@NotNull Player target, @NotNull ContextConsumer consumer) {
        Team team = teams.teamByPlayer(target.getUniqueId());
        TeamMember member = team == null ? null : team.member(target.getUniqueId());
        consumer.accept("donutteams:has-team", String.valueOf(member != null));
        consumer.accept("donutteams:role", member == null ? "none" : member.role().key());
        if (team != null) {
            consumer.accept("donutteams:team", team.name().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public @NotNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder()
                .add("donutteams:has-team", "true").add("donutteams:has-team", "false")
                .add("donutteams:role", "owner").add("donutteams:role", "officer").add("donutteams:role", "member")
                .add("donutteams:role", "none");
        for (Team team : teams.cache().teams()) {
            builder.add("donutteams:team", team.name().toLowerCase(Locale.ROOT));
        }
        return builder.build();
    }
}
