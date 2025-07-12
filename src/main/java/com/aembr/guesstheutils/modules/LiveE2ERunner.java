package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.Replay;
import com.aembr.guesstheutils.Tick;
import com.google.gson.JsonObject;

import java.util.List;

public class LiveE2ERunner {
    List<JsonObject> tickObjects = Replay.load(GuessTheUtils.class.getResourceAsStream("/live_tests/1.json"));
    public int currentTick = 0;

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
