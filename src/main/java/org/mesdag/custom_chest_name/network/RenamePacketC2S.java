package org.mesdag.custom_chest_name.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.mesdag.custom_chest_name.CustomChestName;

public record RenamePacketC2S(Component name) implements CustomPacketPayload {
    public static final Type<RenamePacketC2S> TYPE = new Type<>(CustomChestName.asResource("rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenamePacketC2S> STREAM_CODEC = ComponentSerialization.STREAM_CODEC
            .map(RenamePacketC2S::new, RenamePacketC2S::name);

    @Override
    public @NotNull Type<RenamePacketC2S> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ChestMenu chestMenu) {
                setName(chestMenu.getContainer());
            }
        });
    }

    private void setName(Container container) {
        if (container instanceof ChestBlockEntity blockEntity) {
            blockEntity.name = name;
            if (blockEntity.getLevel() instanceof ServerLevel level) {
                BlockPos pos = blockEntity.getBlockPos();
                PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new TellNamePacketS2C(pos, blockEntity.getDisplayName()));
            }
        } else if (container instanceof CompoundContainer compound) {
            setName(compound.container1);
            setName(compound.container2);
        }
    }
}
