package org.mesdag.custom_chest_name;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
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

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
