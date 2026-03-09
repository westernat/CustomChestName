package org.mesdag.custom_chest_name;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mesdag.custom_chest_name.network.RenamePacketC2S;
import org.mesdag.custom_chest_name.network.RequestNamePacketC2S;
import org.mesdag.custom_chest_name.network.TellNamePacketS2C;

@Mod(CustomChestName.MODID)
@Mod.EventBusSubscriber(modid = CustomChestName.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomChestName {
    public static final String MODID = "custom_chest_name";
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            asResource("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public CustomChestName() {
        if (FMLEnvironment.dist.isClient()) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CustomChestNameClient.config());
        }
        CHANNEL.registerMessage(0, RenamePacketC2S.class, RenamePacketC2S::encode, RenamePacketC2S::decode, RenamePacketC2S::handle);
        CHANNEL.registerMessage(1, RequestNamePacketC2S.class, RequestNamePacketC2S::encode, RequestNamePacketC2S::decode, RequestNamePacketC2S::handle);
        CHANNEL.registerMessage(2, TellNamePacketS2C.class, TellNamePacketS2C::encode, TellNamePacketS2C::decode, TellNamePacketS2C::handle);
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
                blockEntity.name = stack.hasCustomHoverName() ? stack.getHoverName() : null;
                blockEntity.setChanged();
                Component name = RequestNamePacketC2S.getDisplayName(level, pos, blockEntity);
                CHANNEL.send(
                        PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                        new TellNamePacketS2C(pos, name)
                );
                player.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    }
}
