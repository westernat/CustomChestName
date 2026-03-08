package org.mesdag.custom_chest_name.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.mesdag.custom_chest_name.CustomChestName;
import org.mesdag.custom_chest_name.mixin.BaseContainerBlockEntityAccessor;

public record RequestNamePacketC2S(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RequestNamePacketC2S> TYPE = new Type<>(CustomChestName.asResource("request_name"));
    public static final StreamCodec<ByteBuf, RequestNamePacketC2S> STREAM_CODEC = BlockPos.STREAM_CODEC
            .map(RequestNamePacketC2S::new, RequestNamePacketC2S::pos);

    @Override
    public @NotNull Type<RequestNamePacketC2S> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity blockEntity) {
                    PacketDistributor.sendToPlayer(player, new TellNamePacketS2C(pos, getDisplayName(level, pos, blockEntity)));
                }
            }
        });
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
