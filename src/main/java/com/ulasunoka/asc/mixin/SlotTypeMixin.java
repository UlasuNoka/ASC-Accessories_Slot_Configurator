package com.ulasunoka.asc.mixin;

import com.ulasunoka.asc.config.AscConfig;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// remap = false is used because Accessories is a library and method names might not be obfuscated in production.
// priority = 1100 ensures our logic runs before most other mixins.
@Mixin(value = AccessoriesAPI.class, remap = false, priority = 1100)
public class SlotTypeMixin {

    @Inject(
        method = "canInsertIntoSlot(Lnet/minecraft/item/ItemStack;Lio/wispforest/accessories/api/slot/SlotReference;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void overrideSlotInsertValidation(ItemStack stack, SlotReference reference, CallbackInfoReturnable<Boolean> cir) {
        try {
            // 1. Fast lookup via Item ID
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();

            // 2. Check cache (O(1) complexity)
            AscConfig.SlotRule rule = AscConfig.RULE_CACHE.get(itemId);

            if (rule == null) return; // No rule -> vanilla behavior

            if (rule.disableEquipping) {
                cir.setReturnValue(false);
                return;
            }

            // 3. Get current slot name being checked
            String currentSlot = reference.slotName();

            // 4. Apply Logic
            boolean isTargetSlot = rule.targetSlots != null && rule.targetSlots.contains(currentSlot);

            if (isTargetSlot) {
                // User explicitly allowed this slot
                cir.setReturnValue(true);
            } else if (rule.mode == AscConfig.SlotRule.OperationMode.REPLACE) {
                // User wants ONLY specific slots, so block everything else
                cir.setReturnValue(false);
            }
            // If MERGE mode and not target slot -> let vanilla logic decide

        } catch (Exception e) {
            // Fail silently to prevent crashing the game during inventory operations
            // System.err.println("[ASC] Error in slot insert validation: " + e.getMessage());
        }
    }
}