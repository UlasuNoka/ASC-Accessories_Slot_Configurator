package com.ulasunoka.asc.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator;
import me.fzzyhmstrs.fzzy_config.util.AllowableStrings;
import me.fzzyhmstrs.fzzy_config.util.ValidationResult;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class AscConfig extends Config {

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

    private static final List<String> FALLBACK_SLOT_LIST = List.of(FALLBACK_SLOTS);

    private static AscConfig INSTANCE;

    public static final Map<String, SlotRuleData> RULE_CACHE = new HashMap<>();

    public ValidatedList<SlotRule> rules = new ValidatedList<>(new ArrayList<>(), new RuleEntryValidation());

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

        public ValidatedList<String> targetSlots = new ValidatedList<>(
                new ArrayList<>(List.of("head")),
                new ValidatedString("head", new SlotStringChecker(this))
        );

        public ValidatedEnum<OperationMode> mode = new ValidatedEnum<>(OperationMode.MERGE, ValidatedEnum.WidgetType.CYCLING);

        public AdvancedSettings advancedSettings = new AdvancedSettings();

        public SlotRuleData export() {
            List<String> slots = targetSlots.get().stream()
                    .filter(slot -> slot != null && !slot.isBlank())
                    .toList();

            return new SlotRuleData(
                    itemId.get().toString(),
                    slots,
                    mode.get() == null ? OperationMode.MERGE : mode.get()
            );
        }
    }

    public static class AdvancedSettings extends ConfigSection {
        public ValidatedBoolean allowCustomTargetSlots = new ValidatedBoolean(false);
    }

    private static class SlotStringChecker extends AllowableStrings {

        private final SlotRule owner;

        private SlotStringChecker(SlotRule owner) {
            super(new Predicate<>() {
                @Override
                public boolean test(String value) {
                    if (owner.advancedSettings.allowCustomTargetSlots.get()) {
                        return true;
                    }
                    return FALLBACK_SLOT_LIST.contains(value);
                }
            }, () -> FALLBACK_SLOT_LIST);
            this.owner = owner;
        }

        @Override
        public ValidationResult<String> validateEntry(String input, EntryValidator.ValidationType type) {
            if (owner.advancedSettings.allowCustomTargetSlots.get()) {
                return ValidationResult.success(input);
            }
            return super.validateEntry(input, type);
        }

        @Override
        public ValidationResult<String> correctEntry(String input, EntryValidator.ValidationType type) {
            if (owner.advancedSettings.allowCustomTargetSlots.get()) {
                return ValidationResult.success(input);
            }
            return super.correctEntry(input, type);
        }
    }

    private static class RuleEntryValidation extends ValidatedAny<SlotRule> {

        private RuleEntryValidation() {
            super(new SlotRule());
        }

        @Override
        protected MutableText provideTranslation(String fallback) {
            SlotRule rule = get();
            Identifier id = rule.itemId.get();
            Item item = Registries.ITEM.get(id);
            String technicalId = id.toString();
            String localizedName = item.getName().getString();
            return Text.literal(technicalId + " (" + localizedName + ")");
        }

    }

    public record SlotRuleData(String itemId, List<String> targetSlots, OperationMode mode) {}

    public enum OperationMode {
        MERGE,
        REPLACE
    }
}
