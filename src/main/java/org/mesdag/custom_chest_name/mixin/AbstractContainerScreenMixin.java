package org.mesdag.custom_chest_name.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.mesdag.custom_chest_name.CustomChestNameClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    @Unique
    private boolean customChestName$searched;
    @Unique
    private EditBox customChestName$editBox;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void deny(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!customChestName$searched && customChestName$editBox == null) {
            this.customChestName$searched = true;
            for (Renderable renderable : renderables) {
                if (renderable instanceof EditBox editBox && CustomChestNameClient.RENAME_EDIT_BOX.equals(editBox.getMessage())) {
                    this.customChestName$editBox = editBox;
                    break;
                }
            }
        }
        if (customChestName$editBox != null) {
            if (customChestName$editBox.keyPressed(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
            } else if (customChestName$editBox.isFocused()) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    CustomChestNameClient.update(customChestName$editBox, title);
                    cir.setReturnValue(true); // 输入回车时自动更新
                } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                    cir.setReturnValue(true); // 防止输入e的时候关闭
                }
            }
        }
    }
}
