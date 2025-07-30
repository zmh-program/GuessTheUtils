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
    
    public static Minecraft getClient() {
        return Minecraft.getMinecraft();
    }

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
    private String previousScreenTitle = "";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("GuessTheUtils preInit");
        
        GuessTheUtilsConfig.init(event.getSuggestedConfigurationFile());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("GuessTheUtils init");
        proxy.init();
        
        replay.initialize();
        shortcutReminder.init();
        liveE2ERunner = new LiveE2ERunner(Replay.load(GuessTheUtils.class.getResourceAsStream("/assets/live_tests/TestBuggyLeaverDetection.json")));
        
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new Commands());
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