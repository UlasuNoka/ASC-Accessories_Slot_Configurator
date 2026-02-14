package com.ulasunoka.asc.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class AscConfig {

    public static final ConfigClassHandler<AscConfig> HANDLER = ConfigClassHandler.createBuilder(AscConfig.class)
            .id(Identifier.of("asc", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("asc.json5"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .setJson5(true)
                    .build())
            .build();

    private static final Logger LOGGER = LoggerFactory.getLogger("asc-config");

    // Main storage for config rules. Saved to disk.
    @SerialEntry
    public List<SlotRule> rules = new ArrayList<>();

    // Global advanced-mode gate for custom slot typing.
    @SerialEntry
    public boolean allowCustomSlots = false;

    // Runtime cache for O(1) lookups during game tick/rendering.
    // NOT saved to disk. Must be rebuilt manually via updateCache().
    public static Map<String, SlotRule> RULE_CACHE = new HashMap<>();

    public static class SlotRule {
        public String itemId = "minecraft:stick";
        public List<String> targetSlots = new ArrayList<>();
        public OperationMode mode = OperationMode.MERGE;
        public boolean disableEquipping = false;

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
        rebuildCacheFromRules(get().rules, "local config");
    }

    public static void rebuildCacheFromRules(List<SlotRule> rules, String source) {
        RULE_CACHE.clear();

        if (rules == null || rules.isEmpty()) {
            return;
        }

        Map<String, SlotRule> sanitized = new LinkedHashMap<>();
        int invalidItemCount = 0;
        int duplicateItemCount = 0;
        int invalidReplaceCount = 0;

        for (SlotRule rawRule : rules) {
            if (rawRule == null) {
                continue;
            }

            String itemId = sanitizeItemId(rawRule.itemId);
            if (itemId == null) {
                invalidItemCount++;
                continue;
            }

            SlotRule sanitizedRule = sanitizeRule(rawRule, itemId);
            if (!sanitizedRule.disableEquipping
                    && sanitizedRule.mode == SlotRule.OperationMode.REPLACE
                    && sanitizedRule.targetSlots.isEmpty()) {
                // Explicitly skip this footgun. "Disable equipping" is the intended explicit behavior.
                invalidReplaceCount++;
                continue;
            }

            if (sanitized.containsKey(itemId)) {
                // Deterministic duplicate policy: last valid definition wins.
                duplicateItemCount++;
            }
            sanitized.put(itemId, sanitizedRule);
        }

        RULE_CACHE.putAll(sanitized);

        if (invalidItemCount > 0 || duplicateItemCount > 0 || invalidReplaceCount > 0) {
            LOGGER.warn("[ASC] Sanitized {} rules from {} (invalid itemId: {}, duplicate itemId: {}, replace-without-slots skipped: {})",
                    rules.size(),
                    source,
                    invalidItemCount,
                    duplicateItemCount,
                    invalidReplaceCount);
        }
    }

    private static SlotRule sanitizeRule(SlotRule rawRule, String itemId) {
        SlotRule sanitizedRule = new SlotRule();
        sanitizedRule.itemId = itemId;
        sanitizedRule.mode = rawRule.mode == null ? SlotRule.OperationMode.MERGE : rawRule.mode;
        sanitizedRule.disableEquipping = rawRule.disableEquipping;

        LinkedHashSet<String> slots = new LinkedHashSet<>();
        if (rawRule.targetSlots != null) {
            for (String slot : rawRule.targetSlots) {
                if (slot == null) {
                    continue;
                }
                String trimmed = slot.trim();
                if (!trimmed.isEmpty()) {
                    slots.add(trimmed);
                }
            }
        }
        sanitizedRule.targetSlots = new ArrayList<>(slots);
        return sanitizedRule;
    }

    private static String sanitizeItemId(String itemId) {
        if (itemId == null) {
            return null;
        }

        String trimmed = itemId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return Identifier.tryParse(trimmed) == null ? null : trimmed;
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
