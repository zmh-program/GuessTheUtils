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
        MinecraftForge.EVENT_BUS.register(new HudHooks());
    }

    @Override
    public void postInit() {
        super.postInit();
    }
} 