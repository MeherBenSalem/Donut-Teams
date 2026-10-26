package io.nightbeam.donutteams.storage;

/** Table names with the configured prefix. */
public final class Tables {

    final String prefix;
    final String meta;
    final String teams;
    final String members;
    final String invites;
    final String homes;
    final String relations;
    final String chests;
    final String bankLog;

    public Tables(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
        this.meta = this.prefix + "meta";
        this.teams = this.prefix + "teams";
        this.members = this.prefix + "members";
        this.invites = this.prefix + "invites";
        this.homes = this.prefix + "homes";
        this.relations = this.prefix + "relations";
        this.chests = this.prefix + "chests";
        this.bankLog = this.prefix + "bank_log";
    }
}
