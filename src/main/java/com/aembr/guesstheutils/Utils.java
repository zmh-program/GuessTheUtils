package com.aembr.guesstheutils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class Utils {
    public static List<Text> getScoreboardLines(MinecraftClient client) {
        HashMap<Text, Integer> scoredLines = new HashMap<>();
        ClientPlayerEntity player = client.player;
        if (player == null) return new ArrayList<>();

        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.FROM_ID.apply(1));

        for (ScoreHolder scoreHolder : scoreboard.getKnownScoreHolders()) {
            if (!scoreboard.getScoreHolderObjectives(scoreHolder).containsKey(objective)) continue;

            Team team = scoreboard.getScoreHolderTeam(scoreHolder.getNameForScoreboard());
            if (team == null) continue;

            Text textLine = Text.empty().append(team.getPrefix().copy()).append(team.getSuffix().copy());
            String strLine = team.getPrefix().getString() + team.getSuffix().getString();

            if (strLine.trim().isEmpty()) continue;

            int teamScore = Objects.requireNonNull(scoreboard.getScore(scoreHolder, objective)).getScore();
            scoredLines.put(textLine, teamScore);
        }
        // The objective name is usually animated, and the formatting doesn't convey any info, so we strip it
        if (objective != null) scoredLines.put(Text.of(Formatting.strip(objective.getDisplayName().getString())), Integer.MAX_VALUE);

        return scoredLines.entrySet().stream().sorted((e1, e2) ->
                Integer.compare(e2.getValue(), e1.getValue())).map(Map.Entry::getKey).toList();
    }

    public static List<Text> collectTabListEntries(MinecraftClient client) {
        if (client.player == null) return new ArrayList<>();

        return client.player.networkHandler.getListedPlayerListEntries().stream().map(entry -> {
            MutableText entryText;
            if (entry.getDisplayName() != null) {
                entryText = entry.getDisplayName().copy();
            } else {
                entryText = Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName()));
            }
            return (Text) entryText;
        }).toList();
    }

    public static void sendMessage(String message) {
        if (GuessTheUtils.CLIENT == null || GuessTheUtils.CLIENT.player == null) return;
        GuessTheUtils.CLIENT.player.sendMessage(GuessTheUtils.prefix.copy()
                .append(Text.literal(message).formatted(Formatting.GRAY)));
    }

    public static void sendMessage(Text message) {
        if (GuessTheUtils.CLIENT == null || GuessTheUtils.CLIENT.player == null) return;
        GuessTheUtils.CLIENT.player.sendMessage(GuessTheUtils.prefix.copy().append(message));
    }

    public static class FixedSizeBuffer<T> {
        private final int maxSize;
        private final List<T> buffer;

        public FixedSizeBuffer(int maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("Max size must be greater than 0");
            }
            this.maxSize = maxSize;
            this.buffer = new ArrayList<>(maxSize);
        }

        public void add(T element) {
            buffer.add(0, element);
            if (buffer.size() > maxSize) {
                buffer.remove(buffer.size() - 1);
            }
        }

        public T get(int index) {
            if (index < 0 || index >= buffer.size()) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + buffer.size());
            }
            return buffer.get(index);
        }

        public int size() {
            return buffer.size();
        }
    }

    public record Pair<T1, T2>(T1 a, T2 b) {
        @Override
        public @NotNull String toString() {
            return "Pair{" + this.a() + ", " + this.b() + "}";
        }
    }

    public static <T> List<List<T>> generatePermutations(List<T> list) {
        List<List<T>> result = new ArrayList<>();
        generatePermutationsHelper(list, 0, result);
        return result;
    }

    private static <T> void generatePermutationsHelper(List<T> list, int start, List<List<T>> result) {
        if (start >= list.size()) {
            result.add(new ArrayList<>(list));
            return;
        }

        for (int i = start; i < list.size(); i++) {
            swap(list, start, i);
            generatePermutationsHelper(list, start + 1, result);
            swap(list, start, i); // backtrack
        }
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    public static String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
