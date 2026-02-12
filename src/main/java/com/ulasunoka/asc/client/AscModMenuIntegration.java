package com.ulasunoka.asc.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.ulasunoka.asc.config.AscConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.LowProfileButtonWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class AscModMenuIntegration implements ModMenuApi {

    private static final Logger LOGGER = LoggerFactory.getLogger("asc-integration");

    // Fallback list used when world is not loaded (Accessories API needs registry context)
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

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> buildMainScreen(parent);
    }

    private Screen buildMainScreen(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasWorld = client.world != null;

        return YetAnotherConfigLib.create(AscConfig.HANDLER, (defaults, config, builder) -> {
            builder.title(Text.of("ASC Config"));

            var categoryBuilder = ConfigCategory.createBuilder()
                    .name(Text.of("General"));

            // Warning if accessed from Main Menu
            if (!hasWorld) {
                categoryBuilder.option(dev.isxander.yacl3.api.LabelOption.create(
                        Text.of("§e⚠ WARNING: §rYou are not in a world!\nSlot suggestions are limited.")
                ));
            }

            // Dynamic list of SlotRule
            ListOption<AscConfig.SlotRule> rulesOption = ListOption.<AscConfig.SlotRule>createBuilder(AscConfig.SlotRule.class)
                    .name(Text.of("Rules"))
                    .description(OptionDescription.of(Text.of("Add, remove and edit slot rules dynamically.")))
                    .binding(
                            defaults.rules,
                            () -> config.rules,
                            v -> config.rules = v
                    )
                    .initial(AscConfig.SlotRule::new)
                    // Each list entry is rendered as a button which opens a dedicated editor screen
                    .customController(entry -> new SlotRuleEntryController(entry, parent, hasWorld))
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

    /**
     * Attempts to fetch available slot types from Accessories API.
     * Uses RegistryManager context which is required in 1.21.x.
     */
    private static String[] getAvailableSlots(MinecraftClient client) {
        try {
            if (client.world != null) {
                var slots = SlotTypeLoader.getSlotTypes(client.world);
                if (slots != null && !slots.isEmpty()) {
                    return slots.keySet().toArray(String[]::new);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[ASC] Failed to fetch slots from Accessories API", e);
        }
        return FALLBACK_SLOTS;
    }

    /**
     * Wraps a vanilla ButtonWidget into a YACL AbstractWidget.
     */
    private static final class ButtonWrapperWidget extends AbstractWidget {

        private final net.minecraft.client.gui.widget.ClickableWidget button;

        private ButtonWrapperWidget(Dimension<Integer> dim, net.minecraft.client.gui.widget.ClickableWidget button) {
            super(dim);
            this.button = button;
        }

        @Override
        public void setDimension(Dimension<Integer> dim) {
            super.setDimension(dim);
            // Keep underlying button in sync with YACL layout
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

    /**
     * Controller for ListOption entries of SlotRule.
     * Renders each rule as a clickable button and opens an editor screen.
     */
    private static final class SlotRuleEntryController implements dev.isxander.yacl3.api.Controller<AscConfig.SlotRule> {

        private final dev.isxander.yacl3.api.ListOptionEntry<AscConfig.SlotRule> entry;
        private final Screen parent;
        private final boolean hasWorld;

        private SlotRuleEntryController(dev.isxander.yacl3.api.ListOptionEntry<AscConfig.SlotRule> entry, Screen parent, boolean hasWorld) {
            this.entry = entry;
            this.parent = parent;
            this.hasWorld = hasWorld;
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
                    Text.literal("Edit: ").append(formatValue()),
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

            // Ensure we always edit the *pending* rule instance
            AscConfig.SlotRule rule = entry.pendingValue();
            if (rule == null) {
                rule = new AscConfig.SlotRule();
                entry.requestSet(rule);
            }

            final AscConfig.SlotRule workingRule = rule;

            return YetAnotherConfigLib.createBuilder()
                    .title(Text.of("Edit Rule"))
                    .category(ConfigCategory.createBuilder()
                            .name(Text.of("Rule"))
                            .option(dev.isxander.yacl3.api.ButtonOption.createBuilder()
                                    .name(Text.of("← Back to rules"))
                                    .action((screen, button) -> MinecraftClient.getInstance().setScreen(previousScreen))
                                    .build())
                            .option(Option.<String>createBuilder()
                                    .name(Text.of("Item ID"))
                                    .description(OptionDescription.of(Text.of("Example: minecraft:diamond")))

                                    .binding(
                                            "minecraft:stick",
                                            () -> workingRule.itemId,
                                            v -> {
                                                workingRule.itemId = v;
                                                entry.requestSet(workingRule);
                                            }
                                    )
                                    .controller(opt -> DropdownStringControllerBuilder.create(opt)
                                            .values(Registries.ITEM.getIds().stream().map(Identifier::toString).toList())
                                            .allowAnyValue(true)
                                            .allowEmptyValue(false)
                                    )
                                    .build()
                            )

                            .group(ListOption.<String>createBuilder(String.class)
                                    .name(Text.of("Target Slots"))
                                    .description(OptionDescription.of(
                                            hasWorld
                                                    ? Text.of("Available slots from loaded world")
                                                    : Text.of("⚠ Limited list (join world for full list)")
                                    ))
                                    .binding(
                                            List.of(),
                                            () -> workingRule.targetSlots,
                                            v -> {
                                                workingRule.targetSlots = v;
                                                entry.requestSet(workingRule);
                                            }
                                    )
                                    .initial(() -> "")
                                    .controller(opt -> DropdownStringControllerBuilder.create(opt)
                                            .values(Arrays.asList(getAvailableSlots(client)))
                                            .allowAnyValue(true)
                                            .allowEmptyValue(false)
                                    )
                                    .build()
                            )

                            .option(Option.<Boolean>createBuilder()
                                    .name(Text.of("Mode"))
                                    .description(OptionDescription.of(Text.of("Disabled = MERGE, Enabled = REPLACE")))
                                    .binding(
                                            false,
                                            () -> workingRule.mode == AscConfig.SlotRule.OperationMode.REPLACE,
                                            v -> {
                                                workingRule.mode = v
                                                        ? AscConfig.SlotRule.OperationMode.REPLACE
                                                        : AscConfig.SlotRule.OperationMode.MERGE;
                                                entry.requestSet(workingRule);
                                            }
                                    )
                                    .controller(BooleanControllerBuilder::create)
                                    .build()
                            )

                            .build()
                    )
                    .save(() -> {
                        // Save only the list entry pending value; actual config saving happens on the main screen "Done"
                        entry.requestSet(workingRule);
                    })
                    .build()
                    .generateScreen(previousScreen);
        }
    }
}
