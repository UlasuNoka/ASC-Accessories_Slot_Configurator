package com.ulasunoka.asc.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext;
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated;
import me.fzzyhmstrs.fzzy_config.util.AllowableStrings;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.util.ValidationResult;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AscConfig extends Config {

    private static final String[] SLOT_LIST = {
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

    private static final List<String> SLOT_VALUES = List.of(SLOT_LIST);

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

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        updateCache();
    }

    @Override
    public void onSyncClient() {
        super.onSyncClient();
        updateCache();
    }

    @Override
    public void onUpdateServer(ServerUpdateContext context) {
        super.onUpdateServer(context);
        updateCache();
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

    public static class SlotRule extends ConfigSection {

        public ValidatedField<Item> item = ValidatedRegistryType.of(Items.STICK, Registries.ITEM);

        public AdvancedSettings advancedSettings = new AdvancedSettings();

        public ValidatedList<String> targetSlots = new ValidatedList<>(
                new ArrayList<>(List.of("head")),
                new DynamicSlotString(this)
        );

        public ValidatedEnum<OperationMode> mode = new ValidatedEnum<>(OperationMode.MERGE, ValidatedEnum.WidgetType.CYCLING);

        public SlotRuleData export() {
            List<String> slots = new ArrayList<>();
            for (String slot : targetSlots.get()) {
                if (slot != null && !slot.isBlank()) {
                    slots.add(slot);
                }
            }

            return new SlotRuleData(
                    Registries.ITEM.getId(item.get()).toString(),
                    slots,
                    mode.get() == null ? OperationMode.MERGE : mode.get()
            );
        }
    }

    public static class AdvancedSettings extends ConfigSection {

        public ValidatedBoolean allowCustomTargetSlots = new ValidatedBoolean(false);

    }

    private static class DynamicSlotString extends ValidatedString {

        private final SlotRule owner;

        private DynamicSlotString(SlotRule owner) {
            super("head", new AllowableStrings(SLOT_VALUES::contains, () -> SLOT_VALUES));
            this.owner = owner;
        }

        @Override
        public ValidationResult<String> validateEntry(String input, me.fzzyhmstrs.fzzy_config.entry.EntryValidator.ValidationType type) {
            if (owner.advancedSettings != null && owner.advancedSettings.allowCustomTargetSlots.get()) {
                return ValidationResult.Companion.success(input);
            }
            return super.validateEntry(input, type);
        }

        @Override
        public ValidationResult<String> correctEntry(String input, me.fzzyhmstrs.fzzy_config.entry.EntryValidator.ValidationType type) {
            if (owner.advancedSettings != null && owner.advancedSettings.allowCustomTargetSlots.get()) {
                return ValidationResult.Companion.success(input);
            }
            return super.correctEntry(input, type);
        }
    }

    private static class RuleEntryValidation extends ValidatedAny<SlotRule> {

        private RuleEntryValidation() {
            super(new SlotRule());

            ValidatedField.Companion.attachProvider(
                    this,
                    Translatable.Provider.Companion.getWIDGET_TITLE(),
                    (rule, fallback) -> formatRuleSummary(rule)
            );
        }

        @Override
        public MutableText provideTranslation(String fallback) {
            return formatRuleSummary(get());
        }

        @Override
        public Decorated.DecoratedOffset entryDeco() {
            return new Decorated.DecoratedOffset((context, x, y, delta, enabled, selected) -> {
                SlotRule rule = get();
                ItemStack stack = new ItemStack(rule.item.get());
                if (!stack.isEmpty()) {
                    context.drawItem(stack, x, y);
                }
            }, 2, 2);
        }

        private static MutableText formatRuleSummary(SlotRule rule) {
            Item item = rule.item.get();
            Identifier id = Registries.ITEM.getId(item);
            String localizedName = item.getName().getString();
            return Text.literal(id + " (" + localizedName + ")");
        }
    }

    public record SlotRuleData(String itemId, List<String> targetSlots, OperationMode mode) {}

    public enum OperationMode {
        MERGE,
        REPLACE
    }
}
