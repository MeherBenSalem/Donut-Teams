package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.model.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/** Team leaderboards, worked out every few seconds from memory (never on a player's request). */
public final class TopService {

    public enum Stat {
        KILLS(team -> team.kills()),
        BALANCE(Team::balance),
        LEVEL(team -> team.level() * 1_000_000D + team.balance() / 1_000_000_000D),
        MEMBERS(team -> team.size());

        private final ToDoubleFunction<Team> score;

        Stat(ToDoubleFunction<Team> score) {
            this.score = score;
        }

        public double score(Team team) {
            return score.applyAsDouble(team);
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Stat parse(String raw, Stat fallback) {
            if (raw == null) {
                return fallback;
            }
            String value = raw.trim().toUpperCase(Locale.ROOT);
            if (value.equals("MONEY") || value.equals("BANK")) {
                return BALANCE;
            }
            if (value.equals("SIZE")) {
                return MEMBERS;
            }
            try {
                return Stat.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }

    private final TeamCache cache;
    private volatile Map<Stat, List<Team>> top = new EnumMap<>(Stat.class);

    public TopService(TeamCache cache) {
        this.cache = cache;
    }

    public void refresh() {
        Map<Stat, List<Team>> next = new EnumMap<>(Stat.class);
        List<Team> all = new ArrayList<>(cache.teams());
        for (Stat stat : Stat.values()) {
            List<Team> sorted = new ArrayList<>(all);
            sorted.sort(Comparator.comparingDouble((Team team) -> -stat.score(team))
                    .thenComparingLong(Team::createdAtMillis));
            next.put(stat, List.copyOf(sorted));
        }
        top = next;
    }

    /** All teams, best first, as of the last refresh. */
    public List<Team> top(Stat stat) {
        List<Team> list = top.get(stat);
        if (list == null) {
            refresh();
            list = top.get(stat);
        }
        return list.stream().filter(team -> cache.byId(team.id()) != null).toList();
    }

    /** 1-based position, or 0 when the team is not ranked yet. */
    public int rank(UUID teamId, Stat stat) {
        List<Team> list = top(stat);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(teamId)) {
                return i + 1;
            }
        }
        return 0;
    }
}
