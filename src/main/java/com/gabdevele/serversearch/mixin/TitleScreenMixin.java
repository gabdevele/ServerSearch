package com.gabdevele.serversearch.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    @Unique
    private static final Logger serversearch$LOGGER = LogUtils.getLogger();
    protected TitleScreenMixin(Component pTitle) {
        super(pTitle);
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    private void serverSearch$init(CallbackInfo info) {
        serversearch$LOGGER.info("Initializing ServerSearch Mod");
    }
}
