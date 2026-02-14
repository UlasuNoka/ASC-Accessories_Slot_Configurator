package com.ulasunoka.asc.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.ulasunoka.asc.config.AscConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.ItemControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.LowProfileButtonWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AscModMenuIntegration implements ModMenuApi {

    private static final Logger LOGGER = LoggerFactory.getLogger("asc-integration");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::buildMainScreen;
    }

    private Screen buildMainScreen(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasWorld = client.world != null;

        return YetAnotherConfigLib.create(AscConfig.HANDLER, (defaults, config, builder) -> {
            builder.title(Text.of("ASC Config"));

            var categoryBuilder = ConfigCategory.createBuilder()
                    .name(Text.of("General"));

            if (!hasWorld) {
                categoryBuilder.option(dev.isxander.yacl3.api.LabelOption.create(
                        Text.of("§e⚠ WARNING: §rYou are not in a world!\nSlot suggestions are limited.")
                ));
            }

            ListOption<AscConfig.SlotRule> rulesOption = ListOption.<AscConfig.SlotRule>createBuilder()
                    .name(Text.of("Rules"))
                    .description(OptionDescription.of(Text.of("Add, remove and edit slot rules dynamically.")))
                    .binding(defaults.rules, () -> config.rules, v -> config.rules = v)
                    .initial(AscConfig.SlotRule::new)
                    .customController(SlotRuleEntryController::new)
                    .build();

            categoryBuilder.group(rulesOption);
            builder.category(categoryBuilder.build());

            builder.save(() -> {
                AscConfig.save();
                AscConfig.updateCache();
                LOGGER.info("[ASC] Config saved and cache updated");
            });

            return builder;
        }).generateScreen(parent);
    }

    private static final class ButtonWrapperWidget extends AbstractWidget {

        private final net.minecraft.client.gui.widget.ClickableWidget button;

        private ButtonWrapperWidget(Dimension<Integer> dim, net.minecraft.client.gui.widget.ClickableWidget button) {
            super(dim);
            this.button = button;
        }

        @Override
        public void setDimension(Dimension<Integer> dim) {
            super.setDimension(dim);
            this.button.setX(dim.x());
            this.button.setY(dim.y());
            this.button.setWidth(dim.width());
            this.button.setHeight(dim.height());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.button.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return this.button.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean onMouseReleased(double mouseX, double mouseY, int button) {
            return this.button.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            return this.button.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean onCharTyped(char chr, String key, int modifiers) {
            return this.button.charTyped(chr, modifiers);
        }

        @Override
        public boolean isFocused() {
            return this.button.isFocused();
        }

        @Override
        public void setFocused(boolean focused) {
            this.button.setFocused(focused);
        }
    }

    private static final class IconButtonWrapperWidget extends AbstractWidget {

        private final net.minecraft.client.gui.widget.ClickableWidget button;
        private final java.util.function.Supplier<ItemStack> iconSupplier;
        private Dimension<Integer> currentDimension;

        private IconButtonWrapperWidget(Dimension<Integer> dim,
                                        net.minecraft.client.gui.widget.ClickableWidget button,
                                        java.util.function.Supplier<ItemStack> iconSupplier) {
            super(dim);
            this.button = button;
            this.iconSupplier = iconSupplier;
            this.currentDimension = dim;
        }

        @Override
        public void setDimension(Dimension<Integer> dim) {
            super.setDimension(dim);
            this.currentDimension = dim;
            this.button.setX(dim.x());
            this.button.setY(dim.y());
            this.button.setWidth(dim.width());
            this.button.setHeight(dim.height());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.button.render(context, mouseX, mouseY, delta);

            ItemStack stack = iconSupplier.get();
            if (!stack.isEmpty()) {
                int iconX = this.currentDimension.x() + 6;
                int iconY = this.currentDimension.y() + (this.currentDimension.height() - 16) / 2;
                context.drawItem(stack, iconX, iconY);
            }
        }

        @Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return this.button.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean onMouseReleased(double mouseX, double mouseY, int button) {
            return this.button.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            return this.button.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean onCharTyped(char chr, String key, int modifiers) {
            return this.button.charTyped(chr, modifiers);
        }

        @Override
        public boolean isFocused() {
            return this.button.isFocused();
        }

        @Override
        public void setFocused(boolean focused) {
            this.button.setFocused(focused);
        }
    }

    private static final class SlotRuleEntryController implements dev.isxander.yacl3.api.Controller<AscConfig.SlotRule> {

        private final dev.isxander.yacl3.api.ListOptionEntry<AscConfig.SlotRule> entry;

        private SlotRuleEntryController(dev.isxander.yacl3.api.ListOptionEntry<AscConfig.SlotRule> entry) {
            this.entry = entry;
        }

        @Override
        public Option<AscConfig.SlotRule> option() {
            return entry;
        }

        @Override
        public Text formatValue() {
            AscConfig.SlotRule rule = entry.pendingValue();
            if (rule == null || rule.itemId == null || rule.itemId.isEmpty()) return Text.of("New Rule");

            String itemId = rule.itemId;
            return resolveItemName(itemId)
                    .<Text>map(name -> Text.literal(itemId + " (").append(name).append(")"))
                    .orElse(Text.of(itemId));
        }

        @Override
        public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dim) {
            LowProfileButtonWidget button = new LowProfileButtonWidget(
                    dim.x(), dim.y(), dim.width(), dim.height(),
                    formatValue(),
                    btn -> MinecraftClient.getInstance().setScreen(buildRuleEditorScreen(screen))
            );
            return new IconButtonWrapperWidget(dim, button, this::resolveEntryItemStack);
        }

        private ItemStack resolveEntryItemStack() {
            AscConfig.SlotRule rule = entry.pendingValue();
            if (rule == null || rule.itemId == null || rule.itemId.isBlank()) {
                return ItemStack.EMPTY;
            }

            return parseIdentifier(rule.itemId)
                    .flatMap(id -> Registries.ITEM.getOrEmpty(id))
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        }

        private Optional<Text> resolveItemName(String itemId) {
            if (itemId == null || itemId.isBlank()) {
                return Optional.empty();
            }

            return parseIdentifier(itemId)
                    .flatMap(id -> Registries.ITEM.getOrEmpty(id))
                    .map(Item::getName);
        }

        private Optional<Identifier> parseIdentifier(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            return Optional.ofNullable(Identifier.tryParse(value));
        }

        private Screen buildRuleEditorScreen(Screen previousScreen) {
            MinecraftClient client = MinecraftClient.getInstance();
            AscConfig.SlotRule rule = entry.pendingValue();
            if (rule == null) {
                rule = new AscConfig.SlotRule();
                entry.requestSet(rule);
            }

            final AscConfig.SlotRule workingRule = rule;
            if (workingRule.mode == null) {
                workingRule.mode = AscConfig.SlotRule.OperationMode.MERGE;
            }
            if (workingRule.targetSlots == null) {
                workingRule.targetSlots = new ArrayList<>();
            }

            boolean hasWorld = client.world != null;
            boolean allowCustomInput = AscConfig.get().allowCustomSlots && hasWorld;
            List<String> slotSuggestions = allowCustomInput ? AccessoriesDataHelper.getApiSlots(client) : AccessoriesDataHelper.getSafeSlots();

            return YetAnotherConfigLib.createBuilder()
                    .title(Text.of("Edit Rule"))
                    .category(ConfigCategory.createBuilder()
                            .name(Text.of("Rule"))
                            .option(Option.<Item>createBuilder(Item.class)
                                    .name(Text.of("Item"))
                                    .description(OptionDescription.of(Text.of("Select an item (icon + localized name)")))
                                    .binding(
                                            Items.STICK,
                                            () -> parseIdentifier(workingRule.itemId)
                                                    .flatMap(id -> Registries.ITEM.getOrEmpty(id))
                                                    .orElse(Items.STICK),
                                            v -> {
                                                workingRule.itemId = Registries.ITEM.getId(v).toString();
                                                entry.requestSet(workingRule);
                                            }
                                    )
                                    .controller(ItemControllerBuilder::create)
                                    .build()
                            )
                            .group(ListOption.<String>createBuilder()
                                    .name(Text.of("Target Slots"))
                                    .description(OptionDescription.of(
                                            allowCustomInput
                                                    ? Text.of("Suggestions come from Accessories API only. Custom non-empty values are allowed.")
                                                    : Text.of("Safe slots only. Limited list (join world for full list).")
                                    ))
                                    .binding(
                                            List.of(),
                                            () -> new ArrayList<>(AscConfig.sanitizeSlotNames(workingRule.targetSlots)),
                                            v -> {
                                                List<String> sanitized = AscConfig.sanitizeSlotNames(v);
                                                if (!allowCustomInput) {
                                                    sanitized = sanitized.stream().filter(AccessoriesDataHelper.getSafeSlots()::contains).toList();
                                                }
                                                workingRule.targetSlots = new ArrayList<>(sanitized);
                                                entry.requestSet(workingRule);
                                            }
                                    )
                                    .initial(allowCustomInput ? "head" : AccessoriesDataHelper.getSafeSlots().getFirst())
                                    .available(!workingRule.disableEquipping)
                                    .controller(opt -> DropdownStringControllerBuilder.create(opt)
                                            .values(slotSuggestions)
                                            .allowAnyValue(allowCustomInput)
                                            .allowEmptyValue(false)
                                    )
                                    .build())
                            .option(Option.<AscConfig.SlotRule.OperationMode>createBuilder(AscConfig.SlotRule.OperationMode.class)
                                    .name(Text.of("Mode"))
                                    .description(OptionDescription.of(Text.of("Cycle between MERGE and REPLACE")))
                                    .binding(
                                            AscConfig.SlotRule.OperationMode.MERGE,
                                            () -> workingRule.mode == null
                                                    ? AscConfig.SlotRule.OperationMode.MERGE
                                                    : workingRule.mode,
                                            v -> {
                                                workingRule.mode = v;
                                                entry.requestSet(workingRule);
                                            }
                                    )
                                    .available(!workingRule.disableEquipping)
                                    .controller(opt -> CyclingListControllerBuilder.create(opt)
                                            .values(List.of(
                                                    AscConfig.SlotRule.OperationMode.MERGE,
                                                    AscConfig.SlotRule.OperationMode.REPLACE
                                            ))
                                            .valueFormatter(mode -> Text.literal(mode.name())))
                                    .build()
                            )
                            .group(OptionGroup.createBuilder()
                                    .name(Text.of("Advanced Settings"))
                                    .description(OptionDescription.of(Text.of("Advanced controls for slot sources and force blocking equip.")))
                                    .collapsed(true)
                                    .option(Option.<Boolean>createBuilder()
                                            .name(Text.of("Allow custom target slots (Advanced Mode)"))
                                            .description(OptionDescription.of(Text.of("When enabled in-world, target slot suggestions use Accessories API only and typing any non-empty value is allowed.")))
                                            .binding(
                                                    false,
                                                    () -> AscConfig.get().allowCustomSlots,
                                                    v -> {
                                                        AscConfig.get().allowCustomSlots = v;
                                                        AscConfig.save();
                                                        client.setScreen(buildRuleEditorScreen(previousScreen));
                                                    }
                                            )
                                            .available(hasWorld)
                                            .instant(true)
                                            .controller(TickBoxControllerBuilder::create)
                                            .build())
                                    .option(Option.<Boolean>createBuilder()
                                            .name(Text.of("Disable equipping"))
                                            .description(OptionDescription.of(Text.of("When enabled, this item cannot be equipped into any slot.\nExisting Target Slots and Mode are preserved and will be restored when disabled.")))
                                            .binding(
                                                    false,
                                                    () -> workingRule.disableEquipping,
                                                    v -> {
                                                        workingRule.disableEquipping = v;
                                                        entry.requestSet(workingRule);
                                                        client.setScreen(buildRuleEditorScreen(previousScreen));
                                                    }
                                            )
                                            .instant(true)
                                            .controller(TickBoxControllerBuilder::create)
                                            .build())
                                    .build())
                            .build())
                    .save(() -> entry.requestSet(workingRule))
                    .build()
                    .generateScreen(previousScreen);
        }
    }
}
