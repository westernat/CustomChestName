package org.mesdag.custom_chest_name;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.mesdag.custom_chest_name.integration.JadeHelper;
import org.mesdag.custom_chest_name.network.RenamePacketC2S;
import org.mesdag.custom_chest_name.network.RequestNamePacketC2S;

import java.util.Locale;

@Mod(value = CustomChestName.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CustomChestName.MODID, value = Dist.CLIENT)
public class CustomChestNameClient {
    public static final Component RENAME_EDIT_BOX = Component.literal("RenameEditBox");
    public static final boolean IS_JADE_LOADED = LoadingModList.get().getModFileById("jade") != null;
    private static Component name;
    private static BlockPos lastPos;

    private static ModConfigSpec.EnumValue<DisplayMode> displayMode;

    public CustomChestNameClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, config());
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static ModConfigSpec config() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        displayMode = builder.defineEnum("displayMode", DisplayMode.NOT_DEFAULT);
        return builder.build();
    }

    public static void setName(BlockPos pos, Component name) {
        if (pos.equals(lastPos)) {
            CustomChestNameClient.name = name;
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(CustomChestName.asResource("chest_name"), (guiGraphics, deltaTracker) -> {
            if (IS_JADE_LOADED && JadeHelper.shouldDisplayTooltip()) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui) {
                lastPos = null;
                name = null;
                return;
            }
            HitResult hitResult = minecraft.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = BlockPos.containing(hitResult.getLocation());
                if (minecraft.level.getBlockEntity(blockPos) instanceof ChestBlockEntity blockEntity) {
                    if (name == null || !blockPos.equals(lastPos)) {
                        name = null;
                        lastPos = blockPos;
                        PacketDistributor.sendToServer(new RequestNamePacketC2S(blockPos));
                    }
                    if (name != null && displayMode.get().shouldDisplay(blockEntity)) {
                        Font font = minecraft.font;
                        int x = guiGraphics.guiWidth() / 2 + 10;
                        int y = guiGraphics.guiHeight() / 2 + 7;
                        guiGraphics.renderTooltip(font, name, x, y);
                    }
                }
            } else {
                name = null;
            }
        });
    }

    @SubscribeEvent
    public static void screen$Init$Post(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof ContainerScreen screen) {
            Font font = Minecraft.getInstance().font;
            Component title = screen.getTitle();
            EditBox editBox = new EditBox(
                    font,
                    screen.getGuiLeft() + screen.titleLabelX,
                    screen.getGuiTop() + screen.titleLabelY,
                    screen.imageWidth - screen.titleLabelX,
                    font.lineHeight,
                    RENAME_EDIT_BOX
            );
            editBox.setValue(title.getString());
            editBox.setBordered(false);
            editBox.setTextColor(4210752);
            editBox.setTextShadow(false);
            event.addListener(editBox);
            event.addListener(new GuiEventListener() {
                @Override
                public void setFocused(boolean focused) {}

                @Override
                public boolean isFocused() {
                    return false;
                }

                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if (editBox.isFocused() && !editBox.isHovered()) {
                        update(editBox, title);
                    }
                    return false;
                }
            });
            screen.title = Component.empty();
        }
    }

    public static void update(EditBox editBox, Component defaultName) {
        editBox.setFocused(false);
        String value = editBox.getValue();
        Component name = value.isEmpty() ? defaultName : Component.literal(value);
        PacketDistributor.sendToServer(new RenamePacketC2S(name));
    }

    @SuppressWarnings("unused")
    public enum DisplayMode implements TranslatableEnum {
        NEVER {
            @Override
            public boolean shouldDisplay(ChestBlockEntity blockEntity) {
                return false;
            }
        },
        ALWAYS {
            @Override
            public boolean shouldDisplay(ChestBlockEntity blockEntity) {
                return true;
            }
        },
        NOT_DEFAULT {
            @Override
            public boolean shouldDisplay(ChestBlockEntity blockEntity) {
                if (name == null || blockEntity.getLevel() == null) return false;
                Component defaultName = RequestNamePacketC2S.getDefaultName(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity);
                return !name.getString().equals(defaultName.getString());
            }
        };

        public abstract boolean shouldDisplay(ChestBlockEntity blockEntity);

        @Override
        public @NotNull Component getTranslatedName() {
            return Component.translatable("custom_chest_name.display_mode." + name().toLowerCase(Locale.ROOT));
        }
    }
}
