package com.aembr.guesstheutils;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import org.jetbrains.annotations.NotNull;

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
            scoreboardLines = deserializeList(tickUpdate.get("scoreboardLines"));
        }
        if (tickUpdate.has("playerListEntries")) {
            playerListEntries = deserializeList(tickUpdate.get("playerListEntries"));
        }
        if (tickUpdate.has("chatMessages")) {
            chatMessages = deserializeList(tickUpdate.get("chatMessages"));
        }
        if (tickUpdate.has("actionBarMessage")) {
            actionBarMessage = deserializeText(tickUpdate.get("actionBarMessage").getAsString());
        }
        if (tickUpdate.has("title")) {
            title = deserializeText(tickUpdate.get("title").getAsString());
        }
        if (tickUpdate.has("subtitle")) {
            subtitle = deserializeText(tickUpdate.get("subtitle").getAsString());
        }
        if (tickUpdate.has("screenTitle")) {
            screenTitle = deserializeText(tickUpdate.get("screenTitle").getAsString());
        }
    }

    private List<Text> deserializeList(JsonElement jsonElement) {
        List<Text> textList = new ArrayList<>();
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            String jsonString = element.getAsString();
            textList.add(deserializeText(jsonString));
        }
        return textList;
    }

    private Text deserializeText(String jsonString) {
        return TextCodecs.CODEC
                .decode(JsonOps.INSTANCE, new Gson().fromJson(jsonString, JsonElement.class))
                .getOrThrow()
                .getFirst();
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
        public @NotNull String toString() {
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
