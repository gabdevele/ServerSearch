package com.gabdevele.serversearch.mixin;

import com.gabdevele.serversearch.ISearchParameter;
import com.gabdevele.serversearch.ServerApi;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Unique private EditBox serverSearch$searchBar;
    @Unique private final ScheduledExecutorService serverSearch$scheduler = Executors.newScheduledThreadPool(1);
    @Unique private ScheduledFuture<?> serverSearch$debounceFuture;
    @Unique private SpriteIconButton serverSearch$offlineModeButton;
    @Unique private boolean serverSearch$offlineMode = false;
    @Unique private static final Logger serverSearch$LOGGER = LogUtils.getLogger();

    @Shadow private ServerList servers;
    @Shadow protected ServerSelectionList serverSelectionList;
    @Shadow private Button editButton;
    @Shadow private Button selectButton;
    @Shadow private Button deleteButton;
    @Shadow private ServerData editingServer;

    @Shadow protected abstract void addServerCallback(boolean p_99722_);


    @Shadow @Final private static Logger LOGGER;

    public MultiplayerScreenMixin(Component pTitle, List<ServerSelectionList.OnlineServerEntry> onlineServers) {
        super(pTitle);
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    private void serverSearch$init(CallbackInfo info) {
        serverSearch$LOGGER.info("Initializing MultiplayerScreenMixin");
        this.serverSearch$searchBar = new EditBox(this.minecraft.font, this.width / 2 - 100, 5, 200, 20,
                Component.translatable("serverSearch.search"));
        this.serverSearch$searchBar.setResponder(this::serverSearch$searchBarResponder);
        this.addWidget(this.serverSearch$searchBar);

        serverSearch$offlineModeButton = this.serverSearch$generateIconButton();
        serverSearch$offlineModeButton = this.addWidget(this.serverSearch$offlineModeButton);
        serverSearch$offlineModeButton.setPosition(this.width / 2 + 120, 5);

    }

    @Unique
    private SpriteIconButton serverSearch$generateIconButton() {
        SpriteIconButton button = SpriteIconButton.builder(Component.translatable("serverSearch.offlineMode"), (e) -> {
            serverSearch$offlineMode = !serverSearch$offlineMode;
            serverSearch$offlineModeButton = this.serverSearch$generateIconButton();
            serverSearch$offlineModeButton = this.addWidget(this.serverSearch$offlineModeButton);
            serverSearch$offlineModeButton.setPosition(this.width / 2 + 120, 5);
            serverSearch$searchBarResponder(serverSearch$searchBar.getValue());
        }, true).width(20).sprite(
                new ResourceLocation(serverSearch$offlineMode ?  "icon/new_realm" : "icon/link"),
                15, 15).build();
        button.setTooltip(Tooltip.create(
                serverSearch$offlineMode ? Component.translatable("serverSearch.offlineMode") :
                        Component.translatable("serverSearch.onlineMode"))
        );
        return button;
    }

    @Unique
    private void serverSearch$searchBarResponder(String text) {
        if (serverSearch$debounceFuture != null && !serverSearch$debounceFuture.isDone()) {
            serverSearch$debounceFuture.cancel(false);
        }
        serverSearch$debounceFuture = serverSearch$scheduler.schedule(() -> {
            if (text.isEmpty()) {
                this.minecraft.execute(() -> this.serverSelectionList.updateOnlineServers(this.servers));
                return;
            }
            serverSearch$LOGGER.info("Search: {}", text);
            ((ISearchParameter)serverSelectionList).serverSearch$setSearched(true);
            ServerApi.fetchServerData(text, serverSearch$offlineMode).thenAccept(serverDataList -> {
                serverSearch$LOGGER.info("Server data list: {}", serverDataList);
                ServerList serverList = new ServerList(this.minecraft);
                for (ServerData serverData : serverDataList) {
                    serverSearch$LOGGER.info("Server data: {}", serverData);
                    ((ISearchParameter) serverData).serverSearch$setSearched(true);
                    serverList.add(serverData, false);
                }
                ((ISearchParameter)serverSelectionList).serverSearch$setSearched(false);
                this.minecraft.execute(() -> {
                    this.serverSelectionList.updateOnlineServers(serverList);
                });
                
            }).exceptionally(throwable -> {
                ((ISearchParameter)serverSelectionList).serverSearch$setSearched(false);
                LOGGER.error("Failed to fetch server data", throwable);
                return null;
            });
        }, 300, TimeUnit.MILLISECONDS);
    }

    @Inject(method = "render", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void serverSearch$render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        this.serverSearch$searchBar.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.serverSearch$offlineModeButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        ci.cancel();
    }

    @Inject(method = "joinSelectedServer", at = @At("HEAD"), cancellable = true)
    private void serverSearch$joinSelectedServer(CallbackInfo ci) {
        ISearchParameter serverEntry = (ISearchParameter) this.serverSelectionList.getSelected();
        if (serverEntry != null && serverEntry.serverSearch$isSearched()) {
            this.editingServer = ((ServerSelectionList.OnlineServerEntry) this.serverSelectionList.getSelected()).getServerData();
            ((ISearchParameter)this.editingServer).serverSearch$setSearched(false);
            this.addServerCallback(true);
            ci.cancel();
        }
    }

    @Inject(method = "onSelectedChange", at = @At("TAIL"))
    private void serverSearch$onSelectedChange(CallbackInfo info) {
        ISearchParameter serverEntry = (ISearchParameter) this.serverSelectionList.getSelected();
        if (serverEntry != null && serverEntry.serverSearch$isSearched()) {
            this.editButton.active = false;
            this.deleteButton.active = false;
            this.selectButton.setMessage(Component.translatable("serverSearch.save"));
        } else {
            this.selectButton.setMessage(Component.translatable("selectServer.select"));
        }
    }
}