package com.gabdevele.gabmod;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> {
    static final ResourceLocation INCOMPATIBLE_SPRITE = new ResourceLocation("server_list/incompatible");
    static final ResourceLocation UNREACHABLE_SPRITE = new ResourceLocation("server_list/unreachable");
    static final ResourceLocation PING_1_SPRITE = new ResourceLocation("server_list/ping_1");
    static final ResourceLocation PING_2_SPRITE = new ResourceLocation("server_list/ping_2");
    static final ResourceLocation PING_3_SPRITE = new ResourceLocation("server_list/ping_3");
    static final ResourceLocation PING_4_SPRITE = new ResourceLocation("server_list/ping_4");
    static final ResourceLocation PING_5_SPRITE = new ResourceLocation("server_list/ping_5");
    static final ResourceLocation PINGING_1_SPRITE = new ResourceLocation("server_list/pinging_1");
    static final ResourceLocation PINGING_2_SPRITE = new ResourceLocation("server_list/pinging_2");
    static final ResourceLocation PINGING_3_SPRITE = new ResourceLocation("server_list/pinging_3");
    static final ResourceLocation PINGING_4_SPRITE = new ResourceLocation("server_list/pinging_4");
    static final ResourceLocation PINGING_5_SPRITE = new ResourceLocation("server_list/pinging_5");
    static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = new ResourceLocation("server_list/join_highlighted");
    static final ResourceLocation JOIN_SPRITE = new ResourceLocation("server_list/join");
    static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = new ResourceLocation("server_list/move_up_highlighted");
    static final ResourceLocation MOVE_UP_SPRITE = new ResourceLocation("server_list/move_up");
    static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = new ResourceLocation("server_list/move_down_highlighted");
    static final ResourceLocation MOVE_DOWN_SPRITE = new ResourceLocation("server_list/move_down");
    static final Logger LOGGER = LogUtils.getLogger();
    static final ThreadPoolExecutor THREAD_POOL;
    static final Component SCANNING_LABEL;
    static final Component CANT_RESOLVE_TEXT;
    static final Component CANT_CONNECT_TEXT;
    static final Component INCOMPATIBLE_STATUS;
    static final Component NO_CONNECTION_STATUS;
    static final Component PINGING_STATUS;
    static final Component ONLINE_STATUS;
    private final CustomMultiplayerScreen screen;
    private final List<OnlineServerEntry> onlineServers = Lists.newArrayList();
    private final LANHeader lanHeader = new LANHeader();
    private final List<NetworkServerEntry> networkServers = Lists.newArrayList();


    public ServerSelectionList(CustomMultiplayerScreen pScreen, Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
        super(pMinecraft, pWidth, pHeight, pY, pItemHeight);
        this.screen = pScreen;
    }
    private void refreshEntries() {
        this.clearEntries();
        this.onlineServers.forEach(this::addEntry);
        this.addEntry(this.lanHeader);
        this.networkServers.forEach(this::addEntry);
    }

    public void setSelected(@Nullable Entry pEntry) {
        super.setSelected(pEntry);
        this.screen.onSelectedChange();
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        Entry serverselectionlist$entry = this.getSelected();
        return serverselectionlist$entry != null && serverselectionlist$entry.keyPressed(pKeyCode, pScanCode, pModifiers) || super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    public void updateOnlineServers(ServerList pServers) {
        this.onlineServers.clear();

        for (int i = 0; i < pServers.size(); ++i) {
            this.onlineServers.add(new OnlineServerEntry(this.screen, pServers.get(i)));
        }

        this.refreshEntries();
    }

    public void updateNetworkServers(List<LanServer> pLanServers) {
        int i = pLanServers.size() - this.networkServers.size();
        this.networkServers.clear();
        Iterator<LanServer> var3 = pLanServers.iterator();

        while (var3.hasNext()) {
            LanServer lanserver = var3.next();
            this.networkServers.add(new NetworkServerEntry(this.screen, lanserver));
        }

        this.refreshEntries();

        for (int i1 = this.networkServers.size() - i; i1 < this.networkServers.size(); ++i1) {
            NetworkServerEntry serverselectionlist$networkserverentry = this.networkServers.get(i1);
            int j = i1 - this.networkServers.size() + this.children().size();
            int k = this.getRowTop(j);
            int l = this.getRowBottom(j);
            if (l >= this.getY() && k <= this.getBottom()) {
                this.minecraft.getNarrator().say(Component.translatable("multiplayer.lan.server_found", serverselectionlist$networkserverentry.getServerNarration()));
            }
        }
    }

    public int getRowWidth() {
        return 305;
    }

    public void removed() {
    }

    static {
        THREAD_POOL = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());
        SCANNING_LABEL = Component.translatable("lanServer.scanning");
        CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withColor(-65536);
        CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
        INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
        NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
        PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
        ONLINE_STATUS = Component.translatable("multiplayer.status.online");
    }

    @OnlyIn(Dist.CLIENT)
    public static class LANHeader extends Entry {
        private final Minecraft minecraft = Minecraft.getInstance();

        public LANHeader() {
        }

        public void render(GuiGraphics pGuiGraphics, int p_282477_, int p_282819_, int p_282001_, int p_281911_, int p_283126_, int p_282303_, int p_281998_, boolean p_282625_, float p_281811_) {
            int i = p_282819_ + p_283126_ / 2 - 4;
            pGuiGraphics.drawString(this.minecraft.font, SCANNING_LABEL, this.minecraft.screen.width / 2 - this.minecraft.font.width(SCANNING_LABEL) / 2, i, 16777215, false);
            String s = LoadingDotsText.get(Util.getMillis());
            pGuiGraphics.drawString(this.minecraft.font, s, this.minecraft.screen.width / 2 - this.minecraft.font.width(s) / 2, i + 9, -8355712, false);
        }

        public Component getNarration() {
            return SCANNING_LABEL;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ObjectSelectionList.Entry<Entry> implements AutoCloseable {
        public Entry() {
        }

        public void close() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class OnlineServerEntry extends Entry {
        private static final int ICON_WIDTH = 32;
        private static final int ICON_HEIGHT = 32;
        private static final int SPACING = 5;
        private static final int STATUS_ICON_WIDTH = 10;
        private static final int STATUS_ICON_HEIGHT = 8;
        private final CustomMultiplayerScreen screen;
        private final Minecraft minecraft;
        private final ServerData serverData;
        private final FaviconTexture icon;
        @Nullable
        private byte[] lastIconBytes;
        private long lastClickTime;
        @Nullable
        private List<Component> onlinePlayersTooltip;
        @Nullable
        private ResourceLocation statusIcon;
        @Nullable
        private Component statusIconTooltip;

        protected OnlineServerEntry(final CustomMultiplayerScreen pScreen, final ServerData pServerData) {
            this.screen = pScreen;
            this.serverData = pServerData;
            this.minecraft = Minecraft.getInstance();
            this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), pServerData.ip);
            this.refreshStatus();
        }

        public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
            if (this.serverData.state() == State.INITIAL) {
                this.serverData.setState(State.PINGING);
                this.serverData.motd = CommonComponents.EMPTY;
                this.serverData.status = CommonComponents.EMPTY;
                THREAD_POOL.submit(() -> {
                    try {
                        this.screen.getPinger().pingServer(this.serverData, () -> {
                            this.minecraft.execute(this::updateServerList);
                        }, () -> {
                            this.serverData.setState(this.serverData.protocol == SharedConstants.getCurrentVersion().getProtocolVersion() ? State.SUCCESSFUL : State.INCOMPATIBLE);
                            this.minecraft.execute(this::refreshStatus);
                        });
                    } catch (UnknownHostException var2) {
                        this.serverData.setState(State.UNREACHABLE);
                        this.serverData.motd = CANT_RESOLVE_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                    } catch (Exception var3) {
                        this.serverData.setState(State.UNREACHABLE);
                        this.serverData.motd = CANT_CONNECT_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                    }
                });
            }

            pGuiGraphics.drawString(this.minecraft.font, this.serverData.name, pLeft + 32 + 3, pTop + 1, 16777215, false);
            List<FormattedCharSequence> list = this.minecraft.font.split(this.serverData.motd, pWidth - 32 - 2);

            int k1;
            for (k1 = 0; k1 < Math.min(list.size(), 2); ++k1) {
                pGuiGraphics.drawString(this.minecraft.font, list.get(k1), pLeft + 32 + 3, pTop + 12 + 9 * k1, -8355712, false);
            }

            this.drawIcon(pGuiGraphics, pLeft, pTop, this.icon.textureLocation());
            if (this.serverData.state() == State.PINGING) {
                k1 = (int) (Util.getMillis() / 100L + (long) (pIndex * 2) & 7L);
                if (k1 > 4) {
                    k1 = 8 - k1;
                }

                ResourceLocation var10001;
                switch (k1) {
                    case 1 -> var10001 = PINGING_2_SPRITE;
                    case 2 -> var10001 = PINGING_3_SPRITE;
                    case 3 -> var10001 = PINGING_4_SPRITE;
                    case 4 -> var10001 = PINGING_5_SPRITE;
                    default -> var10001 = PINGING_1_SPRITE;
                }

                this.statusIcon = var10001;
            }

            k1 = pLeft + pWidth - 10 - 5;
            if (this.statusIcon != null) {
                pGuiGraphics.blitSprite(this.statusIcon, k1, pTop, 10, 8);
            }

            byte[] abyte = this.serverData.getIconBytes();
            if (!Arrays.equals(abyte, this.lastIconBytes)) {
                if (this.uploadServerIcon(abyte)) {
                    this.lastIconBytes = abyte;
                } else {
                    this.serverData.setIconBytes(null);
                    this.updateServerList();
                }
            }

            Component component = this.serverData.state() == State.INCOMPATIBLE ? this.serverData.version.copy().withStyle(ChatFormatting.RED) : this.serverData.status;
            int j = this.minecraft.font.width(component);
            int k = k1 - j - 5;
            pGuiGraphics.drawString(this.minecraft.font, component, k, pTop + 1, -8355712, false);
            if (this.statusIconTooltip != null && pMouseX >= k1 && pMouseX <= k1 + 10 && pMouseY >= pTop && pMouseY <= pTop + 8) {
                this.screen.setTooltipForNextRenderPass(this.statusIconTooltip);
            } else if (this.onlinePlayersTooltip != null && pMouseX >= k && pMouseX <= k + j && pMouseY >= pTop && pMouseY <= pTop - 1 + 9) {
                this.screen.setTooltipForNextRenderPass(Lists.transform(this.onlinePlayersTooltip, Component::getVisualOrderText));
            }

            CustomForgeHooksClient.drawForgePingInfo(this.screen, this.serverData, pGuiGraphics, pLeft, pTop, pWidth, pMouseX - pLeft, pMouseY - pTop);
            if (this.minecraft.options.touchscreen().get() || pHovering) {
                pGuiGraphics.fill(pLeft, pTop, pLeft + 32, pTop + 32, -1601138544);
                int l = pMouseX - pLeft;
                int i1 = pMouseY - pTop;
                if (this.canJoin()) {
                    if (l < 32 && l > 16) {
                        pGuiGraphics.blitSprite(JOIN_HIGHLIGHTED_SPRITE, pLeft, pTop, 32, 32);
                    } else {
                        pGuiGraphics.blitSprite(JOIN_SPRITE, pLeft, pTop, 32, 32);
                    }
                }

                if (pIndex > 0) {
                    if (l < 16 && i1 < 16) {
                        pGuiGraphics.blitSprite(MOVE_UP_HIGHLIGHTED_SPRITE, pLeft, pTop, 32, 32);
                    } else {
                        pGuiGraphics.blitSprite(MOVE_UP_SPRITE, pLeft, pTop, 32, 32);
                    }
                }

                if (pIndex < this.screen.getServers().size() - 1) {
                    if (l < 16 && i1 > 16) {
                        pGuiGraphics.blitSprite(MOVE_DOWN_HIGHLIGHTED_SPRITE, pLeft, pTop, 32, 32);
                    } else {
                        pGuiGraphics.blitSprite(MOVE_DOWN_SPRITE, pLeft, pTop, 32, 32);
                    }
                }
            }
        }

        private void refreshStatus() {
            this.onlinePlayersTooltip = null;
            switch (this.serverData.state()) {
                case INITIAL, PINGING -> {
                    this.statusIcon = PING_1_SPRITE;
                    this.statusIconTooltip = PINGING_STATUS;
                }
                case INCOMPATIBLE -> {
                    this.statusIcon = INCOMPATIBLE_SPRITE;
                    this.statusIconTooltip = INCOMPATIBLE_STATUS;
                    this.onlinePlayersTooltip = this.serverData.playerList;
                }
                case UNREACHABLE -> {
                    this.statusIcon = UNREACHABLE_SPRITE;
                    this.statusIconTooltip = NO_CONNECTION_STATUS;
                }
                case SUCCESSFUL -> {
                    if (this.serverData.ping < 150L) {
                        this.statusIcon = PING_5_SPRITE;
                    } else if (this.serverData.ping < 300L) {
                        this.statusIcon = PING_4_SPRITE;
                    } else if (this.serverData.ping < 600L) {
                        this.statusIcon = PING_3_SPRITE;
                    } else if (this.serverData.ping < 1000L) {
                        this.statusIcon = PING_2_SPRITE;
                    } else {
                        this.statusIcon = PING_1_SPRITE;
                    }

                    this.statusIconTooltip = Component.translatable("multiplayer.status.ping", this.serverData.ping);
                    this.onlinePlayersTooltip = this.serverData.playerList;
                }
            }
        }

        public void updateServerList() {
            this.screen.getServers().save();
        }

        protected void drawIcon(GuiGraphics pGuiGraphics, int pX, int pY, ResourceLocation pIcon) {
            RenderSystem.enableBlend();
            pGuiGraphics.blit(pIcon, pX, pY, 0.0F, 0.0F, 32, 32, 32, 32);
            RenderSystem.disableBlend();
        }

        private boolean canJoin() {
            return true;
        }

        private boolean uploadServerIcon(@Nullable byte[] pIconBytes) {
            if (pIconBytes == null) {
                this.icon.clear();
            } else {
                try {
                    this.icon.upload(NativeImage.read(pIconBytes));
                } catch (Throwable var3) {
                    Throwable throwable = var3;
                    ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", new Object[]{this.serverData.name, this.serverData.ip, throwable});
                    return false;
                }
            }

            return true;
        }

        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            if (Screen.hasShiftDown()) {
                ServerSelectionList serverselectionlist = this.screen.serverSelectionList;
                int i = serverselectionlist.children().indexOf(this);
                if (i == -1) {
                    return true;
                }

                if (pKeyCode == 264 && i < this.screen.getServers().size() - 1 || pKeyCode == 265 && i > 0) {
                    this.swap(i, pKeyCode == 264 ? i + 1 : i - 1);
                    return true;
                }
            }

            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        private void swap(int pPos1, int pPos2) {
            this.screen.getServers().swap(pPos1, pPos2);
            this.screen.serverSelectionList.updateOnlineServers(this.screen.getServers());
            ServerSelectionList.Entry serverselectionlist$entry = this.screen.serverSelectionList.children().get(pPos2);
            this.screen.serverSelectionList.setSelected(serverselectionlist$entry);
            //ServerSelectionList.this.ensureVisible(serverselectionlist$entry);

        }

        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            double d0 = pMouseX - (double) ServerSelectionList.this.getRowLeft();
            double d1 = pMouseY - (double) ServerSelectionList.this.getRowTop(ServerSelectionList.this.children().indexOf(this));
            if (d0 <= 32.0) {
                if (d0 < 32.0 && d0 > 16.0 && this.canJoin()) {
                    this.screen.setSelected(this);
                    this.screen.joinSelectedServer();
                    return true;
                }

                int i = this.screen.serverSelectionList.children().indexOf(this);
                if (d0 < 16.0 && d1 < 16.0 && i > 0) {
                    this.swap(i, i - 1);
                    return true;
                }

                if (d0 < 16.0 && d1 > 16.0 && i < this.screen.getServers().size() - 1) {
                    this.swap(i, i + 1);
                    return true;
                }
            }

            this.screen.setSelected(this);
            if (Util.getMillis() - this.lastClickTime < 250L) {
                this.screen.joinSelectedServer();
            }

            this.lastClickTime = Util.getMillis();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        public ServerData getServerData() {
            return this.serverData;
        }

        public Component getNarration() {
            MutableComponent mutablecomponent = Component.empty();
            mutablecomponent.append(Component.translatable("narrator.select", new Object[]{this.serverData.name}));
            mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
            switch (this.serverData.state()) {
                case PINGING:
                    mutablecomponent.append(ServerSelectionList.PINGING_STATUS);
                    break;
                case INCOMPATIBLE:
                    mutablecomponent.append(ServerSelectionList.INCOMPATIBLE_STATUS);
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.version.narration", new Object[]{this.serverData.version}));
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", new Object[]{this.serverData.motd}));
                    break;
                case UNREACHABLE:
                    mutablecomponent.append(ServerSelectionList.NO_CONNECTION_STATUS);
                    break;
                default:
                    mutablecomponent.append(ServerSelectionList.ONLINE_STATUS);
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.ping.narration", new Object[]{this.serverData.ping}));
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", new Object[]{this.serverData.motd}));
                    if (this.serverData.players != null) {
                        mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                        mutablecomponent.append(Component.translatable("multiplayer.status.player_count.narration", new Object[]{this.serverData.players.online(), this.serverData.players.max()}));
                        mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                        mutablecomponent.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
                    }
            }

            return mutablecomponent;
        }

        public void close() {
            this.icon.close();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NetworkServerEntry extends ServerSelectionList.Entry {
        private static final int ICON_WIDTH = 32;
        private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
        private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
        private final CustomMultiplayerScreen screen;
        protected final Minecraft minecraft;
        protected final LanServer serverData;
        private long lastClickTime;

        protected NetworkServerEntry(CustomMultiplayerScreen pScreen, LanServer pServerData) {
            this.screen = pScreen;
            this.serverData = pServerData;
            this.minecraft = Minecraft.getInstance();
        }

        public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
            pGuiGraphics.drawString(this.minecraft.font, LAN_SERVER_HEADER, pLeft + 32 + 3, pTop + 1, 16777215, false);
            pGuiGraphics.drawString(this.minecraft.font, this.serverData.getMotd(), pLeft + 32 + 3, pTop + 12, -8355712, false);
            if (this.minecraft.options.hideServerAddress) {
                pGuiGraphics.drawString(this.minecraft.font, HIDDEN_ADDRESS_TEXT, pLeft + 32 + 3, pTop + 12 + 11, 3158064, false);
            } else {
                pGuiGraphics.drawString(this.minecraft.font, this.serverData.getAddress(), pLeft + 32 + 3, pTop + 12 + 11, 3158064, false);
            }

        }

        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            this.screen.setSelected(this);
            if (Util.getMillis() - this.lastClickTime < 250L) {
                this.screen.joinSelectedServer();
            }

            this.lastClickTime = Util.getMillis();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        public LanServer getServerData() {
            return this.serverData;
        }

        public Component getNarration() {
            return Component.translatable("narrator.select", new Object[]{this.getServerNarration()});
        }

        public Component getServerNarration() {
            return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
        }
    }
}
