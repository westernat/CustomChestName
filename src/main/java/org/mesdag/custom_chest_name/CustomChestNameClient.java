package org.mesdag.custom_chest_name;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.mesdag.custom_chest_name.integration.JadeHelper;
import org.mesdag.custom_chest_name.network.RenamePacketC2S;
import org.mesdag.custom_chest_name.network.RequestNamePacketC2S;

public class CustomChestNameClient {
    public static final Component RENAME_EDIT_BOX = Component.literal("RenameEditBox");
    public static final boolean IS_JADE_LOADED = LoadingModList.get().getModFileById("jade") != null;
    private static Component name;
    private static BlockPos lastPos;

    private static ForgeConfigSpec.EnumValue<DisplayMode> displayMode;

    public static ForgeConfigSpec config() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        displayMode = builder.defineEnum("displayMode", DisplayMode.NOT_DEFAULT);
        return builder.build();
    }

    public static void setName(BlockPos pos, Component name) {
        if (pos.equals(lastPos)) {
            CustomChestNameClient.name = name;
        }
    }

    @Mod.EventBusSubscriber(modid = CustomChestName.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("chest_name", (forgeGui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
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
                            CustomChestName.CHANNEL.send(
                                    PacketDistributor.SERVER.noArg(),
                                    new RequestNamePacketC2S(blockPos)
                            );
                        }
                        if (name != null && displayMode.get().shouldDisplay(blockEntity)) {
                            RenderSystem.setShaderColor(1,1,1,0.7F);
                            Font font = minecraft.font;
                            int x = screenWidth / 2 + 10;
                            int y = screenHeight / 2 + 7;
                            guiGraphics.renderTooltip(font, name, x, y);
                            RenderSystem.setShaderColor(1,1,1,1);
                        }
                    }
                } else {
                    name = null;
                }
            });
        }
    }

    @Mod.EventBusSubscriber(modid = CustomChestName.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GameEvents {
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
                ) {
                    @Override
                    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        super.renderWidget(new GuiGraphics(Minecraft.getInstance(), guiGraphics.bufferSource()) {
                            @Override
                            public int drawString(Font p_282636_, FormattedCharSequence p_281596_, float p_281586_, float p_282816_, int p_281743_, boolean p_282394_) {
                                return guiGraphics.drawString(p_282636_, p_281596_, p_281586_, p_282816_, p_281743_, false);
                            }

                            @Override
                            public int drawString(Font p_283343_, @Nullable String p_281896_, float p_283569_, float p_283418_, int p_281560_, boolean p_282130_) {
                                return guiGraphics.drawString(p_283343_, p_281896_, p_283569_, p_283418_, p_281560_, false);
                            }

                            @Override
                            public void fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int z, int color) {
                                guiGraphics.fill(renderType, minX, minY, maxX, maxY, z, color);
                            }
                        }, mouseX, mouseY, partialTick);
                    }
                };
                editBox.setValue(title.getString());
                editBox.setBordered(false);
                editBox.setTextColor(4210752);
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
    }

    public static void update(EditBox editBox, Component defaultName) {
        editBox.setFocused(false);
        String value = editBox.getValue();
        Component name = value.isEmpty() ? defaultName : Component.literal(value);
        CustomChestName.CHANNEL.send(
                PacketDistributor.SERVER.noArg(),
                new RenamePacketC2S(name)
        );
    }

    @SuppressWarnings("unused")
    public enum DisplayMode {
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
        },
        IS_CROUCHING {
            @Override
            public boolean shouldDisplay(ChestBlockEntity blockEntity) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return false;
                return player.isCrouching();
            }
        },
        IS_CROUCHING_AND_NOT_DEFAULT {
            @Override
            public boolean shouldDisplay(ChestBlockEntity blockEntity) {
                return IS_CROUCHING.shouldDisplay(blockEntity) && NOT_DEFAULT.shouldDisplay(blockEntity);
            }
        };

        public abstract boolean shouldDisplay(ChestBlockEntity blockEntity);
    }
}
