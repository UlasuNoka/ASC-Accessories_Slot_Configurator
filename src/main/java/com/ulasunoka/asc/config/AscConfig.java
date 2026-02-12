package com.ulasunoka.asc.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Config(name = "asc")
public class AscConfig implements ConfigData {

    // Main storage for config rules. Saved to disk.
    @ConfigEntry.Gui.CollapsibleObject
    public List<SlotRule> rules = new ArrayList<>();

    // Runtime cache for O(1) lookups during game tick/rendering.
    // NOT saved to disk. Must be rebuilt manually via updateCache().
    @ConfigEntry.Gui.Excluded
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

    // Helper to fetch instance from AutoConfig holder
    public static AscConfig get() {
        return AutoConfig.getConfigHolder(AscConfig.class).getConfig();
    }
}