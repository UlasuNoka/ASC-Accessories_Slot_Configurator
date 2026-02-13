package com.ulasunoka.asc.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ulasunoka.asc.config.AscConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;
import java.util.List;

public class AscSyncPacket {

    public static final Identifier PACKET_ID = Identifier.of("asc", "config_sync");
    private static final Gson GSON = new Gson();
    private static final Type RULE_LIST_TYPE = new TypeToken<List<AscConfig.SlotRuleData>>() {}.getType();

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
            PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);

            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                    (handler, sender, server) -> {
                        String configJson = GSON.toJson(AscConfig.get().rules.stream().map(AscConfig.SlotRule::export).toList());
                        ServerPlayNetworking.send(handler.player, new SyncPayload(configJson));
                    }
            );
        }
    }

    public static class Client {

        public static void init() {
            ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> {
                context.client().execute(() -> {
                    try {
                        List<AscConfig.SlotRuleData> receivedRules = GSON.fromJson(payload.configJson(), RULE_LIST_TYPE);

                        AscConfig.RULE_CACHE.clear();
                        if (receivedRules != null) {
                            for (AscConfig.SlotRuleData rule : receivedRules) {
                                AscConfig.RULE_CACHE.put(rule.itemId(), rule);
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("[ASC] Failed to sync config: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            });
        }
    }
}
