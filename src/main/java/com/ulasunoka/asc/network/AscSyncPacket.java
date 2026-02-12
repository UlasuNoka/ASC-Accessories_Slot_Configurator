package com.ulasunoka.asc.network;

import com.google.gson.Gson;
import com.ulasunoka.asc.config.AscConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class AscSyncPacket {

    public static final Identifier PACKET_ID = Identifier.of("asc", "config_sync");
    private static final Gson GSON = new Gson();

    // Data payload definition for Fabric Networking API (1.21.1)
    public record SyncPayload(String configJson) implements CustomPayload {
        public static final CustomPayload.Id<SyncPayload> ID = new CustomPayload.Id<>(PACKET_ID);
        
        public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.configJson),
            buf -> new SyncPayload(buf.readString())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static class Server {
        
        public static void init() {
            // Register packet type for S2C communication
            PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
            
            // Send server config to player on join to prevent desync
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    String configJson = GSON.toJson(AscConfig.get());
                    ServerPlayNetworking.send(handler.player, new SyncPayload(configJson));
                    
                    // Debug log to confirm sync attempt
                    // System.out.println("[ASC] Sent config to " + handler.player.getName().getString());
                }
            );
        }
    }

    public static class Client {
        
        public static void init() {
            // Register receiver on client side
            ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> {
                context.client().execute(() -> {
                    try {
                        // Parse received JSON
                        AscConfig receivedConfig = GSON.fromJson(payload.configJson(), AscConfig.class);
                        
                        // CRITICAL: Update ONLY the runtime cache, do not overwrite local config file
                        // This ensures server rules apply without messing up client settings
                        AscConfig.RULE_CACHE.clear();
                        if (receivedConfig.rules != null) {
                            for (AscConfig.SlotRule rule : receivedConfig.rules) {
                                AscConfig.RULE_CACHE.put(rule.itemId, rule);
                            }
                        }
                        
                        // System.out.println("[ASC] Synced " + receivedConfig.rules.size() + " rules from server");
                        
                    } catch (Exception e) {
                        System.err.println("[ASC] Failed to sync config: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            });
        }
    }
}