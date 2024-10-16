package com.gabdevele.gabmod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = GabMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScreenEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean isCheckingTitleScreen = false;
    private static boolean firstTime = true;

    @SubscribeEvent
    public static void openMainMenu(ScreenEvent.Init.Pre event) {
        LOGGER.info("Screen opened: {}", event.getScreen().getClass().getName());
        if (event.getScreen() instanceof TitleScreen) {
            isCheckingTitleScreen = true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (isCheckingTitleScreen && Minecraft.getInstance().screen instanceof TitleScreen screen) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new CustomTitleScreen(firstTime));
                isCheckingTitleScreen = false;
                firstTime = false;
            });
        }
    }

}