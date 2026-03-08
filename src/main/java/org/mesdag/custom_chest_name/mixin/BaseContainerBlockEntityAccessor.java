package org.mesdag.custom_chest_name.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BaseContainerBlockEntity.class)
public interface BaseContainerBlockEntityAccessor {
    @Invoker
    Component callGetDefaultName();
}
