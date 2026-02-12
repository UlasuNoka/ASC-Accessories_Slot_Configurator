package com.ulasunoka.asc.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.ulasunoka.asc.config.AscConfig;
import io.wispforest.accessories.api.AccessoriesAPI;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
        return parent -> {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean hasWorld = client.world != null;

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.of("ASC Config"));

            AscConfig config = AscConfig.get();
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            var general = builder.getOrCreateCategory(Text.of("General"));

            // Warning if accessed from Main Menu
            if (!hasWorld) {
                general.addEntry(entryBuilder.startTextDescription(
                    Text.of("§e⚠ WARNING: §rYou are not in a world!\nSlot suggestions are limited.")
                ).build());
            }

            // Explicitly cast to help compiler match the method signature in Cloth Config 15
            List<AscConfig.SlotRule> ruleList = config.rules;

            // List of Rules using startObjectList for proper object handling
            general.addEntry(entryBuilder.startObjectList(Text.of("Rules"), ruleList, new AscConfig.SlotRule())
                .setExpanded(true)
                .setRenderer((rule, rulesListEntry) -> {
                    var innerEntries = new ArrayList<me.shedaniel.clothconfig2.api.AbstractConfigListEntry>();

                    // 1. Item ID (Registry Autocomplete)
                    innerEntries.add(entryBuilder.startStrField(Text.of("Item ID"), rule.itemId)
                        .setDefaultValue("minecraft:stick")
                        .setTooltip(Text.of("§7Example: minecraft:diamond"))
                        .setSuggestionProvider(() -> 
                            Registries.ITEM.getIds().stream()
                                .map(Identifier::toString)
                                .toArray(String[]::new)
                        )
                        .setSaveConsumer(s -> rule.itemId = s)
                        .build());

                    // 2. Target Slots (API Autocomplete)
                    innerEntries.add(entryBuilder.startStrList(Text.of("Target Slots"), rule.targetSlots)
                        .setTooltip(hasWorld ? 
                            Text.of("§7Available slots from loaded world") : 
                            Text.of("§e⚠ Limited list (join world for full list)")
                        )
                        .setSuggestionProvider(() -> getAvailableSlots(client))
                        .setSaveConsumer(l -> rule.targetSlots = l)
                        .build());

                    // 3. Operation Mode
                    innerEntries.add(entryBuilder.startEnumSelector(
                            Text.of("Mode"), 
                            AscConfig.SlotRule.OperationMode.class, 
                            rule.mode
                        )
                        .setTooltip(Text.of("§7MERGE: Adds slots\n§7REPLACE: Overrides slots"))
                        .setSaveConsumer(m -> rule.mode = m)
                        .build());

                    // Return collapsible sub-category for better UX
                    return entryBuilder.startSubCategory(
                        Text.literal(rule.itemId.isEmpty() ? "§7New Rule" : rule.itemId),
                        innerEntries
                    ).build();
                })
                .build());

            return builder.setSavingRunnable(() -> {
                AutoConfig.getConfigHolder(AscConfig.class).save();
                AscConfig.updateCache(); // Update runtime map immediately
                LOGGER.info("[ASC] Config saved and cache updated");
            }).build();
        };
    }

    /**
     * Attempts to fetch available slot types from Accessories API.
     * Uses RegistryManager context which is required in 1.21.x.
     */
    private String[] getAvailableSlots(MinecraftClient client) {
        try {
            if (client.world != null) {
                // Fixed: Accessories 1.21.1 requires RegistryManager
                var slots = AccessoriesAPI.getSlotDefinitions(client.world.getRegistryManager());
                if (slots != null && !slots.isEmpty()) {
                    return slots.keySet().toArray(String[]::new);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[ASC] Failed to fetch slots from Accessories API", e);
        }
        return FALLBACK_SLOTS;
    }
}