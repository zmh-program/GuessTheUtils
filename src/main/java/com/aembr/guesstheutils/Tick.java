package com.aembr.guesstheutils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.List;

public class Tick {
    public List<Text> scoreboardLines;
    public List<Text> playerListEntries;
    public List<Text> chatMessages;
    public Text actionBarMessage;
    public Text title;
    public Text subtitle;
    public Text screenTitle;
    public String error;

    public Tick() {}

    public Tick(String jsonString) {
        JsonObject tickUpdate = JsonParser.parseString(jsonString).getAsJsonObject();
        if (tickUpdate.has("scoreboardLines")) {
            scoreboardLines = deserializeList(new Gson().fromJson(tickUpdate.get("scoreboardLines"),
                    new TypeToken<List<String>>() {}.getType()));
        }
        if (tickUpdate.has("playerListEntries")) {
            playerListEntries = deserializeList(new Gson().fromJson(tickUpdate.get("playerListEntries"),
                    new TypeToken<List<String>>() {}.getType()));
        }
        if (tickUpdate.has("chatMessages")) {
            chatMessages = deserializeList(new Gson().fromJson(tickUpdate.get("chatMessages"),
                    new TypeToken<List<String>>() {}.getType()));
        }
        if (tickUpdate.has("actionBarMessage")) {
            actionBarMessage = Text.Serialization.fromJson(new Gson().fromJson(tickUpdate.get("actionBarMessage"),
                    new TypeToken<String>() {}.getType()));
        }
        if (tickUpdate.has("title")) {
            title = Text.Serialization.fromJson(new Gson().fromJson(tickUpdate.get("title"),
                    new TypeToken<String>() {}.getType()));
        }
        if (tickUpdate.has("subtitle")) {
            subtitle = Text.Serialization.fromJson(new Gson().fromJson(tickUpdate.get("subtitle"),
                    new TypeToken<String>() {}.getType()));
        }
        if (tickUpdate.has("screenTitle")) {
            screenTitle = Text.Serialization.fromJson(new Gson().fromJson(tickUpdate.get("screenTitle"),
                    new TypeToken<String>() {}.getType()));
        }
    }

    public static List<Text> deserializeList(List<String> input) {
        return input.stream().map(str -> (Text) Text.Serialization.fromJson(str)).toList();
    }

    public static List<String> serializeList(List<Text> input) {
        return input.stream().map(Text.Serialization::toJsonString).toList();
    }

    public SerializedTick serialize() {
        return new SerializedTick(scoreboardLines == null ? null : serializeList(scoreboardLines),
                playerListEntries == null ? null : serializeList(playerListEntries),
                chatMessages == null ? null : serializeList(chatMessages),
                actionBarMessage == null ? null : Text.Serialization.toJsonString(actionBarMessage),
                title == null ? null : Text.Serialization.toJsonString(title),
                subtitle == null ? null : Text.Serialization.toJsonString(subtitle),
                screenTitle == null ? null : Text.Serialization.toJsonString(screenTitle),
                error);
    }

    public boolean isEmpty() {
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                Object value = field.get(this);
                if (value != null) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                GuessTheUtils.LOGGER.error(e.toString());
            }
        }
        return true;
    }

    public record SerializedTick(List<String> scoreboardLines, List<String> playerListEntries,
                                 List<String> chatMessages, String actionBarMessage, String title, String subtitle,
                                 String screenTitle, String error) {

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
