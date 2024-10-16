package com.gabdevele.gabmod;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CustomForgeHooksClient extends ForgeHooksClient{
    private static final ResourceLocation ICON_SHEET = new ResourceLocation("forge", "textures/gui/icons.png");

    public static void renderMainMenu(CustomTitleScreen gui, GuiGraphics graphics, Font font, int width, int height, int alpha) {
        VersionChecker.Status status = ForgeVersion.getStatus();
        if (status == VersionChecker.Status.BETA || status == VersionChecker.Status.BETA_OUTDATED) {
            Component line = Component.translatable("forge.update.beta.1", new Object[]{ChatFormatting.RED, ChatFormatting.RESET}).withStyle(ChatFormatting.RED);
            int var10003 = width / 2;
            Objects.requireNonNull(font);
            graphics.drawCenteredString(font, line, var10003, 4 + 0 * (9 + 1), 16777215 | alpha);
            line = Component.translatable("forge.update.beta.2");
            var10003 = width / 2;
            Objects.requireNonNull(font);
            graphics.drawCenteredString(font, line, var10003, 4 + 1 * (9 + 1), 16777215 | alpha);
        }

        String var10000;
        switch (status) {
            case OUTDATED:
            case BETA_OUTDATED:
                var10000 = I18n.get("forge.update.newversion", new Object[]{ForgeVersion.getTarget()});
                break;
            default:
                var10000 = null;
        }

        forgeStatusLine = var10000;
    }

    public static void drawForgePingInfo(CustomMultiplayerScreen gui, ServerData target, GuiGraphics guiGraphics, int x, int y, int width, int relativeMouseX, int relativeMouseY) {
        if (target.forgeData != null) {
            byte idx;
            String tooltip;
            switch (target.forgeData.type()) {
                case "FML":
                    if (target.forgeData.isCompatible()) {
                        idx = 0;
                        tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.compatible", new Object[]{target.forgeData.numberOfMods()});
                    } else {
                        idx = 16;
                        if (target.forgeData.extraReason() != null) {
                            String extraReason = ForgeI18n.parseMessage(target.forgeData.extraReason(), new Object[0]);
                            tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.incompatible.extra", new Object[]{extraReason});
                        } else {
                            tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.incompatible", new Object[0]);
                        }
                    }

                    if (target.forgeData.truncated()) {
                        tooltip = tooltip + "\n" + ForgeI18n.parseMessage("fml.menu.multiplayer.truncated", new Object[0]);
                    }
                    break;
                case "VANILLA":
                    if (target.forgeData.isCompatible()) {
                        idx = 48;
                        tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.vanilla", new Object[0]);
                    } else {
                        idx = 80;
                        tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.vanilla.incompatible", new Object[0]);
                    }
                    break;
                default:
                    idx = 64;
                    tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.unknown", new Object[]{target.forgeData.type()});
            }

            guiGraphics.blit(ICON_SHEET, x + width - 18, y + 10, 16, 16, 0.0F, (float)idx, 16, 16, 256, 256);
            if (relativeMouseX > width - 15 && relativeMouseX < width && relativeMouseY > 10 && relativeMouseY < 26) {
                List<MutableComponent> lines = Arrays.stream(tooltip.split("\n")).map(Component::literal).toList();
                gui.setTooltipForNextRenderPass(Lists.transform(lines, Component::getVisualOrderText));
            }

        }
    }
}
