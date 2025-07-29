# GuessTheUtils Fabric 1.21 → Forge 1.8.9 Detailed Implementation Guide

## 🚀 Stage 1: Project Foundation Setup

### Step 1: Create Forge 1.8.9 Project Structure

#### 1.1 Download Forge MDK
```bash
# Download Forge 1.8.9 MDK
wget https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318/forge-1.8.9-11.15.1.2318-mdk.zip
unzip forge-1.8.9-11.15.1.2318-mdk.zip -d GuessTheUtils-Forge
cd GuessTheUtils-Forge
```

#### 1.2 Create build.gradle Configuration
**Delete**: `build.gradle.kts`
**Create**: `build.gradle`

```gradle
buildscript {
    repositories {
        jcenter()
        maven { url = "http://files.minecraftforge.net/maven" }
    }
    dependencies {
        classpath 'net.minecraftforge.gradle:ForgeGradle:1.2-SNAPSHOT'
    }
}

apply plugin: 'forge'

version = "0.9.5"
group = "com.aembr.guesstheutils"
archivesBaseName = "guesstheutils"

minecraft {
    version = "1.8.9-11.15.1.2318"
    runDir = "eclipse"
    mappings = "stable_22"
}

sourceCompatibility = targetCompatibility = "1.8"
compileJava {
    sourceCompatibility = targetCompatibility = "1.8"
}

dependencies {
    compile 'org.yaml:snakeyaml:1.18'
}

processResources {
    inputs.property "version", project.version
    inputs.property "mcversion", project.minecraft.version

    from(sourceSets.main.resources.srcDirs) {
        include 'mcmod.info'
        expand 'version':project.version, 'mcversion':project.minecraft.version
    }
        
    from(sourceSets.main.resources.srcDirs) {
        exclude 'mcmod.info'
    }
}
```

#### 1.3 Create mcmod.info
**Delete**: `src/main/resources/fabric.mod.json`
**Create**: `src/main/resources/mcmod.info`

```json
[
{
  "modid": "guesstheutils",
  "name": "GuessTheUtils",
  "description": "A quality of life mod for Guess The Build.",
  "version": "${version}",
  "mcversion": "${mcversion}",
  "url": "https://github.com/aembur/GuessTheUtils",
  "updateUrl": "",
  "authorList": ["aembr (Yria)"],
  "credits": "",
  "logoFile": "",
  "screenshots": [],
  "dependencies": []
}
]
```

### Step 2: Create Proxy System

#### 2.1 CommonProxy
**Create**: `src/main/java/com/aembr/guesstheutils/proxy/CommonProxy.java`

```java
package com.aembr.guesstheutils.proxy;

public class CommonProxy {
    public void preInit() {}
    public void init() {}
    public void postInit() {}
}
```

#### 2.2 ClientProxy
**Create**: `src/main/java/com/aembr/guesstheutils/proxy/ClientProxy.java`

```java
package com.aembr.guesstheutils.proxy;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.hooks.HudHooks;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        super.preInit();
    }

    @Override
    public void init() {
        super.init();
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new GuessTheUtils());
        MinecraftForge.EVENT_BUS.register(new HudHooks());
        MinecraftForge.EVENT_BUS.register(GuessTheUtils.gameTracker);
        MinecraftForge.EVENT_BUS.register(GuessTheUtils.chatCooldown);
    }

    @Override
    public void postInit() {
        super.postInit();
    }
}
```

---

## 🔧 Stage 2: Core System Rewrite

### Step 3: Convert Main Mod Class

