package com.gabdevele.gabmod;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
import net.minecraftforge.internal.BrandingControl;
import org.slf4j.Logger;


//I modified the original code to add a custom things.
//this is the net.minecraft.client.gui.screens.TitleScreen code
//decompiled with IntelliJ

@OnlyIn(Dist.CLIENT)
public class CustomTitleScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("narrator.screen.title");
    private static final Component COPYRIGHT_TEXT = Component.translatable("title.credits");
    private static final String DEMO_LEVEL_ID = "Demo_World";
    private static final float FADE_IN_TIME = 2000.0F;
    @Nullable
    private SplashRenderer splash;
    private Button resetDemoButton;
    @Nullable
    private RealmsNotificationsScreen realmsNotificationsScreen;
    private float panoramaFade;
    private boolean fading;
    private long fadeInStart;
    @Nullable
    private WarningLabel warningLabel;
    private final LogoRenderer logoRenderer;
//    private TitleScreenModUpdateIndicator modUpdateNotification;

    public CustomTitleScreen() {
        this(false);
    }

    public CustomTitleScreen(boolean pFading) {
        this(pFading, (LogoRenderer)null);
    }

    public CustomTitleScreen(boolean pFading, @Nullable LogoRenderer pLogoRenderer) {
        super(TITLE);
        this.panoramaFade = 1.0F;
        this.fading = pFading;
        this.logoRenderer = (LogoRenderer)Objects.requireNonNullElseGet(pLogoRenderer, () -> {
            return new LogoRenderer(false);
        });
    }

    private boolean realmsNotificationsEnabled() {
        return this.realmsNotificationsScreen != null;
    }

    public void tick() {
        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.tick();
        }

    }

    public static CompletableFuture<Void> preloadResources(TextureManager pTexMngr, Executor pBackgroundExecutor) {
        return CompletableFuture.allOf(pTexMngr.preload(LogoRenderer.MINECRAFT_LOGO, pBackgroundExecutor), pTexMngr.preload(LogoRenderer.MINECRAFT_EDITION, pBackgroundExecutor), pTexMngr.preload(PanoramaRenderer.PANORAMA_OVERLAY, pBackgroundExecutor), CUBE_MAP.preload(pTexMngr, pBackgroundExecutor));
    }

    public boolean isPauseScreen() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    protected void init() {
        if (this.splash == null) {
            this.splash = this.minecraft.getSplashManager().getSplash();
        }

        int i = this.font.width(COPYRIGHT_TEXT);
        int j = this.width - i - 2;
        int l = this.height / 4 + 48;
        Button modButton = null;
        if (this.minecraft.isDemo()) {
            this.createDemoMenuOptions(l, 24);
        } else {
            this.createNormalMenuOptions(l, 24);
            modButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("fml.menu.mods"), (p_280830_) -> {
                this.minecraft.setScreen(new ModListScreen(this));
            }).pos(this.width / 2 - 100, l + 48).size(98, 20).build());
        }

