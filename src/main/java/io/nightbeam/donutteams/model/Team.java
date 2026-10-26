package io.nightbeam.donutteams.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A team as held in memory. Every field may be read from any thread; changes go through the team service, which
 * makes them under its write lock and then saves them.
 */
public final class Team {

    /** Owner first, then officers, then members; within a rank, the earliest to join first. */
    public static final Comparator<TeamMember> ROSTER_ORDER = Comparator
            .comparingInt((TeamMember member) -> -member.role().weight())
            .thenComparingLong(TeamMember::joinedAtMillis)
            .thenComparing(member -> member.name().toLowerCase(Locale.ROOT));

    private final UUID id;
    private volatile String name;
    private volatile String tag;
    private volatile UUID ownerId;
    private final long createdAtMillis;
    private volatile String description = "";
    private volatile String motd = "";
    private volatile TeamColor color = TeamColor.GOLD;
    private volatile String iconData = "";
    private volatile boolean open;
    private volatile boolean friendlyFire;
    private volatile boolean allyFriendlyFire;
    private volatile int level = 1;
    private volatile double balance;
    private volatile int memberCap;
    private final Map<UUID, TeamMember> members = new ConcurrentHashMap<>();
    private final Map<String, TeamHome> homes = new ConcurrentHashMap<>();
    private final Set<UUID> allies = ConcurrentHashMap.newKeySet();
    private final Set<UUID> enemies = ConcurrentHashMap.newKeySet();

    public Team(UUID id, String name, String tag, UUID ownerId, long createdAtMillis) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerId = ownerId;
        this.createdAtMillis = createdAtMillis;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public String tag() {
        return tag;
    }

    public void tag(String tag) {
        this.tag = tag;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public void ownerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public String description() {
        return description;
    }

    public void description(String description) {
        this.description = description == null ? "" : description;
    }

    public String motd() {
        return motd;
    }

    public void motd(String motd) {
        this.motd = motd == null ? "" : motd;
    }

    public TeamColor color() {
        return color;
    }

    public void color(TeamColor color) {
        this.color = color == null ? TeamColor.GOLD : color;
    }

    /** The team icon: a Base64 item, or empty for the default banner. */
    public String iconData() {
        return iconData;
    }

    public void iconData(String iconData) {
        this.iconData = iconData == null ? "" : iconData;
    }

    /** Whether anyone may join without an invite. */
    public boolean open() {
        return open;
    }

    public void open(boolean open) {
        this.open = open;
    }

    public boolean friendlyFire() {
        return friendlyFire;
    }

    public void friendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public boolean allyFriendlyFire() {
        return allyFriendlyFire;
    }

    public void allyFriendlyFire(boolean allyFriendlyFire) {
        this.allyFriendlyFire = allyFriendlyFire;
    }

    public int level() {
        return level;
    }

    public void level(int level) {
        this.level = Math.max(1, level);
    }

    public double balance() {
        return balance;
    }

    public void balance(double balance) {
        this.balance = Math.max(0D, Math.round(balance * 100D) / 100D);
    }

    /** The member cap the owner's slot permission gave, saved while the owner was online (0 = none known). */
    public int memberCap() {
        return memberCap;
    }

    public void memberCap(int memberCap) {
        this.memberCap = Math.max(0, memberCap);
    }

    public Collection<TeamMember> members() {
        return members.values();
    }

    public List<TeamMember> roster() {
        List<TeamMember> list = new ArrayList<>(members.values());
        list.sort(ROSTER_ORDER);
        return list;
    }

    public TeamMember member(UUID playerId) {
        return playerId == null ? null : members.get(playerId);
    }

    public TeamMember owner() {
        return members.get(ownerId);
    }

    public boolean isMember(UUID playerId) {
        return playerId != null && members.containsKey(playerId);
    }

    public void putMember(TeamMember member) {
        members.put(member.playerId(), member);
    }

    public TeamMember removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    public int size() {
        return members.size();
    }

    public Map<String, TeamHome> homes() {
        return Collections.unmodifiableMap(homes);
    }

    public List<TeamHome> homeList() {
        List<TeamHome> list = new ArrayList<>(homes.values());
        list.sort(Comparator.comparing((TeamHome home) -> !home.name().equalsIgnoreCase(TeamHome.DEFAULT))
                .thenComparingLong(TeamHome::createdAtMillis));
        return list;
    }

    public TeamHome home(String name) {
        return name == null ? null : homes.get(name.toLowerCase(Locale.ROOT));
    }

    /** The home a bare {@code /team home} uses: "home" if it exists, else the first one set. */
    public TeamHome defaultHome() {
        TeamHome home = homes.get(TeamHome.DEFAULT);
        if (home != null) {
            return home;
        }
        List<TeamHome> list = homeList();
        return list.isEmpty() ? null : list.get(0);
    }

    public void putHome(TeamHome home) {
        homes.put(home.name().toLowerCase(Locale.ROOT), home);
    }

    public TeamHome removeHome(String name) {
        return homes.remove(name.toLowerCase(Locale.ROOT));
    }

    public Set<UUID> allies() {
        return Collections.unmodifiableSet(allies);
    }

    public Set<UUID> enemies() {
        return Collections.unmodifiableSet(enemies);
    }

    public boolean alliedWith(UUID teamId) {
        return allies.contains(teamId);
    }

    public boolean enemyOf(UUID teamId) {
        return enemies.contains(teamId);
    }

    public void addAlly(UUID teamId) {
        enemies.remove(teamId);
        allies.add(teamId);
    }

    public void addEnemy(UUID teamId) {
        allies.remove(teamId);
        enemies.add(teamId);
    }

    public void clearRelation(UUID teamId) {
        allies.remove(teamId);
        enemies.remove(teamId);
    }

    public int kills() {
        int total = 0;
        for (TeamMember member : members.values()) {
            total += member.kills();
        }
        return total;
    }

    public int deaths() {
        int total = 0;
        for (TeamMember member : members.values()) {
            total += member.deaths();
        }
        return total;
    }

    /** Kills per death, with deaths floored at one. */
    public double kdr() {
        return kills() / (double) Math.max(1, deaths());
    }
}