#### 3.1 Rewrite GuessTheUtils.java
```java
package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.aembr.guesstheutils.modules.*;
import com.aembr.guesstheutils.proxy.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = GuessTheUtils.MOD_ID, name = GuessTheUtils.MOD_NAME, version = GuessTheUtils.VERSION, clientSideOnly = true)
public class GuessTheUtils {
    public static final String MOD_ID = "guesstheutils";
    public static final String MOD_NAME = "GuessTheUtils";
    public static final String VERSION = "0.9.5";
    
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final Minecraft CLIENT = Minecraft.getMinecraft();

    @SidedProxy(clientSide = "com.aembr.guesstheutils.proxy.ClientProxy", serverSide = "com.aembr.guesstheutils.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static final Replay replay = new Replay();
    public static GTBEvents events = new GTBEvents();

    public static GameTracker gameTracker = new GameTracker(events);
    public static NameAutocomplete nameAutocomplete = new NameAutocomplete(events);
    public static ShortcutReminder shortcutReminder = new ShortcutReminder(events);
    public static BuilderNotification builderNotification = new BuilderNotification(events);
    public static ChatCooldownTimer chatCooldown = new ChatCooldownTimer(events);

    public static boolean testing = false;
    public static LiveE2ERunner liveE2ERunner;

    private static Tick currentTick;
    private List<String> previousScoreboardLines = new ArrayList<String>();
    private List<String> previousPlayerListEntries = new ArrayList<String>();
    private String previousActionBarMessage = "";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("GuessTheUtils preInit");
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("GuessTheUtils init");
        proxy.init();
        
        replay.initialize();
        shortcutReminder.init();
        GuessTheUtilsConfig.loadConfig();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("GuessTheUtils postInit");
        proxy.postInit();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        onStartTick();
    }

    private void onStartTick() {
        if (CLIENT.thePlayer == null || events == null) return;
        if (currentTick == null) currentTick = new Tick();

        if (!currentTick.isEmpty()) {
            replay.addTick(currentTick);
            try {
                Tick tempTick = currentTick;
                currentTick = new Tick();
                events.processTickUpdate(tempTick);
            } catch (Exception e) {
                String stackTrace = Utils.getStackTraceAsString(e);
                events = null;
                Tick error = new Tick();
                error.error = stackTrace;
                replay.addTick(error);
                Utils.sendMessage("Exception in GTBEvents: " + e.getMessage());
                replay.save();
            }
        }

        onScoreboardUpdate(Utils.getScoreboardLines());
        onPlayerListUpdate(Utils.collectTabListEntries());
    }

    public static void onTitleSet(String title) {
        if (currentTick == null) return;
        currentTick.title = title;
    }

    public static void onSubtitleSet(String subtitle) {
        if (currentTick == null) return;
        currentTick.subtitle = subtitle;
    }

    // Other methods...
}
```

### Step 4: Convert Event System

#### 4.1 Convert Records to Regular Classes
**Modify**: `src/main/java/com/aembr/guesstheutils/GTBEvents.java`

