package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;

import java.util.Collection;
import java.util.HashSet;

public class NameAutocomplete extends GTBEvents.Module {
    Collection<String> names = new HashSet<String>();

    public NameAutocomplete(GTBEvents events) {
        super(events);
        events.subscribe(GTBEvents.GameStartEvent.class, this::collectNames, this);
    }

    private void collectNames(GTBEvents.GameStartEvent event) {
        names.clear();
        for (GTBEvents.InitialPlayerData player : event.players()) {
            names.add(player.name());
        }
    }

    public Collection<String> getNames() {
        if (events.isInGtb() && GuessTheUtilsConfig.enableNameAutocomplete) return names;
        return new HashSet<String>();
    }
}