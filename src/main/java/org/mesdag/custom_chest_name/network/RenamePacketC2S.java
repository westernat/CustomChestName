package org.mesdag.custom_chest_name.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.custom_chest_name.CustomChestName;

import java.util.function.Supplier;

public record RenamePacketC2S(Component name) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeComponent(name);
    }

    public static RenamePacketC2S decode(FriendlyByteBuf buffer) {
        return new RenamePacketC2S(buffer.readComponent());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.containerMenu instanceof ChestMenu chestMenu) {
                setName(chestMenu.getContainer());
            }
        });
        context.get().setPacketHandled(true);
    }

    private void setName(Container container) {
        if (container instanceof ChestBlockEntity blockEntity) {
            blockEntity.name = name;
            if (blockEntity.getLevel() instanceof ServerLevel level) {
                BlockPos pos = blockEntity.getBlockPos();
                CustomChestName.CHANNEL.send(
                        PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                        new TellNamePacketS2C(pos, blockEntity.getDisplayName())
                );
            }
        } else if (container instanceof CompoundContainer compound) {
            setName(compound.container1);
            setName(compound.container2);
        }
    }
}