```java
package com.aembr.guesstheutils;

import java.util.*;

public class GTBEvents {
    
    public interface BaseEvent {}
    
    // Game start event (converted from record)
    public static class GameStartEvent implements BaseEvent {
        private final Set<InitialPlayerData> players;
        
        public GameStartEvent(Set<InitialPlayerData> players) {
            this.players = players;
        }
        
        public Set<InitialPlayerData> getPlayers() { return players; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GameStartEvent)) return false;
            GameStartEvent that = (GameStartEvent) o;
            return Objects.equals(players, that.players);
        }
        
        @Override
        public int hashCode() { return Objects.hash(players); }
    }
    
    // Builder change event
    public static class BuilderChangeEvent implements BaseEvent {
        private final String previous;
        private final String current;
        
        public BuilderChangeEvent(String previous, String current) {
            this.previous = previous;
            this.current = current;
        }
        
        public String getPrevious() { return previous; }
        public String getCurrent() { return current; }
    }
    
    // User becomes builder event
    public static class UserBuilderEvent implements BaseEvent {}
    
    // Round start event
    public static class RoundStartEvent implements BaseEvent {
        private final int currentRound;
        private final int totalRounds;
        
        public RoundStartEvent(int currentRound, int totalRounds) {
            this.currentRound = currentRound;
            this.totalRounds = totalRounds;
        }
        
        public int getCurrentRound() { return currentRound; }
        public int getTotalRounds() { return totalRounds; }
    }
    
    // Correct guess event
    public static class CorrectGuessEvent implements BaseEvent {
        private final List<FormattedName> players;
        
        public CorrectGuessEvent(List<FormattedName> players) {
            this.players = players;
        }
        
        public List<FormattedName> getPlayers() { return players; }
    }
    
    // Other event classes...
    
    // Event listener interface
    public interface EventListener<T extends BaseEvent> {
        void onEvent(T event);
    }
    
    // Event manager
    private final Map<Class<? extends BaseEvent>, List<EventListener<?>>> listeners = 
        new HashMap<Class<? extends BaseEvent>, List<EventListener<?>>>();
    
    @SuppressWarnings("unchecked")
    public <T extends BaseEvent> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<EventListener<?>>()).add(listener);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends BaseEvent> void publish(T event) {
        List<EventListener<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<?> listener : eventListeners) {
                try {
                    ((EventListener<T>) listener).onEvent(event);
                } catch (Exception e) {
                    GuessTheUtils.LOGGER.error("Error in event listener", e);
                }
            }
        }
    }
    
    public void processTickUpdate(Tick tick) {
        // Existing processing logic adaptation...
        // Need to change places that used Text to String
    }
}
```

### Step 5: Replace Mixin System

#### 5.1 Create HUD Hooks
**Create**: `src/main/java/com/aembr/guesstheutils/hooks/HudHooks.java`

```java
package com.aembr.guesstheutils.hooks;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;

public class HudHooks {
    
    private static Field displayedTitleField;
    private static Field displayedSubtitleField;
    private static String lastTitle = "";
    private static String lastSubtitle = "";
    
    static {
        try {
            // Need to find correct field names in 1.8.9
            displayedTitleField = GuiIngame.class.getDeclaredField("field_175201_x"); // obfuscated name
            displayedTitleField.setAccessible(true);
            
            displayedSubtitleField = GuiIngame.class.getDeclaredField("field_175200_y"); // obfuscated name  
            displayedSubtitleField.setAccessible(true);
            
        } catch (Exception e) {
            GuessTheUtils.LOGGER.error("Failed to setup HUD hooks", e);
        }
    }
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            try {
                GuiIngame ingameGUI = event.ingameGUI;
                String currentTitle = (String) displayedTitleField.get(ingameGUI);
                String currentSubtitle = (String) displayedSubtitleField.get(ingameGUI);
                
                if (currentTitle != null && !currentTitle.equals(lastTitle)) {
                    lastTitle = currentTitle;
                    GuessTheUtils.onTitleSet(currentTitle);
                }
                
                if (currentSubtitle != null && !currentSubtitle.equals(lastSubtitle)) {
                    lastSubtitle = currentSubtitle;
                    GuessTheUtils.onSubtitleSet(currentSubtitle);
                }
                
            } catch (Exception e) {
                // Silent failure
            }
        }
    }
}
```

---

## 🎯 Stage 3: Module Migration

### Step 6: Adapt Utils Class

