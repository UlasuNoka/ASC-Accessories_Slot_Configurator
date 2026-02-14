package com.ulasunoka.asc.mixin;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.controllers.dropdown.DropdownWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DropdownWidget.class, remap = false)
public abstract class DropdownWidgetMixin {

    @Shadow protected Dimension<Integer> dropdownDim;

    @Shadow public abstract int entryHeight();

    @Shadow public abstract void selectVisibleItem(int visibleIndex);

    @Inject(method = "onMouseClicked", at = @At("HEAD"))
    private void asc$selectEntryBeforeUnfocus(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.dropdownDim != null && this.dropdownDim.isPointInside((int) mouseX, (int) mouseY)) {
            int index = (int) ((mouseY - this.dropdownDim.y()) / Math.max(1, this.entryHeight()));
            this.selectVisibleItem(index);
        }
    }
}
