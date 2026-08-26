package io.nightbeam.donutteams.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class DonutTeams {

    private DonutTeams() {
    }

    public static DonutTeamsApi getApi() {
        RegisteredServiceProvider<DonutTeamsApi> registration =
                Bukkit.getServicesManager().getRegistration(DonutTeamsApi.class);
        if (registration == null) {
            throw new IllegalStateException("DonutTeams is not loaded");
        }
        return registration.getProvider();
    }
}
