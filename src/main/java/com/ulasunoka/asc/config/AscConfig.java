package com.ulasunoka.asc.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AscConfig extends Config {

    public static final String CUSTOM_SLOT_VALUE = "★ Custom...";

    private static final String[] FALLBACK_SLOTS = {
            "head",
            "necklace",
            "back",
            "body",
            "charm",
            "ring",
            "hands",
            "belt",
            "legs",
            "feet"
    };

    private static AscConfig INSTANCE;

    public static final Map<String, SlotRuleData> RULE_CACHE = new HashMap<>();

    public ValidatedList<SlotRule> rules = new ValidatedList<>(new ArrayList<>(), new ValidatedAny<>(new SlotRule()));

    public AscConfig() {
        super(Identifier.of("asc", "config"), "", "", "asc");
    }

    public static void load() {
        INSTANCE = ConfigApiJava.registerAndLoadConfig(AscConfig::new, RegisterType.BOTH);
    }

    public static AscConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void updateCache() {
        RULE_CACHE.clear();

        List<? extends SlotRule> configuredRules = get().rules.get();
        if (configuredRules == null) {
            return;
        }

        for (SlotRule rule : configuredRules) {
            SlotRuleData exported = rule.export();
            RULE_CACHE.put(exported.itemId(), exported);
        }
    }

    public static class SlotRule {

        public ValidatedIdentifier itemId = ValidatedIdentifier.ofRegistry(Identifier.of("minecraft", "stick"), Registries.ITEM);

        public ValidatedString targetSlot = ValidatedString.fromList(getTargetChoices().getFirst(), getTargetChoices());

        public ValidatedString customTargetSlot = new ValidatedString("");

        public ValidatedEnum<OperationMode> mode = new ValidatedEnum<>(OperationMode.MERGE);

        private static List<String> getTargetChoices() {
            List<String> slots = new ArrayList<>(List.of(FALLBACK_SLOTS));
            slots.add(CUSTOM_SLOT_VALUE);
            return slots;
        }

        public SlotRuleData export() {
            String chosenTarget = targetSlot.get();
            String customValue = customTargetSlot.get();

            String resolvedSlot = CUSTOM_SLOT_VALUE.equals(chosenTarget)
                    ? customValue
                    : chosenTarget;

            List<String> targetSlots = resolvedSlot == null || resolvedSlot.isBlank()
                    ? List.of()
                    : List.of(resolvedSlot);

            return new SlotRuleData(
                    itemId.get().toString(),
                    targetSlots,
                    mode.get() == null ? OperationMode.MERGE : mode.get()
            );
        }
    }

    public record SlotRuleData(String itemId, List<String> targetSlots, OperationMode mode) {}

    public enum OperationMode {
        MERGE,
        REPLACE
    }
}
