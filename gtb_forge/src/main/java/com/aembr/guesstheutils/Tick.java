package com.aembr.guesstheutils;

import java.util.List;

public class Tick {
    public String title;
    public String subtitle;
    public String actionBar;
    public List<String> chatMessages;
    public List<String> scoreboardLines;
    public List<String> playerListEntries;
    public String error;
    
    public boolean isEmpty() {
        return title == null && 
               subtitle == null && 
               actionBar == null && 
               (chatMessages == null || chatMessages.isEmpty()) &&
               (scoreboardLines == null || scoreboardLines.isEmpty()) &&
               (playerListEntries == null || playerListEntries.isEmpty()) &&
               error == null;
    }
} 