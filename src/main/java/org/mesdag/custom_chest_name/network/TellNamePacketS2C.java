package org.mesdag.custom_chest_name.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.mesdag.custom_chest_name.CustomChestNameClient;

import java.util.function.Supplier;

public record TellNamePacketS2C(BlockPos pos, Component name) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeComponent(name);
    }

    public static TellNamePacketS2C decode(FriendlyByteBuf buffer) {
        return new TellNamePacketS2C(buffer.readBlockPos(), buffer.readComponent());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CustomChestNameClient.setName(pos, name)));
        context.get().setPacketHandled(true);
    }
}
