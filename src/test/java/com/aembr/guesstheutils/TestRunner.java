package com.aembr.guesstheutils;

import com.google.gson.JsonObject;

import java.io.File;
import java.util.Iterator;

public class TestRunner {
    Iterator<JsonObject> tickObjects;

    public TestRunner(File test) {
        tickObjects = Replay.load(test).iterator();
    }

    public void play(GTBEvents events) {
        while (tickObjects.hasNext()) {
            try {
                events.processTickUpdate(new Tick(tickObjects.next()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