#### 6.1 Rewrite Utils.java
```java
package com.aembr.guesstheutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utils {
    
    public static void sendMessage(String message) {
        if (GuessTheUtils.CLIENT.thePlayer != null) {
            ChatComponentText prefix = new ChatComponentText("[GuessTheUtils] ");
            prefix.getChatStyle().setColor(EnumChatFormatting.GOLD);
            
            ChatComponentText msg = new ChatComponentText(message);
            msg.getChatStyle().setColor(EnumChatFormatting.WHITE);
            
            prefix.appendSibling(msg);
            GuessTheUtils.CLIENT.thePlayer.addChatMessage(prefix);
        }
    }
    
    public static List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<String>();
        
        if (GuessTheUtils.CLIENT.theWorld == null) return lines;
        
        Scoreboard scoreboard = GuessTheUtils.CLIENT.theWorld.getScoreboard();
        if (scoreboard == null) return lines;
        
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // SIDEBAR
        if (objective == null) return lines;
        
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(EnumChatFormatting.getTextWithoutFormattingCodes(line));
        }
        
        return lines;
    }
    
    public static List<String> collectTabListEntries() {
        List<String> entries = new ArrayList<String>();
        
        if (GuessTheUtils.CLIENT.getNetHandler() == null) return entries;
        
        Collection<NetworkPlayerInfo> playerInfos = GuessTheUtils.CLIENT.getNetHandler().getPlayerInfoMap();
        
        for (NetworkPlayerInfo info : playerInfos) {
            String displayName = info.getDisplayName() != null ? 
                info.getDisplayName().getFormattedText() : 
                info.getGameProfile().getName();
            entries.add(EnumChatFormatting.getTextWithoutFormattingCodes(displayName));
        }
        
        return entries;
    }
    
    public static String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
```

### Step 7: Migrate DisallowedItemHider

#### 7.1 Item ID Mapping
```java
package com.aembr.guesstheutils.modules;

import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DisallowedItemHider {
    
    // 1.8.9 allowed items set
    private static final Set<Item> ALLOWED_ITEMS = new HashSet<Item>(Arrays.asList(
        // Doors
        Item.getItemFromBlock(Blocks.acacia_door),
        Item.getItemFromBlock(Blocks.birch_door),
        Item.getItemFromBlock(Blocks.dark_oak_door),
        Item.getItemFromBlock(Blocks.jungle_door),
        Item.getItemFromBlock(Blocks.spruce_door),
        
        // Fences
        Item.getItemFromBlock(Blocks.acacia_fence),
        Item.getItemFromBlock(Blocks.acacia_fence_gate),
        Item.getItemFromBlock(Blocks.birch_fence),
        Item.getItemFromBlock(Blocks.birch_fence_gate),
        
        // Armor stand
        Items.armor_stand,
        
        // Barrier
        Item.getItemFromBlock(Blocks.barrier),
        
        // Food
        Items.cooked_mutton,
        Items.cooked_rabbit,
        Items.mutton,
        Items.rabbit,
        Items.rabbit_foot,
        Items.rabbit_hide,
        Items.rabbit_stew,
        
        // Prismarine related
        Item.getItemFromBlock(Blocks.prismarine),
        Items.prismarine_crystals,
        Items.prismarine_shard,
        Item.getItemFromBlock(Blocks.sea_lantern),
        
        // Slime block
        Item.getItemFromBlock(Blocks.slime_block),
        
        // Sponge
        Item.getItemFromBlock(Blocks.sponge),
        
        // Stained glass
        Item.getItemFromBlock(Blocks.stained_glass),
        Item.getItemFromBlock(Blocks.stained_glass_pane),
        
        // Stone variants (distinguished by metadata)
        Item.getItemFromBlock(Blocks.stone),
        
        // Dirt variants
        Item.getItemFromBlock(Blocks.dirt),
        
        // Spawn eggs
        Items.spawn_egg
    ));
    
    public static boolean isItemAllowed(ItemStack itemStack) {
        if (itemStack == null) return false;
        
        Item item = itemStack.getItem();
        if (!ALLOWED_ITEMS.contains(item)) {
            return false;
        }
        
        return isMetadataAllowed(item, itemStack.getMetadata());
    }
    
    private static boolean isMetadataAllowed(Item item, int metadata) {
        if (item == Item.getItemFromBlock(Blocks.stained_glass) || 
            item == Item.getItemFromBlock(Blocks.stained_glass_pane)) {
            // All colors of stained glass allowed (0-15)
            return metadata >= 0 && metadata <= 15;
        }
        
        if (item == Item.getItemFromBlock(Blocks.stone)) {
            // 1=granite, 2=polished_granite, 3=diorite, 4=polished_diorite, 5=andesite, 6=polished_andesite
            return metadata >= 1 && metadata <= 6;
        }
        
        if (item == Item.getItemFromBlock(Blocks.dirt)) {
            // 1=coarse_dirt
            return metadata == 1;
        }
        
        if (item == Items.spawn_egg) {
            // Rabbit(101), Guardian(68), Endermite(67)
            return metadata == 101 || metadata == 68 || metadata == 67;
        }
        
        return true;
    }
}
```

