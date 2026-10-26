package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.model.TeamRole;
import io.nightbeam.donutteams.util.TeamNameValidator;
import java.util.Collection;
import java.util.List;

/**
 * Pure rules for creating, inviting, joining, leaving and managing members. Free of Bukkit so they can be unit
 * tested; {@link TeamService} turns each denial into a message.
 */
public final class TeamRules {

    private TeamRules() {
    }

    public enum Denial {
        NO_CREATE_PERMISSION,
        ALREADY_IN_TEAM,
        INVALID_NAME,
        INVALID_TAG,
        BLOCKED_NAME,
        NAME_TAKEN,
        NOT_IN_TEAM,
        NO_INVITE_PERMISSION,
        INVITE_SELF,
        ALREADY_INVITED_OR_MEMBER,
        TEAM_FULL,
        OWNER_CANNOT_LEAVE,
        TEAM_NOT_FOUND,
        NO_INVITE,
        INVITE_EXPIRED,
        NOT_A_MEMBER,
        CANNOT_MANAGE_SELF,
        RANK_TOO_LOW,
        ALREADY_HIGHEST,
        ALREADY_LOWEST
    }

    public record PreparedName(String name, String tag) {
    }

    /** Trims the name; without a tag, the tag is the name's letters and digits cut to {@code tagMax}. */
    public static PreparedName prepareCreate(String rawName, String rawTag, int tagMax) {
        String name = TeamNameValidator.normalizeName(rawName);
        String tag = rawTag == null || rawTag.isBlank()
                ? TeamNameValidator.tagFromName(name, tagMax)
                : TeamNameValidator.normalizeTag(rawTag);
        int max = Math.max(1, tagMax);
        if (tag.length() > max) {
            tag = tag.substring(0, max);
        }
        return new PreparedName(name, tag);
    }

    public static Denial createDenial(boolean hasCreatePermission, boolean alreadyInTeam, PreparedName prepared, boolean nameTaken,
                                      int nameMin, int nameMax, int tagMin, int tagMax) {
        return createDenial(hasCreatePermission, alreadyInTeam, prepared, nameTaken, nameMin, nameMax, tagMin, tagMax, List.of());
    }

    public static Denial createDenial(boolean hasCreatePermission, boolean alreadyInTeam, PreparedName prepared, boolean nameTaken,
                                      int nameMin, int nameMax, int tagMin, int tagMax, Collection<String> blocked) {
        if (!hasCreatePermission) {
            return Denial.NO_CREATE_PERMISSION;
        }
        if (alreadyInTeam) {
            return Denial.ALREADY_IN_TEAM;
        }
        Denial invalid = nameDenial(prepared, nameMin, nameMax, tagMin, tagMax, blocked);
        if (invalid != null) {
            return invalid;
        }
        if (nameTaken) {
            return Denial.NAME_TAKEN;
        }
        return null;
    }

    /** Whether a name and tag are acceptable (also used for renames). */
    public static Denial nameDenial(PreparedName prepared, int nameMin, int nameMax, int tagMin, int tagMax, Collection<String> blocked) {
        if (prepared == null || !TeamNameValidator.validName(prepared.name(), nameMin, nameMax)) {
            return Denial.INVALID_NAME;
        }
        if (!TeamNameValidator.validTag(prepared.tag(), tagMin, tagMax)) {
            return Denial.INVALID_TAG;
        }
        if (TeamNameValidator.blocked(prepared.name(), blocked) || TeamNameValidator.blocked(prepared.tag(), blocked)) {
            return Denial.BLOCKED_NAME;
        }
        return null;
    }

    public static Denial inviteDenial(boolean actorInTeam, boolean hasInvitePermission, boolean selfInvite,
                                      boolean targetAlreadyMemberOrInvited, int teamSize, int maxMembers) {
        if (!actorInTeam) {
            return Denial.NOT_IN_TEAM;
        }
        if (!hasInvitePermission) {
            return Denial.NO_INVITE_PERMISSION;
        }
        if (selfInvite) {
            return Denial.INVITE_SELF;
        }
        if (targetAlreadyMemberOrInvited) {
            return Denial.ALREADY_INVITED_OR_MEMBER;
        }
        if (teamSize >= Math.max(1, maxMembers)) {
            return Denial.TEAM_FULL;
        }
        return null;
    }

    public static Denial joinDenial(boolean alreadyInTeam, boolean teamFound, boolean hasInvite, boolean inviteExpired,
                                    int teamSize, int maxMembers) {
        return joinDenial(alreadyInTeam, teamFound, hasInvite, inviteExpired, false, teamSize, maxMembers);
    }

    /** An open team needs no invite. */
    public static Denial joinDenial(boolean alreadyInTeam, boolean teamFound, boolean hasInvite, boolean inviteExpired,
                                    boolean teamOpen, int teamSize, int maxMembers) {
        if (alreadyInTeam) {
            return Denial.ALREADY_IN_TEAM;
        }
        if (!teamFound) {
            return Denial.TEAM_NOT_FOUND;
        }
        if (!teamOpen) {
            if (!hasInvite) {
                return Denial.NO_INVITE;
            }
            if (inviteExpired) {
                return Denial.INVITE_EXPIRED;
            }
        }
        if (teamSize >= Math.max(1, maxMembers)) {
            return Denial.TEAM_FULL;
        }
        return null;
    }

    public static Denial leaveDenial(boolean inTeam, boolean isOwner) {
        if (!inTeam) {
            return Denial.NOT_IN_TEAM;
        }
        if (isOwner) {
            return Denial.OWNER_CANNOT_LEAVE;
        }
        return null;
    }

    /** Kicking, promoting or demoting: the actor must outrank the target. */
    public static Denial manageDenial(TeamRole actor, TeamRole target, boolean self) {
        if (target == null) {
            return Denial.NOT_A_MEMBER;
        }
        if (self) {
            return Denial.CANNOT_MANAGE_SELF;
        }
        if (actor == null || !actor.outranks(target)) {
            return Denial.RANK_TOO_LOW;
        }
        return null;
    }

    /** The rank a promotion leads to: a member becomes an officer. Owners change only by transfer. */
    public static TeamRole promoted(TeamRole role) {
        return role == TeamRole.MEMBER ? TeamRole.OFFICER : null;
    }

    public static TeamRole demoted(TeamRole role) {
        return role == TeamRole.OFFICER ? TeamRole.MEMBER : null;
    }
}
