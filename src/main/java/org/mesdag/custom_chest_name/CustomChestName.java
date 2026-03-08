package org.mesdag.custom_chest_name;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
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
    public static void r(PlayerInteractEvent.RightClickBlock event) {

    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    }
}