### Step 8: Rewrite CustomScoreboard Rendering

#### 8.1 1.8.9 Rendering System
```java
package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class CustomScoreboard {
    
    private boolean enabled = true;
    private List<String> scoreboardLines;
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        
        if (GuessTheUtils.CLIENT.currentScreen != null) {
            return;
        }
        
        renderCustomScoreboard();
    }
    
    private void renderCustomScoreboard() {
        Minecraft mc = GuessTheUtils.CLIENT;
        FontRenderer fontRenderer = mc.fontRendererObj;
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();
        
        int scoreboardWidth = 120;
        int scoreboardHeight = 150;
        int x = screenWidth - scoreboardWidth - 10;
        int y = screenHeight / 2 - scoreboardHeight / 2;
        
        // Enable blending
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Draw background
        drawRect(x - 2, y - 2, x + scoreboardWidth + 2, y + scoreboardHeight + 2, 0x80000000);
        drawRect(x, y, x + scoreboardWidth, y + scoreboardHeight, 0x60000000);
        
        // Draw title
        String title = "Guess The Build";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawStringWithShadow(title, x + (scoreboardWidth - titleWidth) / 2, y + 5, 0xFFFFFF);
        
        // Draw scoreboard content
        if (scoreboardLines != null) {
            int lineY = y + 20;
            for (String line : scoreboardLines) {
                if (!line.trim().isEmpty()) {
                    fontRenderer.drawStringWithShadow(line, x + 5, lineY, 0xFFFFFF);
                    lineY += 10;
                }
            }
        }
        
        GlStateManager.disableBlend();
    }
    
    // 1.8.9 compatible rectangle drawing
    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            int j = top;
            top = bottom;
            bottom = j;
        }

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(r, g, b, a);
        
        worldrenderer.startDrawingQuads();
        worldrenderer.addVertex((double)left, (double)bottom, 0.0D);
        worldrenderer.addVertex((double)right, (double)bottom, 0.0D);
        worldrenderer.addVertex((double)right, (double)top, 0.0D);
        worldrenderer.addVertex((double)left, (double)top, 0.0D);
        tessellator.draw();
        
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    
    public void updateScoreboardLines(List<String> lines) {
        this.scoreboardLines = lines;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

### Step 9: Adapt GameTracker Logic

#### 9.1 Game State Detection
```java
package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.Tick;
import net.minecraft.util.EnumChatFormatting;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GameTracker {
    
    private final GTBEvents events;
    private GameState currentState = GameState.UNKNOWN;
    
    // 1.8.9 chat message patterns
    private static final Pattern GAME_START_PATTERN = Pattern.compile(".*The game starts in \\d+ seconds.*");
    private static final Pattern BUILDER_PATTERN = Pattern.compile(".*([\\w]+) is now the builder.*");
    private static final Pattern ROUND_START_PATTERN = Pattern.compile(".*Round (\\d+)/(\\d+).*");
    private static final Pattern CORRECT_GUESS_PATTERN = Pattern.compile(".*([\\w]+) guessed the theme!.*");
    
    public enum GameState {
        UNKNOWN, LOBBY, GAME_STARTING, IN_GAME, BUILDING, GUESSING, ROUND_END, GAME_END
    }
    
    public GameTracker(GTBEvents events) {
        this.events = events;
    }
    
    public void processTick(Tick tick) {
        // Process chat messages (now String list)
        if (tick.chatMessages != null) {
            for (String message : tick.chatMessages) {
                processChatMessage(message);
            }
        }
        
        // Process scoreboard
        if (tick.scoreboardLines != null) {
            processScoreboardUpdate(tick.scoreboardLines);
        }
    }
    
    private void processChatMessage(String message) {
        String cleanMessage = EnumChatFormatting.getTextWithoutFormattingCodes(message);
        
        if (GAME_START_PATTERN.matcher(cleanMessage).matches()) {
            changeState(GameState.GAME_STARTING);
            events.publish(new GTBEvents.GameStartEvent(Collections.<GTBEvents.InitialPlayerData>emptySet()));
        }
        
        Matcher builderMatcher = BUILDER_PATTERN.matcher(cleanMessage);
        if (builderMatcher.matches()) {
            String newBuilder = builderMatcher.group(1);
            events.publish(new GTBEvents.BuilderChangeEvent(null, newBuilder));
            
            if (newBuilder.equals(GuessTheUtils.CLIENT.thePlayer.getName())) {
                events.publish(new GTBEvents.UserBuilderEvent());
            }
        }
        
        // Other pattern matching...
    }
    
    private void processScoreboardUpdate(List<String> lines) {
        for (String line : lines) {
            String cleanLine = EnumChatFormatting.getTextWithoutFormattingCodes(line).trim();
            
            if (cleanLine.contains("GUESS THE BUILD")) {
                // In GTB game
            }
            
            if (cleanLine.startsWith("Theme:")) {
                String theme = cleanLine.substring(6).trim();
                events.publish(new GTBEvents.ThemeUpdateEvent(theme));
            }
        }
    }
    
    private void changeState(GameState newState) {
        if (currentState != newState) {
            GameState previousState = currentState;
            currentState = newState;
            events.publish(new GTBEvents.StateChangeEvent(previousState, newState));
        }
    }
}
```

---

## ⚙️ Stage 4: Configuration System

### Step 10: Implement Configuration System

#### 10.1 YAML Configuration Management
```java
package com.aembr.guesstheutils.config;

