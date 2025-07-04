package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;

import java.util.Collection;
import java.util.HashSet;

public class NameAutocomplete extends GTBEvents.Module {
    Collection<String> names = new HashSet<>();

    public NameAutocomplete(GTBEvents events) {
        super(events);
        events.subscribe(GTBEvents.GameStartEvent.class, this::collectNames, this);
    }

    private void collectNames(GTBEvents.GameStartEvent event) {
        names.clear();
        event.players().forEach(p -> names.add(p.name()));
    }

    public Collection<String> getNames() {
        if (events.isInGtb()) return names;
        return new HashSet<>();
    }
}
