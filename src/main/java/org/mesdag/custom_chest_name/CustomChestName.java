package org.mesdag.custom_chest_name;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.mesdag.custom_chest_name.network.RenamePacketC2S;
import org.mesdag.custom_chest_name.network.RequestNamePacketC2S;
import org.mesdag.custom_chest_name.network.TellNamePacketS2C;

@Mod(CustomChestName.MODID)
@EventBusSubscriber(modid = CustomChestName.MODID)
public class CustomChestName {
    public static final String MODID = "custom_chest_name";

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(RenamePacketC2S.TYPE, RenamePacketC2S.STREAM_CODEC, RenamePacketC2S::handle)
                .playToServer(RequestNamePacketC2S.TYPE, RequestNamePacketC2S.STREAM_CODEC, RequestNamePacketC2S::handle)
                .playToClient(TellNamePacketS2C.TYPE, TellNamePacketS2C.STREAM_CODEC, TellNamePacketS2C::handle);
    }

    @SubscribeEvent
    public static void playerInteract$RightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (event.getLevel() instanceof ServerLevel level &&
                player.isCrouching() &&
                stack.is(Items.NAME_TAG) &&
                event.getHand() == InteractionHand.MAIN_HAND
        ) {
            BlockPos pos = event.getHitVec().getBlockPos();
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity blockEntity) {
                blockEntity.name = stack.get(DataComponents.CUSTOM_NAME);
                blockEntity.setChanged();
                Component name = RequestNamePacketC2S.getDisplayName(level, pos, blockEntity);
                PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new TellNamePacketS2C(pos, name));
                player.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