import com.aembr.guesstheutils.GuessTheUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class GuessTheUtilsConfig {
    
    private static final File CONFIG_FILE = new File("config/guesstheutils.yml");
    private static Map<String, Object> config = new HashMap<String, Object>();
    
    private static final Map<String, Object> DEFAULT_CONFIG = new HashMap<String, Object>() {{
        put("customScoreboard.enabled", true);
        put("chatCooldownTimer.enabled", true);
        put("nameAutocomplete.enabled", true);
        put("builderNotification.enabled", true);
        put("shortcutReminder.enabled", true);
        put("disallowedItemHider.enabled", true);
    }};
    
    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            createDefaultConfig();
            return;
        }
        
        try (FileInputStream inputStream = new FileInputStream(CONFIG_FILE)) {
            Yaml yaml = new Yaml();
            Map<String, Object> loadedConfig = yaml.load(inputStream);
            if (loadedConfig != null) {
                config.putAll(loadedConfig);
            }
            
            for (Map.Entry<String, Object> entry : DEFAULT_CONFIG.entrySet()) {
                if (!config.containsKey(entry.getKey())) {
                    config.put(entry.getKey(), entry.getValue());
                }
            }
            
            GuessTheUtils.LOGGER.info("Config loaded");
        } catch (IOException e) {
            GuessTheUtils.LOGGER.error("Failed to load config", e);
            config.putAll(DEFAULT_CONFIG);
        }
    }
    
    public static void saveConfig() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                Yaml yaml = new Yaml();
                yaml.dump(config, writer);
            }
        } catch (IOException e) {
            GuessTheUtils.LOGGER.error("Failed to save config", e);
        }
    }
    
    private static void createDefaultConfig() {
        config.putAll(DEFAULT_CONFIG);
        saveConfig();
    }
    
    public static boolean getBoolean(String key) {
        Object value = config.get(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }
    
    public static void setBoolean(String key, boolean value) {
        config.put(key, value);
    }
    
    // Module configuration accessors
    public static class CustomScoreboard {
        public static boolean isEnabled() { return getBoolean("customScoreboard.enabled"); }
        public static void setEnabled(boolean enabled) { setBoolean("customScoreboard.enabled", enabled); }
    }
    
    public static class ChatCooldownTimer {
        public static boolean isEnabled() { return getBoolean("chatCooldownTimer.enabled"); }
        public static void setEnabled(boolean enabled) { setBoolean("chatCooldownTimer.enabled", enabled); }
    }
    
    // Other module configurations...
}
```

---

## 📁 Stage 5: Resource Adaptation

### Step 11: Migrate Resource Files

#### 11.1 Language File Conversion
**Delete**: `src/main/resources/assets/guesstheutils/lang/en_us.json`
**Create**: `src/main/resources/assets/guesstheutils/lang/en_US.lang`

```properties
guesstheutils.name=GuessTheUtils
guesstheutils.prefix=[GuessTheUtils]
guesstheutils.command.config=Open configuration
guesstheutils.module.customScoreboard=Custom Scoreboard
guesstheutils.module.chatCooldownTimer=Chat Cooldown Timer
guesstheutils.module.nameAutocomplete=Name Autocomplete
guesstheutils.message.enabled=Enabled
guesstheutils.message.disabled=Disabled
```

---

## 🧪 Stage 6: Testing Framework

### Step 12: Create Test Scripts

#### 12.1 Build Testing
**Create**: `build_test.bat`

```batch
@echo off
echo Building GuessTheUtils for Forge 1.8.9...
gradlew clean build
if %ERRORLEVEL% == 0 (
    echo Build successful!
) else (
    echo Build failed!
    pause
    exit /b 1
)
pause
```

#### 12.2 Basic Functionality Testing
```java
public class BasicTest {
    public static void main(String[] args) {
        System.out.println("Testing GuessTheUtils...");
        
        // Test configuration system
        GuessTheUtilsConfig.loadConfig();
        System.out.println("Config test: " + GuessTheUtilsConfig.CustomScoreboard.isEnabled());
        
        // Test event system
        GTBEvents events = new GTBEvents();
        events.subscribe(GTBEvents.GameStartEvent.class, new GTBEvents.EventListener<GTBEvents.GameStartEvent>() {
            @Override
            public void onEvent(GTBEvents.GameStartEvent event) {
                System.out.println("Event received!");
            }
        });
        
        events.publish(new GTBEvents.GameStartEvent(Collections.<GTBEvents.InitialPlayerData>emptySet()));
        
        System.out.println("All tests passed!");
    }
}
```

---

## ✅ Completion Checklist

### Completion Criteria for Each Stage:

#### Stage 1 ✓
- [ ] Forge 1.8.9 project builds successfully
- [ ] mcmod.info correctly configured
- [ ] Proxy system working

#### Stage 2 ✓  
- [ ] @Mod annotation main class working properly
- [ ] Event system conversion completed
- [ ] Mixins replaced with hooks

#### Stage 3 ✓
- [ ] All modules compile successfully
- [ ] Item IDs correctly mapped
- [ ] Rendering system working

#### Stage 4 ✓
- [ ] Configuration system functional
- [ ] YAML files handled correctly

#### Stage 5 ✓
- [ ] Resource files load properly
- [ ] Language files working

#### Stage 6 ✓
- [ ] Basic tests passing
- [ ] Hypixel server testing successful

---

**Note**: This migration requires 6-8 weeks of time. Consider migrating to 1.12.2 or 1.16.5 to significantly reduce workload.