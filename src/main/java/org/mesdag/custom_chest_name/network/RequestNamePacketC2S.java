package org.mesdag.custom_chest_name.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.custom_chest_name.CustomChestName;
import org.mesdag.custom_chest_name.mixin.BaseContainerBlockEntityAccessor;

import java.util.function.Supplier;

public record RequestNamePacketC2S(BlockPos pos) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    public static RequestNamePacketC2S decode(FriendlyByteBuf buffer) {
        return new RequestNamePacketC2S(buffer.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                Level level = player.level();
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity blockEntity) {
                    CustomChestName.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new TellNamePacketS2C(pos, getDisplayName(level, pos, blockEntity))
                    );
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static Component getDisplayName(Level level, BlockPos pos, ChestBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (!blockEntity.hasCustomName() && state.getBlock() instanceof ChestBlock) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                BlockPos relative = pos.relative(ChestBlock.getConnectedDirection(state));
                if (level.getBlockEntity(relative) instanceof ChestBlockEntity blockEntity1 && blockEntity1.getBlockState().is(state.getBlock())) {
                    if (blockEntity1.hasCustomName()) {
                        return blockEntity1.getDisplayName();
                    } else {
                        return Component.translatable("container.chestDouble");
                    }
                }
            }
        }
        return blockEntity.getDisplayName();
    }

    public static Component getDefaultName(Level level, BlockPos pos, ChestBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                BlockPos relative = pos.relative(ChestBlock.getConnectedDirection(state));
                if (level.getBlockState(relative).is(state.getBlock())) {
                    return Component.translatable("container.chestDouble");
                }
            }
        }
        return ((BaseContainerBlockEntityAccessor) blockEntity).callGetDefaultName();
    }
}
