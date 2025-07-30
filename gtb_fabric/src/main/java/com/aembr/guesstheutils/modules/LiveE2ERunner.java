package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.Tick;
import com.google.gson.JsonObject;

import java.util.List;

public class LiveE2ERunner {
    List<JsonObject> tickObjects;
    public int currentTick = 0;

    public LiveE2ERunner(List<JsonObject> replay) {
        this.tickObjects = replay;
    }

    public Tick getNext() {
        Tick tick = new Tick(tickObjects.get(currentTick));
        if (currentTick == tickObjects.size() - 1) {
            currentTick = 0;
        } else {
            currentTick++;
        }
        return tick;
    }
}
