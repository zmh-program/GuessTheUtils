package com.aembr.guesstheutils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Tick {
    public List<String> scoreboardLines;
    public List<String> playerListEntries;
    public List<String> chatMessages;
    public String actionBarMessage;
    public String title;
    public String subtitle;
    public String screenTitle;
    public String error;

    public Tick() {
        com.aembr.guesstheutils.interceptor.ChatInterceptor.extractPendingMessages(this);
    }

    public Tick(JsonObject json) {
        if (json.has("scoreboardLines")) {
            scoreboardLines = new ArrayList<>();
            json.get("scoreboardLines").getAsJsonArray().forEach(element -> 
                scoreboardLines.add(element.getAsString()));
        }
        if (json.has("playerListEntries")) {
            playerListEntries = new ArrayList<>();
            json.get("playerListEntries").getAsJsonArray().forEach(element -> 
                playerListEntries.add(element.getAsString()));
        }
        if (json.has("chatMessages")) {
            chatMessages = new ArrayList<>();
            json.get("chatMessages").getAsJsonArray().forEach(element -> 
                chatMessages.add(element.getAsString()));
        }
        if (json.has("actionBarMessage")) {
            actionBarMessage = json.get("actionBarMessage").getAsString();
        }
        if (json.has("title")) {
            title = json.get("title").getAsString();
        }
        if (json.has("subtitle")) {
            subtitle = json.get("subtitle").getAsString();
        }
        if (json.has("screenTitle")) {
            screenTitle = json.get("screenTitle").getAsString();
        }
        if (json.has("error")) {
            error = json.get("error").getAsString();
        }
    }

    public SerializedTick serialize() {
        return new SerializedTick(scoreboardLines, playerListEntries, chatMessages, 
                actionBarMessage, title, subtitle, screenTitle, error);
    }

    public boolean isEmpty() {
        return title == null && 
               subtitle == null && 
               actionBarMessage == null && 
               screenTitle == null &&
               (chatMessages == null || chatMessages.isEmpty()) &&
               (scoreboardLines == null || scoreboardLines.isEmpty()) &&
               (playerListEntries == null || playerListEntries.isEmpty()) &&
               error == null;
    }

    public static class SerializedTick {
        public final List<String> scoreboardLines;
        public final List<String> playerListEntries;
        public final List<String> chatMessages;
        public final String actionBarMessage;
        public final String title;
        public final String subtitle;
        public final String screenTitle;
        public final String error;

        public SerializedTick(List<String> scoreboardLines, List<String> playerListEntries,
                             List<String> chatMessages, String actionBarMessage, String title, 
                             String subtitle, String screenTitle, String error) {
            this.scoreboardLines = scoreboardLines;
            this.playerListEntries = playerListEntries;
            this.chatMessages = chatMessages;
            this.actionBarMessage = actionBarMessage;
            this.title = title;
            this.subtitle = subtitle;
            this.screenTitle = screenTitle;
            this.error = error;
        }

        @Override
        public String toString() {
            return "SerializedTick{" +
                    "scoreboardLines=" + scoreboardLines +
                    ", playerListEntries=" + playerListEntries +
                    ", chatMessages=" + chatMessages +
                    ", actionBarMessage='" + actionBarMessage + '\'' +
                    ", title='" + title + '\'' +
                    ", subtitle='" + subtitle + '\'' +
                    ", screenTitle='" + screenTitle + '\'' +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
} 