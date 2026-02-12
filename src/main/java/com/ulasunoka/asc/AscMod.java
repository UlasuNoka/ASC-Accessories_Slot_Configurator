package com.ulasunoka.asc;

import com.ulasunoka.asc.config.AscConfig;
import com.ulasunoka.asc.network.AscSyncPacket;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AscMod implements ModInitializer {
    
    public static final String MOD_ID = "asc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        LOGGER.info("[ASC] Initializing...");
        
        // Register config with AutoConfig
        AutoConfig.register(AscConfig.class, GsonConfigSerializer::new);
        
        // Build initial cache from config
        AscConfig.updateCache();
        LOGGER.info("[ASC] Loaded {} slot rules", AscConfig.get().rules.size());
        
        // Init server-side networking for config sync
        AscSyncPacket.Server.init();
        
        LOGGER.info("[ASC] Initialization complete.");
    }
}