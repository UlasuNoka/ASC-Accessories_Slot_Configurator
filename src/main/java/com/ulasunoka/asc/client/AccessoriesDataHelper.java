package com.ulasunoka.asc.client;

import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public final class AccessoriesDataHelper {

    public static final List<String> DEFAULT_SLOTS = List.of(
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
    );

    private AccessoriesDataHelper() {
    }

    public static List<String> getSafeSlots() {
        return new ArrayList<>(DEFAULT_SLOTS);
    }

    public static List<String> getApiSlots(MinecraftClient client) {
        if (client == null || client.world == null) {
            return List.of();
        }

        try {
            var slots = SlotTypeLoader.getSlotTypes(client.world);
            if (slots == null || slots.isEmpty()) {
                return List.of();
            }

            return slots.keySet().stream().sorted().toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
