package com.ulasunoka.asc.client;

import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.client.MinecraftClient;

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
        return DEFAULT_SLOTS;
    }

    public static List<String> getApiSlots(MinecraftClient client) {
        if (client == null || client.world == null) {
            return List.of();
        }

        var slotTypes = SlotTypeLoader.getSlotTypes(client.world);
        if (slotTypes == null || slotTypes.isEmpty()) {
            return List.of();
        }

        return slotTypes.keySet().stream().sorted().toList();
    }
}
