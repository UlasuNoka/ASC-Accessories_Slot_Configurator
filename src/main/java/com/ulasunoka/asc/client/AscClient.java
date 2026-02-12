package com.ulasunoka.asc.client;

import com.ulasunoka.asc.network.AscSyncPacket;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AscClient implements ClientModInitializer {
    
    public static final Logger LOGGER = LoggerFactory.getLogger("asc-client");

    @Override
    public void onInitializeClient() {
        // Initialize client-side networking to receive config from server
        AscSyncPacket.Client.init();
        
        LOGGER.info("[ASC] Client initialization complete");
    }
}