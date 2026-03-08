package org.mesdag.custom_chest_name.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.mesdag.custom_chest_name.CustomChestName;
import org.mesdag.custom_chest_name.CustomChestNameClient;

public record TellNamePacketS2C(BlockPos pos, Component name) implements CustomPacketPayload {
    public static final Type<TellNamePacketS2C> TYPE = new Type<>(CustomChestName.asResource("tell_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TellNamePacketS2C> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TellNamePacketS2C::pos,
            ComponentSerialization.STREAM_CODEC, TellNamePacketS2C::name,
            TellNamePacketS2C::new
    );

    @Override
    public @NotNull Type<TellNamePacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> CustomChestNameClient.setName(pos, name));
    }
}
