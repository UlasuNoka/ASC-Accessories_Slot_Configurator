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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AscConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("asc-config");

    public static final ConfigClassHandler<AscConfig> HANDLER = ConfigClassHandler.createBuilder(AscConfig.class)
            .id(Identifier.of("asc", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("asc.json5"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public List<SlotRule> rules = new ArrayList<>();

    @SerialEntry
    public boolean allowCustomSlots = false;

    public static Map<String, SlotRule> RULE_CACHE = new HashMap<>();

    public static class SlotRule {
        public String itemId = "minecraft:stick";
        public List<String> targetSlots = new ArrayList<>();
        public OperationMode mode = OperationMode.MERGE;
        public boolean disableEquipping = false;

        public enum OperationMode {
            MERGE,
            REPLACE
        }
    }

    public static void updateCache() {
        RULE_CACHE.clear();
        AscConfig config = get();
        config.rules = sanitizeRules(config.rules, true);

        for (SlotRule rule : config.rules) {
            RULE_CACHE.put(rule.itemId, rule);
        }
    }

    public static List<SlotRule> sanitizeRules(List<SlotRule> inputRules, boolean logWarnings) {
        if (inputRules == null) {
            return new ArrayList<>();
        }

        List<SlotRule> sanitized = new ArrayList<>();
        Set<String> seenItemIds = new LinkedHashSet<>();

        for (SlotRule originalRule : inputRules) {
            if (originalRule == null) {
                continue;
            }

            SlotRule rule = new SlotRule();
            rule.itemId = sanitizeItemId(originalRule.itemId);
            rule.mode = originalRule.mode == null ? SlotRule.OperationMode.MERGE : originalRule.mode;
            rule.disableEquipping = originalRule.disableEquipping;
            rule.targetSlots = sanitizeSlotNames(originalRule.targetSlots);

            if (rule.itemId == null) {
                if (logWarnings) {
                    LOGGER.warn("[ASC] Skipping invalid rule with blank or malformed itemId: {}", originalRule.itemId);
                }
                continue;
            }

            if (!seenItemIds.add(rule.itemId)) {
                if (logWarnings) {
                    LOGGER.warn("[ASC] Duplicate rule for itemId '{}' ignored (first rule wins)", rule.itemId);
                }
                continue;
            }

            if (!rule.disableEquipping && rule.mode == SlotRule.OperationMode.REPLACE && rule.targetSlots.isEmpty()) {
                if (logWarnings) {
                    LOGGER.warn("[ASC] Ignoring REPLACE rule with empty targetSlots for itemId '{}'", rule.itemId);
                }
                continue;
            }

            sanitized.add(rule);
        }

        return sanitized;
    }

    private static String sanitizeItemId(String itemId) {
        if (itemId == null) {
            return null;
        }

        String trimmed = itemId.trim();
        if (trimmed.isEmpty() || Identifier.tryParse(trimmed) == null) {
            return null;
        }

        return trimmed;
    }

    public static List<String> sanitizeSlotNames(List<String> slots) {
        if (slots == null) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String slot : slots) {
            if (slot == null) {
                continue;
            }

            String trimmed = slot.trim();
            if (!trimmed.isEmpty()) {
                unique.add(trimmed);
            }
        }

        return new ArrayList<>(unique);
    }

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
