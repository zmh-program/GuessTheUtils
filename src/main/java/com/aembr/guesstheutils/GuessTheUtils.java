package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.aembr.guesstheutils.hooks.HudHooks;
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

import net.minecraft.client.gui.GuiScreen;

@Mod(modid = GuessTheUtils.MOD_ID, name = GuessTheUtils.MOD_NAME, version = GuessTheUtils.VERSION, clientSideOnly = true)
public class GuessTheUtils {
    public static final String MOD_ID = "guesstheutils";
    public static final String MOD_NAME = "GuessTheUtils";
    public static final String VERSION = "0.9.5";
    
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    public static Minecraft getClient() {
        return Minecraft.getMinecraft();
    }

    @SidedProxy(clientSide = "com.aembr.guesstheutils.proxy.ClientProxy", serverSide = "com.aembr.guesstheutils.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static final Replay replay = new Replay();
    public static GTBEvents events = new GTBEvents();

    public static GameTracker gameTracker = new GameTracker(events);
    public static ShortcutReminder shortcutReminder = new ShortcutReminder(events);
    public static BuilderNotification builderNotification = new BuilderNotification(events);
    public static ChatCooldownTimer chatCooldown = new ChatCooldownTimer(events);

    public static boolean testing = false;
    public static LiveE2ERunner liveE2ERunner;
    public static boolean debugMode = false;

    private static Tick currentTick;
    
    public static Tick getCurrentTick() {
        return currentTick;
    }
    private List<String> previousScoreboardLines = new ArrayList<String>();
    private List<String> previousPlayerListEntries = new ArrayList<String>();
    private String previousActionBarMessage = "";
    private String previousScreenTitle = "";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("GuessTheUtils preInit");
        
        GuessTheUtilsConfig.init(event.getSuggestedConfigurationFile());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("GuessTheUtils init starting...");
        proxy.init();
        
        try {
            LOGGER.info("Initializing replay system...");
            replay.initialize();
            
            LOGGER.info("Initializing shortcut reminder...");
            shortcutReminder.init();
            
            LOGGER.info("Loading live E2E runner...");
            java.io.InputStream testStream = GuessTheUtils.class.getResourceAsStream("/assets/live_tests/TestBuggyLeaverDetection.json");
            if (testStream == null) {
                LOGGER.warn("Test file not found, creating empty runner");
                liveE2ERunner = new LiveE2ERunner(new java.util.ArrayList<com.google.gson.JsonObject>());
            } else {
                try {
                    liveE2ERunner = new LiveE2ERunner(Replay.load(testStream));
                    LOGGER.info("Live E2E runner loaded successfully");
                } catch (Exception e) {
                    LOGGER.error("Failed to load test file due to JSON error, creating empty runner", e);
                    liveE2ERunner = new LiveE2ERunner(new java.util.ArrayList<com.google.gson.JsonObject>());
                }
            }
            
            LOGGER.info("Registering commands...");
            net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new Commands());
            
            LOGGER.info("GuessTheUtils init completed successfully!");
        } catch (Exception e) {
            LOGGER.error("Error during GuessTheUtils initialization", e);
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("GuessTheUtils postInit starting...");
        proxy.postInit();
        
        // Register this mod instance for events
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Registered GuessTheUtils for events");
        
        // Register HUD hooks for rendering
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new HudHooks());
        LOGGER.info("Registered HudHooks for rendering events");
        
        // Register scoreboard interceptor for direct scoreboard modification
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.aembr.guesstheutils.modules.ScoreboardInterceptor());
        LOGGER.info("Registered ScoreboardInterceptor for scoreboard content modification");
        
        // Register chat interceptor for chat cooldown
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ChatInterceptor());
        ChatInterceptor.init();
        LOGGER.info("Registered ChatInterceptor for chat message interception");
        
        // Enable chat cooldown for testing (normally enabled by game events)
        if (chatCooldown != null) {
            chatCooldown.enable();
            LOGGER.info("ChatCooldown enabled for testing");
        }
        
        LOGGER.info("GuessTheUtils postInit completed!");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        onStartTick();
    }

    private void onStartTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || events == null) return;
        if (currentTick == null) currentTick = new Tick();

        if (testing) {
            currentTick = liveE2ERunner.getNext();
        }

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
        
        gameTracker.onTick();
        chatCooldown.onTick();
    }

    public void onScoreboardUpdate(List<String> lines) {
        if (currentTick == null) return;
        currentTick.scoreboardLines = lines;
    }

    public void onPlayerListUpdate(List<String> entries) {
        if (currentTick == null) return;
        currentTick.playerListEntries = entries;
    }

    public static void onTitleSet(String title) {
        // Title handling - placeholder for now
    }

    public static void onSubtitleSet(String subtitle) {
        // Subtitle handling - placeholder for now
    }
} 