//        this.modUpdateNotification = TitleScreenModUpdateIndicator.init( this, modButton);
        SpriteIconButton spriteiconbutton = (SpriteIconButton)this.addRenderableWidget(CommonButtons.language(20, (p_280838_) -> {
            this.minecraft.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()));
        }, true));
        spriteiconbutton.setPosition(this.width / 2 - 124, l + 72 + 12);
        this.addRenderableWidget(Button.builder(Component.translatable("menu.options"), (p_280831_) -> {
            this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
        }).bounds(this.width / 2 - 100, l + 72 + 12, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), (p_280835_) -> {
            this.minecraft.stop();
        }).bounds(this.width / 2 + 2, l + 72 + 12, 98, 20).build());
        SpriteIconButton spriteiconbutton1 = (SpriteIconButton)this.addRenderableWidget(CommonButtons.accessibility(20, (p_280834_) -> {
            this.minecraft.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options));
        }, true));
        spriteiconbutton1.setPosition(this.width / 2 + 104, l + 72 + 12);
        this.addRenderableWidget(new PlainTextButton(j, this.height - 10, i, 10, COPYRIGHT_TEXT, (p_280834_) -> {
            this.minecraft.setScreen(new CreditsAndAttributionScreen(this));
        }, this.font));
        if (this.realmsNotificationsScreen == null) {
            this.realmsNotificationsScreen = new RealmsNotificationsScreen();
        }

        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.init(this.minecraft, this.width, this.height);
        }

    }

    private void createNormalMenuOptions(int pY, int pRowHeight) {
        this.addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"), (p_280833_) -> {
            this.minecraft.setScreen(new SelectWorldScreen(this));
        }).bounds(this.width / 2 - 100, pY, 200, 20).build());
        Component component = this.getMultiplayerDisabledReason();
        boolean flag = component == null;
        Tooltip tooltip = component != null ? Tooltip.create(component) : null;
        ((Button)this.addRenderableWidget(Button.builder(Component.translatable("gabmod.multiplayer"), (p_325369_) -> {
            Screen screen = this.minecraft.options.skipMultiplayerWarning ? new CustomMultiplayerScreen(this) : new SafetyScreen(this);
            this.minecraft.setScreen((Screen)screen);
        }).bounds(this.width / 2 - 100, pY + pRowHeight * 1, 200, 20).tooltip(tooltip).build())).active = flag;
        ((Button)this.addRenderableWidget(Button.builder(Component.translatable("menu.online"), (p_325369_) -> {
            this.minecraft.setScreen(new RealmsMainScreen(this));
        }).bounds(this.width / 2 + 2, pY + pRowHeight * 2, 98, 20).tooltip(tooltip).build())).active = flag;
    }

    @Nullable
    private Component getMultiplayerDisabledReason() {
        if (this.minecraft.allowsMultiplayer()) {
            return null;
        } else if (this.minecraft.isNameBanned()) {
            return Component.translatable("title.multiplayer.disabled.banned.name");
        } else {
            BanDetails bandetails = this.minecraft.multiplayerBan();
            if (bandetails != null) {
                return bandetails.expires() != null ? Component.translatable("title.multiplayer.disabled.banned.temporary") : Component.translatable("title.multiplayer.disabled.banned.permanent");
            } else {
                return Component.translatable("title.multiplayer.disabled");
            }
        }
    }

    private void createDemoMenuOptions(int pY, int pRowHeight) {
        boolean flag = this.checkDemoWorldPresence();
        this.addRenderableWidget(Button.builder(Component.translatable("menu.playdemo"), (p_325371_) -> {
            if (flag) {
                this.minecraft.createWorldOpenFlows().openWorld("Demo_World", () -> {
                    this.minecraft.setScreen(this);
                });
            } else {
                this.minecraft.createWorldOpenFlows().createFreshLevel("Demo_World", MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
            }

        }).bounds(this.width / 2 - 100, pY, 200, 20).build());
        this.resetDemoButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("menu.resetdemo"), (p_308197_) -> {
            LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();

            try {
                LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.createAccess("Demo_World");

                try {
                    if (levelstoragesource$levelstorageaccess.hasWorldData()) {
                        this.minecraft.setScreen(new ConfirmScreen(this::confirmDemo, Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", new Object[]{MinecraftServer.DEMO_SETTINGS.levelName()}), Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
                    }
                } catch (Throwable var7) {
                    if (levelstoragesource$levelstorageaccess != null) {
                        try {
                            levelstoragesource$levelstorageaccess.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (levelstoragesource$levelstorageaccess != null) {
                    levelstoragesource$levelstorageaccess.close();
                }
            } catch (IOException var8) {
                IOException ioexception = var8;
                SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
                LOGGER.warn("Failed to access demo world", ioexception);
            }

        }).bounds(this.width / 2 - 100, pY + pRowHeight * 1, 200, 20).build());
        this.resetDemoButton.active = flag;
    }

    private boolean checkDemoWorldPresence() {
        try {
            LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().createAccess("Demo_World");

            boolean flag;
            try {
                flag = levelstoragesource$levelstorageaccess.hasWorldData();
            } catch (Throwable var6) {
                if (levelstoragesource$levelstorageaccess != null) {
                    try {
                        levelstoragesource$levelstorageaccess.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (levelstoragesource$levelstorageaccess != null) {
                levelstoragesource$levelstorageaccess.close();
            }

            return flag;
        } catch (IOException var7) {
            IOException ioexception = var7;
            SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to read demo world data", ioexception);
            return false;
        }
    }

    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }

        float f = 1.0F;
        if (this.fading) {
            float f1 = (float)(Util.getMillis() - this.fadeInStart) / 2000.0F;
            if (f1 > 1.0F) {
                this.fading = false;
                this.panoramaFade = 1.0F;
            } else {
                f1 = Mth.clamp(f1, 0.0F, 1.0F);
                f = Mth.clampedMap(f1, 0.5F, 1.0F, 0.0F, 1.0F);
                this.panoramaFade = Mth.clampedMap(f1, 0.0F, 0.5F, 0.0F, 1.0F);
            }

            this.fadeWidgets(f);
        }

        this.renderPanorama(pGuiGraphics, pPartialTick);
        int i = Mth.ceil(f * 255.0F) << 24;
        if ((i & -67108864) != 0) {
            super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            this.logoRenderer.renderLogo(pGuiGraphics, this.width, f);
            if (this.warningLabel != null) {
                this.warningLabel.render(pGuiGraphics, i);
            }

            CustomForgeHooksClient.renderMainMenu(this, pGuiGraphics, this.font, this.width, this.height, i);
            if (this.splash != null && !(Boolean)this.minecraft.options.hideSplashTexts().get()) {
                this.splash.render(pGuiGraphics, this.width, this.font, i);
            }

            String s = "Minecraft " + SharedConstants.getCurrentVersion().getName();
            if (this.minecraft.isDemo()) {
                s = s + " Demo";
            } else {
                s = s + ("release".equalsIgnoreCase(this.minecraft.getVersionType()) ? "" : "/" + this.minecraft.getVersionType());
            }

            if (Minecraft.checkModStatus().shouldReportAsModified()) {
                s = s + I18n.get("menu.modded", new Object[0]);
            }

            BrandingControl.forEachLine(true, true, (brdline, brd) -> {
                Font var10001 = this.font;
                int var10004 = this.height;
                int var10006 = brdline;
                Objects.requireNonNull(this.font);
                pGuiGraphics.drawString(var10001, brd, 2, var10004 - (10 + var10006 * (9 + 1)), 16777215 | i);
            });
            BrandingControl.forEachAboveCopyrightLine((brdline, brd) -> {
                Font var10001 = this.font;
                int var10003 = this.width - this.font.width(brd);
                int var10004 = this.height;
                int var10006 = brdline + 1;
                Objects.requireNonNull(this.font);
                pGuiGraphics.drawString(var10001, brd, var10003, var10004 - (10 + var10006 * (9 + 1)), 16777215 | i);
            });
            if (this.realmsNotificationsEnabled() && f >= 1.0F) {
                RenderSystem.enableDepthTest();
                this.realmsNotificationsScreen.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }

//            if (f >= 1.0F) {
//                this.modUpdateNotification.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
//            }
        }

    }

    private void fadeWidgets(float pAlpha) {
        Iterator var2 = this.children().iterator();

        while(var2.hasNext()) {
            GuiEventListener guieventlistener = (GuiEventListener)var2.next();
            if (guieventlistener instanceof AbstractWidget abstractwidget) {
                abstractwidget.setAlpha(pAlpha);
            }
        }

    }

    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    }

    protected void renderPanorama(GuiGraphics pGuiGraphics, float pPartialTick) {
        PANORAMA.render(pGuiGraphics, this.width, this.height, this.panoramaFade, this.advancePanoramaTime());
    }

    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return super.mouseClicked(pMouseX, pMouseY, pButton) ? true : this.realmsNotificationsEnabled() && this.realmsNotificationsScreen.mouseClicked(pMouseX, pMouseY, pButton);
    }

    public void removed() {
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.removed();
        }

    }

    public void added() {
        super.added();
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.added();
        }

    }

    private void confirmDemo(boolean p_96778_) {
        if (p_96778_) {
            try {
                LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().createAccess("Demo_World");

                try {
                    levelstoragesource$levelstorageaccess.deleteLevel();
                } catch (Throwable var6) {
                    if (levelstoragesource$levelstorageaccess != null) {
                        try {
                            levelstoragesource$levelstorageaccess.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }
                    }

                    throw var6;
                }

                if (levelstoragesource$levelstorageaccess != null) {
                    levelstoragesource$levelstorageaccess.close();
                }
            } catch (IOException var7) {
                IOException ioexception = var7;
                SystemToast.onWorldDeleteFailure(this.minecraft, "Demo_World");
                LOGGER.warn("Failed to delete demo world", ioexception);
            }
        }

        this.minecraft.setScreen(this);
    }

    @OnlyIn(Dist.CLIENT)
     static record WarningLabel(Font font, MultiLineLabel label, int x, int y) {
        WarningLabel(Font font, MultiLineLabel label, int x, int y) {
            this.font = font;
            this.label = label;
            this.x = x;
            this.y = y;
        }

        public void render(GuiGraphics pGuiGraphics, int pColor) {
            this.label.renderBackgroundCentered(pGuiGraphics, this.x, this.y, 9, 2, 2097152 | Math.min(pColor, 1426063360));
            this.label.renderCentered(pGuiGraphics, this.x, this.y, 9, 16777215 | pColor);
        }

        public Font font() {
            return this.font;
        }

        public MultiLineLabel label() {
            return this.label;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }
    }
}

