package com.aembr.guesstheutils;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
            actionBarMessage = TextCodecs.CODEC.decode(JsonOps.INSTANCE, new Gson().fromJson(tickUpdate.get("actionBarMessage"),
                    JsonObject.class)).getOrThrow().getFirst();
        }
        if (tickUpdate.has("title")) {
            title = TextCodecs.CODEC.decode(JsonOps.INSTANCE, new Gson().fromJson(tickUpdate.get("title"),
                    JsonObject.class)).getOrThrow().getFirst();
        }
        if (tickUpdate.has("subtitle")) {
            subtitle = TextCodecs.CODEC.decode(JsonOps.INSTANCE, new Gson().fromJson(tickUpdate.get("subtitle"),
                    JsonObject.class)).getOrThrow().getFirst();
        }
        if (tickUpdate.has("screenTitle")) {
            screenTitle = TextCodecs.CODEC.decode(JsonOps.INSTANCE, new Gson().fromJson(tickUpdate.get("screenTitle"),
                    JsonObject.class)).getOrThrow().getFirst();
        }
    }

    public static List<Text> deserializeList(List<String> input) {
        Gson gson = new Gson();
        return input.stream().map(str -> TextCodecs.CODEC
                .decode(JsonOps.INSTANCE, gson.fromJson(str, JsonObject.class)).getOrThrow().getFirst())
                .toList();
    }

    public static List<String> serializeList(List<Text> input) {
        Gson gson = new Gson();
        return input.stream().map(text ->
                gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow())).toList();
    }

    public SerializedTick serialize() {
        Gson gson = new Gson();
        return new SerializedTick(scoreboardLines == null ? null : serializeList(scoreboardLines),
                playerListEntries == null ? null : serializeList(playerListEntries),
                chatMessages == null ? null : serializeList(chatMessages),
                actionBarMessage == null ? null : gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, actionBarMessage).getOrThrow()),
                title == null ? null : gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, title).getOrThrow()),
                subtitle == null ? null : gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, subtitle).getOrThrow()),
                screenTitle == null ? null : gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, screenTitle).getOrThrow()),
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
