package com.aembr.guesstheutils.proxy;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        super.preInit();
    }

    @Override
    public void init() {
        super.init();
        
        // Register event handlers - will be added as modules are created
        MinecraftForge.EVENT_BUS.register(new GuessTheUtils());
    }

    @Override
    public void postInit() {
        super.postInit();
    }
} 