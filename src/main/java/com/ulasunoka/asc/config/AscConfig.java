package com.ulasunoka.asc.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AscConfig {

    public static final ConfigClassHandler<AscConfig> HANDLER = ConfigClassHandler.createBuilder(AscConfig.class)
            .id(new Identifier("asc", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("asc.json5"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .setJson5(true)
                    .build())
            .build();

    // Main storage for config rules. Saved to disk.
    @SerialEntry
    public List<SlotRule> rules = new ArrayList<>();

    // Runtime cache for O(1) lookups during game tick/rendering.
    // NOT saved to disk. Must be rebuilt manually via updateCache().
    public static Map<String, SlotRule> RULE_CACHE = new HashMap<>();

    public static class SlotRule {
        public String itemId = "minecraft:stick";
        public List<String> targetSlots = new ArrayList<>();
        public OperationMode mode = OperationMode.MERGE;

        public enum OperationMode {
            MERGE,   // Allows item in new slots + original slots
            REPLACE  // Allows item in new slots ONLY (blocks original)
        }
    }

    /**
     * Rebuilds RULE_CACHE from the current 'rules' list.
     * Must be called on startup, after config save, and after packet sync.
     */
    public static void updateCache() {
        RULE_CACHE.clear();
        AscConfig config = get();

        if (config.rules != null) {
            for (SlotRule rule : config.rules) {
                // Key is raw Item ID string (e.g., "minecraft:stick")
                RULE_CACHE.put(rule.itemId, rule);
            }
        }
    }

    // Helper to fetch instance from config handler
    public static AscConfig get() {
        return HANDLER.instance();
    }

    public static void load() {
        HANDLER.load();
    }

    public static void save() {
        HANDLER.save();
    }
}