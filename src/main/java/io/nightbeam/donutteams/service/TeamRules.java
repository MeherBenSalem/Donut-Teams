package io.nightbeam.donutteams.service;

import io.nightbeam.donutteams.util.TeamNameValidator;

/**
 * Pure create / invite / join / leave rules used by {@link TeamService}.
 * Kept free of Bukkit so unit tests can cover the lite edition constraints.
 */
public final class TeamRules {

    private TeamRules() {
    }

    public enum Denial {
        NO_CREATE_PERMISSION,
        ALREADY_IN_TEAM,
        INVALID_NAME,
        INVALID_TAG,
        NAME_TAKEN,
        NOT_IN_TEAM,
        NO_INVITE_PERMISSION,
        INVITE_SELF,
        ALREADY_INVITED_OR_MEMBER,
        TEAM_FULL,
        OWNER_CANNOT_LEAVE,
        TEAM_NOT_FOUND,
        NO_INVITE,
        INVITE_EXPIRED
    }

    public record PreparedName(String name, String tag) {
    }

    public static PreparedName prepareCreate(String rawName, String rawTag, int tagMax) {
        String name = TeamNameValidator.normalizeName(rawName);
        String tagSource = rawTag == null || rawTag.isBlank() ? name : rawTag;
        String tag = TeamNameValidator.normalizeTag(tagSource);
        int max = Math.max(1, tagMax);
        if (tag.length() > max) {
            tag = tag.substring(0, max);
        }
        return new PreparedName(name, tag);
    }

    public static Denial createDenial(
            boolean hasCreatePermission,
            boolean alreadyInTeam,
            PreparedName prepared,
            boolean nameTaken,
            int nameMin,
            int nameMax,
            int tagMin,
            int tagMax
    ) {
        if (!hasCreatePermission) {
            return Denial.NO_CREATE_PERMISSION;
        }
        if (alreadyInTeam) {
            return Denial.ALREADY_IN_TEAM;
        }
        if (prepared == null || !TeamNameValidator.validName(prepared.name(), nameMin, nameMax)) {
            return Denial.INVALID_NAME;
        }
        if (!TeamNameValidator.validTag(prepared.tag(), tagMin, tagMax)) {
            return Denial.INVALID_TAG;
        }
        if (nameTaken) {
            return Denial.NAME_TAKEN;
        }
        return null;
    }

    public static Denial inviteDenial(
            boolean actorInTeam,
            boolean hasInvitePermission,
            boolean selfInvite,
            boolean targetAlreadyMemberOrInvited,
            int teamSize,
            int maxMembers
    ) {
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

    public static Denial joinDenial(
            boolean alreadyInTeam,
            boolean teamFound,
            boolean hasInvite,
            boolean inviteExpired,
            int teamSize,
            int maxMembers
    ) {
        if (alreadyInTeam) {
            return Denial.ALREADY_IN_TEAM;
        }
        if (!teamFound) {
            return Denial.TEAM_NOT_FOUND;
        }
        if (!hasInvite) {
            return Denial.NO_INVITE;
        }
        if (inviteExpired) {
            return Denial.INVITE_EXPIRED;
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
}
