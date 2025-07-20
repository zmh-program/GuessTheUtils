package com.aembr.guesstheutils.config;

import com.aembr.guesstheutils.GuessTheUtils;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.autogen.Boolean;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class GuessTheUtilsConfig {
    public static final Path CONFIG_PATH = YACLPlatform.getConfigDir().resolve("guesstheutils.json");

    public static final ConfigClassHandler<GuessTheUtilsConfig> CONFIG = ConfigClassHandler
            .createBuilder(GuessTheUtilsConfig.class)
            .id(Identifier.of(GuessTheUtils.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(CONFIG_PATH)
                    .build())
            .build();

    // CUSTOM SCOREBOARD

    public enum CustomScoreboardOption { ON, EXPANDED, OFF }

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @TickBox
    public boolean enableCustomScoreboardModule = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public CustomScoreboardOption customScoreboardShowPlaces = CustomScoreboardOption.EXPANDED;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public CustomScoreboardOption customScoreboardShowTitles = CustomScoreboardOption.EXPANDED;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    CustomScoreboardOption customScoreboardShowEmblems = CustomScoreboardOption.ON;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public CustomScoreboardOption customScoreboardShowPoinsGainedInRound = CustomScoreboardOption.ON;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @TickBox
    public boolean customScoreboardTextShadow = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @IntSlider(min = 0, max = 5, step = 1)
    public int customScoreboardLinePadding = 1;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @IntSlider(min = 0, max = 5, step = 1)
    public int customScoreboardLineSpacing = 0;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @IntSlider(min = 0, max = 20, step = 1)
    public int customScoreboardSeparatorHeight = 6;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @TickBox
    public boolean customScoreboardDrawSeparatorBackground = false;

    // SHORTCUT REMINDER

    @SerialEntry
    @AutoGen(category = "modules", group = "shortcut_reminder")
    @TickBox
    public boolean enableShortcutReminderModule = true;

    // CHAT COOLDOWN

    @SerialEntry
    @AutoGen(category = "modules", group = "chat_cooldown")
    @TickBox
    public boolean enableChatCooldownModule = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "chat_cooldown")
    @IntSlider(min = 0, max = 100, step = 1)
    public int chatCooldownPingVolume = 50;

    @SerialEntry
    @AutoGen(category = "modules", group = "chat_cooldown")
    @TickBox
    public boolean chatCooldownTimer = true;

    // BUILDER NOTIFICATION

    @SerialEntry
    @AutoGen(category = "modules", group = "builder_notification")
    @TickBox
    public boolean enableBuilderNotificationModule = true;

    // DISALLOWED ITEM HIDER

    @SerialEntry
    @AutoGen(category = "modules", group = "disallowed_item_hider")
    @TickBox
    public boolean enableDisallowedItemHiderModule = true;

    // NAME AUTOCOMPLETE

    @SerialEntry
    @AutoGen(category = "modules", group = "name_autocomplete")
    @TickBox
    public boolean enableNameAutocompleteModule = true;

    public static Screen createScreen(@Nullable Screen parent) {
        return CONFIG.generateGui().generateScreen(parent);
    }
}
