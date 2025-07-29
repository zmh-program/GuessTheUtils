package com.aembr.guesstheutils;

import java.util.ArrayList;
import java.util.List;

public class Replay {
    private List<Tick> ticks = new ArrayList<Tick>();
    
    public void initialize() {
        ticks.clear();
    }
    
    public void addTick(Tick tick) {
        ticks.add(tick);
    }
    
    public void save() {
        // Save replay to file - placeholder for now
        GuessTheUtils.LOGGER.info("Replay saved with " + ticks.size() + " ticks");
    }
} 