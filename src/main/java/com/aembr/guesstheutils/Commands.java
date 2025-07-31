package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.aembr.guesstheutils.modules.BuilderNotification;
import com.aembr.guesstheutils.modules.GameTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class Commands extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "gtu";
    }
    
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtu [reload|status|debug|toggle|replay|livetest|test]";
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = (EntityPlayer) sender;
        
        GuessTheUtils.LOGGER.info("Processing command with " + args.length + " arguments");
        
        if (args.length == 0) {
            sendMessage(player, "GuessTheUtils v" + GuessTheUtils.VERSION);
            sendMessage(player, "Use /gtu status to see module status");
            sendMessage(player, "Available commands: reload, status, debug, toggle, replay, livetest, test");
            return;
        }
        
        GuessTheUtils.LOGGER.info("Executing command: " + args[0]);
        
        switch (args[0].toLowerCase()) {
            case "reload":
                GuessTheUtilsConfig.syncConfig();
                sendMessage(player, "Configuration reloaded!");
                GuessTheUtils.LOGGER.info("Config reloaded by " + player.getName());
                break;
                
            case "status":
                sendMessage(player, "Game Tracker: " + getStatus(GuessTheUtilsConfig.enableGameTracker));
                sendMessage(player, "Custom Scoreboard: " + getStatus(GuessTheUtilsConfig.enableCustomScoreboard));
                sendMessage(player, "Chat Cooldown: " + getStatus(GuessTheUtilsConfig.enableChatCooldownTimer));
                sendMessage(player, "Shortcut Reminder: " + getStatus(GuessTheUtilsConfig.enableShortcutReminder));
                sendMessage(player, "Builder Notification: " + getStatus(GuessTheUtilsConfig.enableBuilderNotification));
                break;
                
            case "debug":
                sendMessage(player, "=== Game Detection ===");
                boolean inGtb = GuessTheUtils.events.isInGtb();
                sendMessage(player, "In Guess The Build: " + (inGtb ? 
                    EnumChatFormatting.GREEN + "Yes" : 
                    EnumChatFormatting.RED + "No"));
            
                sendMessage(player, "");
                sendMessage(player, "=== Vanilla Scoreboard Info ===");
                showVanillaScoreboardInfo(player);
                break;
                
            case "toggle":
                if (args.length < 2) {
                    sendMessage(player, "Usage: /gtu toggle <module>");
                    return;
                }
                toggleModule(player, args[1]);
                break;
                
            case "replay":
                GuessTheUtils.replay.save();
                sendMessage(player, "Replay saved!");
                break;
                
            case "livetest":
                GuessTheUtils.testing = !GuessTheUtils.testing;
                if (GuessTheUtils.testing) GuessTheUtils.liveE2ERunner.currentTick = 0;
                sendMessage(player, "Live testing: " + (GuessTheUtils.testing ? "Enabled" : "Disabled"));
                break;
                
            case "test":
                if (args.length < 2) {
                    sendMessage(player, "Usage: /gtu test <module>");
                    sendMessage(player, "Available tests: cooldown, shortcut, notification, scoreboard");
                    return;
                }
                runTest(player, args[1]);
                break;
                
            default:
                sendMessage(player, "Unknown command. Use /gtu for help.");
                break;
        }
    }
    
    private void toggleModule(EntityPlayer player, String module) {
        switch (module.toLowerCase()) {
            case "gametracker":
                GuessTheUtilsConfig.enableGameTracker = !GuessTheUtilsConfig.enableGameTracker;
                sendMessage(player, "Game Tracker: " + getStatus(GuessTheUtilsConfig.enableGameTracker));
                break;
            case "scoreboard":
                GuessTheUtilsConfig.enableCustomScoreboard = !GuessTheUtilsConfig.enableCustomScoreboard;
                sendMessage(player, "Custom Scoreboard: " + getStatus(GuessTheUtilsConfig.enableCustomScoreboard));
                break;
            case "chatcooldown":
                GuessTheUtilsConfig.enableChatCooldownTimer = !GuessTheUtilsConfig.enableChatCooldownTimer;
                sendMessage(player, "Chat Cooldown: " + getStatus(GuessTheUtilsConfig.enableChatCooldownTimer));
                break;
            case "shortcutreminder":
                GuessTheUtilsConfig.enableShortcutReminder = !GuessTheUtilsConfig.enableShortcutReminder;
                sendMessage(player, "Shortcut Reminder: " + getStatus(GuessTheUtilsConfig.enableShortcutReminder));
                break;
            case "buildernotification":
                GuessTheUtilsConfig.enableBuilderNotification = !GuessTheUtilsConfig.enableBuilderNotification;
                sendMessage(player, "Builder Notification: " + getStatus(GuessTheUtilsConfig.enableBuilderNotification));
                break;
            default:
                sendMessage(player, "Unknown module: " + module);
                sendMessage(player, "Available modules: gametracker, scoreboard, chatcooldown, shortcutreminder, buildernotification");
                break;
        }
    }
    
    private void runTest(EntityPlayer player, String testType) {
        switch (testType.toLowerCase()) {
            case "cooldown":
                testCooldown(player);
                break;
            case "shortcut":
                testShortcut(player);
                break;
            case "notification":
                testNotification(player);
                break;
            case "scoreboard":
                testScoreboard(player);
                break;
            default:
                sendMessage(player, "Unknown test: " + testType);
                sendMessage(player, "Available tests: cooldown, shortcut, notification, scoreboard");
                break;
        }
    }
    
    private void testCooldown(EntityPlayer player) {
        if (!GuessTheUtilsConfig.enableChatCooldownTimer) {
            sendMessage(player, "Chat Cooldown Timer is disabled. Enable it first with: /gtu toggle chatcooldown");
            return;
        }
        
        sendMessage(player, "Testing Chat Cooldown Timer...");
        
        GuessTheUtils.chatCooldown.enable();
        GuessTheUtils.chatCooldown.onMessageSent();
        sendMessage(player, "Cooldown timer started - check the screen for timer display");
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                GuessTheUtils.chatCooldown.disable();
                player.addChatMessage(new ChatComponentText("[GTU] Cooldown timer disabled"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void testShortcut(EntityPlayer player) {
        if (!GuessTheUtilsConfig.enableShortcutReminder) {
            sendMessage(player, "Shortcut Reminder is disabled. Enable it first with: /gtu toggle shortcutreminder");
            return;
        }
        
        sendMessage(player, "Testing Shortcut Reminder...");
        sendMessage(player, "Simulating theme update with 'trash can'");
        
        GTBEvents.ThemeUpdateEvent testEvent = new GTBEvents.ThemeUpdateEvent("trash can");
        GuessTheUtils.shortcutReminder.onThemeUpdate(testEvent);
        sendMessage(player, "Theme update sent - check for shortcut reminders in chat");
    }
    
    private void testNotification(EntityPlayer player) {
        if (!GuessTheUtilsConfig.enableBuilderNotification) {
            sendMessage(player, "Builder Notification is disabled. Enable it first with: /gtu toggle buildernotification");
            return;
        }
        
        sendMessage(player, "Testing Builder Notification...");
        sendMessage(player, "Sending test notification - you should receive a system notification");
        
        BuilderNotification.NotificationUtil.sendNotification("Guess the Build Test", "This is a test notification from GuessTheUtils!");
        sendMessage(player, "Test notification sent - check for system notification");
    }
    
    private void testScoreboard(EntityPlayer player) {
        if (!GuessTheUtilsConfig.enableCustomScoreboard) {
            sendMessage(player, "Custom Scoreboard is disabled. Enable it first with: /gtu toggle scoreboard");
            return;
        }

        if (!GuessTheUtils.events.isInGtb()) {
            sendMessage(player, "You are not in Guess The Build, so the scoreboard will not be visible.");
            return;
        }
        
        sendMessage(player, "Testing Custom Scoreboard...");
        sendMessage(player, "Creating fake game state for testing");
        
        // Create a fake game for testing
        createTestGame(player);
        
        sendMessage(player, "Test game created - check right side of screen for custom scoreboard");
        sendMessage(player, "Use '/gtu test scoreboard' again to reset the test");
    }
    
    private void createTestGame(EntityPlayer player) {
        // Create test game data
        if (GuessTheUtils.gameTracker.game != null) {
            // Clear existing game
            GuessTheUtils.gameTracker.game = null;
            sendMessage(player, "Cleared test game");
            return;
        }
        
        // Create new test game
        GuessTheUtils.gameTracker.game = new GameTracker.Game();
        GuessTheUtils.gameTracker.game.currentRound = 2;
        GuessTheUtils.gameTracker.game.totalRounds = 3;
        GuessTheUtils.gameTracker.game.currentTheme = "house";
        GuessTheUtils.gameTracker.game.currentTimer = "1:30";
        
        // Add test players
        GuessTheUtils.gameTracker.game.players.clear();
        
        // Add user player
        GameTracker.Player userPlayer = new GameTracker.Player(player.getName());
        userPlayer.isUser = true;
        userPlayer.points = new int[]{20, 3, 0};
        userPlayer.rank = EnumChatFormatting.YELLOW;
        GuessTheUtils.gameTracker.game.players.add(userPlayer);
        
        // Add builder player
        GameTracker.Player builderPlayer = new GameTracker.Player("TestBuilder");
        builderPlayer.points = new int[]{12, 2, 0};
        builderPlayer.rank = EnumChatFormatting.GREEN;
        GuessTheUtils.gameTracker.game.currentBuilder = builderPlayer;
        GuessTheUtils.gameTracker.game.players.add(builderPlayer);
        
        // Add other players
        GameTracker.Player player1 = new GameTracker.Player("Player1");
        player1.points = new int[]{6, 1, 0};
        player1.rank = EnumChatFormatting.BLUE;
        GuessTheUtils.gameTracker.game.players.add(player1);
        
        GameTracker.Player player2 = new GameTracker.Player("Player2");
        player2.points = new int[]{4, 1, 0};
        player2.rank = EnumChatFormatting.RED;
        player2.leaverState = GameTracker.Player.LeaverState.POTENTIAL_LEAVER;
        GuessTheUtils.gameTracker.game.players.add(player2);
        
        GameTracker.Player leaver = new GameTracker.Player("Leaver");
        leaver.points = new int[]{3, 0, 0};
        leaver.leaverState = GameTracker.Player.LeaverState.LEAVER;
        GuessTheUtils.gameTracker.game.players.add(leaver);
        
        // Set game state
        GameTracker.state = GTBEvents.GameState.ROUND_BUILD;
        
        sendMessage(player, "Created test game with 5 players, round 2/3");
    }
    
    private String getStatus(boolean enabled) {
        return enabled ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
    }
    

    
    private String getScoreboardLineText(Scoreboard scoreboard, String playerName, int scorePoints) {
        try {
            net.minecraft.scoreboard.Team team = scoreboard.getPlayersTeam(playerName);
            
            if (team != null) {
                TeamTextInfo textInfo = getTeamTextInfo(team);
                
                // Construct the full display text
                StringBuilder displayText = new StringBuilder();
                if (textInfo.prefix != null && !textInfo.prefix.isEmpty()) {
                    displayText.append(textInfo.prefix);
                }
                if (playerName != null && !playerName.isEmpty()) {
                    displayText.append(playerName);
                }
                if (textInfo.suffix != null && !textInfo.suffix.isEmpty()) {
                    displayText.append(textInfo.suffix);
                }
                
                String finalText = displayText.toString();
                if (!finalText.trim().isEmpty()) {
                    return finalText.trim() + ": " + scorePoints;
                }
            }
            
            // Simple fallback
            String displayName = playerName;
            if (playerName == null || playerName.isEmpty()) {
                displayName = "[EMPTY]";
            }
            
            return displayName + ": " + scorePoints;
            
        } catch (Exception e) {
            return (playerName == null ? "[NULL]" : playerName) + ": " + scorePoints;
        }
    }
    
    private static class TeamTextInfo {
        public String prefix = "";
        public String suffix = "";
        
        public TeamTextInfo(String prefix, String suffix) {
            this.prefix = prefix != null ? prefix : "";
            this.suffix = suffix != null ? suffix : "";
        }
    }
    
    private TeamTextInfo getTeamTextInfo(net.minecraft.scoreboard.Team team) {
        String prefix = "";
        String suffix = "";
        
        try {
            // Get prefix and suffix using known 1.8.9 method names
            java.lang.reflect.Method[] methods = team.getClass().getMethods();
            
            for (java.lang.reflect.Method method : methods) {
                if (method.getParameterCount() == 0 && method.getReturnType() == String.class) {
                    String name = method.getName();
                    try {
                        Object result = method.invoke(team);
                        if (result instanceof String) {
                            String resultStr = (String) result;
                            
                            // func_96668_e is the prefix in Hypixel (1.8.9)
                            if (name.equals("func_96668_e") && !resultStr.isEmpty()) {
                                prefix = resultStr;
                            }
                            // func_96663_f is the suffix in Hypixel (1.8.9)  
                            else if (name.equals("func_96663_f") && !resultStr.isEmpty()) {
                                suffix = resultStr;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        
        return new TeamTextInfo(prefix, suffix);
    }
    


    private void showVanillaScoreboardInfo(EntityPlayer player) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            
            if (scoreboard == null) {
                sendMessage(player, "No scoreboard available");
                return;
            }
            
            sendMessage(player, "Current displayed scoreboard:");
            
            // Sidebar scoreboard (slot 1)
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebarObjective != null) {
                sendMessage(player, "Sidebar: " + sidebarObjective.getDisplayName() + " (" + sidebarObjective.getName() + ")");
                
                // Get scores for sidebar
                java.util.Collection scores = scoreboard.getSortedScores(sidebarObjective);
                if (!scores.isEmpty()) {
                    sendMessage(player, "  Scores (sorted by score, descending):");
                    
                    // Convert to list and sort by score (descending)
                    java.util.List<Score> scoreList = new java.util.ArrayList<Score>();
                    for (Object scoreObj : scores) {
                        scoreList.add((Score) scoreObj);
                    }
                    
                    // Sort by score points in descending order (high scores first)
                    java.util.Collections.sort(scoreList, new java.util.Comparator<Score>() {
                        @Override
                        public int compare(Score s1, Score s2) {
                            return Integer.compare(s2.getScorePoints(), s1.getScorePoints());
                        }
                    });
                    
                    int count = 0;
                    for (Score score : scoreList) {
                        if (count >= 15) break; // Sidebar shows max 15 entries
                        String playerName = score.getPlayerName();
                        String actualText = getScoreboardLineText(scoreboard, playerName, score.getScorePoints());
                        
                        sendMessage(player, "    " + actualText);
                        count++;
                    }
                }
            } else {
                sendMessage(player, "Sidebar: None");
            }
            
            // Player list scoreboard (slot 0)
            ScoreObjective listObjective = scoreboard.getObjectiveInDisplaySlot(0);
            if (listObjective != null) {
                sendMessage(player, "Player List: " + listObjective.getDisplayName() + " (" + listObjective.getName() + ")");
                if (scoreboard.entityHasObjective(player.getName(), listObjective)) {
                    int score = scoreboard.getValueFromObjective(player.getName(), listObjective).getScorePoints();
                    sendMessage(player, "  Your score: " + score);
                }
            } else {
                sendMessage(player, "Player List: None");
            }
            
            // Below name scoreboard (slot 2)
            ScoreObjective belowNameObjective = scoreboard.getObjectiveInDisplaySlot(2);
            if (belowNameObjective != null) {
                sendMessage(player, "Below Name: " + belowNameObjective.getDisplayName() + " (" + belowNameObjective.getName() + ")");
                if (scoreboard.entityHasObjective(player.getName(), belowNameObjective)) {
                    int score = scoreboard.getValueFromObjective(player.getName(), belowNameObjective).getScorePoints();
                    sendMessage(player, "  Your score: " + score);
                }
            } else {
                sendMessage(player, "Below Name: None");
            }
            
        } catch (Exception e) {
            sendMessage(player, "Error retrieving scoreboard info: " + e.getMessage());
            GuessTheUtils.LOGGER.error("Error in showVanillaScoreboardInfo", e);
        }
    }
    
    private void sendMessage(EntityPlayer player, String message) {
        ChatComponentText prefix = new ChatComponentText("[GTU] ");
        prefix.getChatStyle().setColor(EnumChatFormatting.GOLD);
        
        ChatComponentText msg = new ChatComponentText(message);
        msg.getChatStyle().setColor(EnumChatFormatting.WHITE);
        
        prefix.appendSibling(msg);
        player.addChatMessage(prefix);
    }
} 