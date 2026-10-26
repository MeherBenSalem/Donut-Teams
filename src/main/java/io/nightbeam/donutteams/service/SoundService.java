package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.model.Team;
import io.nightbeam.donutteams.model.TeamMember;
import io.nightbeam.donutteams.scheduler.FoliaScheduler;
import io.nightbeam.donutteams.util.Sounds;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/** Sounds from config.yml {@code sounds}: {@code NAME} or {@code NAME volume pitch}; empty = silent. */
public final class SoundService {

    private record Spec(Sound sound, float volume, float pitch) {
    }

    private final FoliaScheduler scheduler;
    private volatile Map<String, Optional<Spec>> specs = Map.of();
    private volatile ConfigurationSection section;

    public SoundService(FoliaScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void load(ConfigurationSection section) {
        this.section = section;
        this.specs = new ConcurrentHashMap<>();
    }

    /** Plays a sound to the player; call on the player's thread. */
    public void play(Player player, String key) {
        Spec spec = spec(key);
        if (spec != null && player != null && player.isOnline()) {
            player.playSound(player.getLocation(), spec.sound, spec.volume, spec.pitch);
        }
    }

    public void playTeam(Team team, String key) {
        for (TeamMember member : team.members()) {
            Player online = Bukkit.getPlayer(member.playerId());
            if (online != null) {
                scheduler.forPlayer(online, () -> play(online, key));
            }
        }
    }

    private Spec spec(String key) {
        Map<String, Optional<Spec>> current = specs;
        Optional<Spec> cached = current.get(key);
        if (cached != null) {
            return cached.orElse(null);
        }
        Spec spec = parse(section == null ? null : section.getString(key));
        current.put(key, Optional.ofNullable(spec));
        return spec;
    }

    static Spec parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split("\\s+");
        Sound sound = Sounds.lookup(parts[0]);
        if (sound == null) {
            return null;
        }
        float volume = parts.length > 1 ? parseFloat(parts[1], 1f) : 1f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1f) : 1f;
        return new Spec(sound, volume, pitch);
    }

    private static float parseFloat(String raw, float fallback) {
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
