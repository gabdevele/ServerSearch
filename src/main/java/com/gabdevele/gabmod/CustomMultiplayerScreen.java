package com.gabdevele.gabmod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import javax.annotation.Nullable;
import java.util.List;

//https://findmcserver.com/api/servers?pageNumber=0&pageSize=15&sortBy=default&gamerSaferStatus=undefined&mojangStatus=undefined&searchTerms=hy&size=
//TODO: trovare un api decente internazionale oppure se non esiste fare scraping
//TODO: fixare bug tasto refresh
//TODO: riutilizzare il loading della lan per la ricerca di server
//TODO: fix crash quando si clicca sulla freccia per spostarsi tra i server
//TODO: sostituire i numeri con le costanti
//TODO: ottimizare codice decompilato

@OnlyIn(Dist.CLIENT)
public class CustomMultiplayerScreen extends Screen {
    public static final int BUTTON_ROW_WIDTH = 308;
    public static final int TOP_ROW_BUTTON_WIDTH = 100;
    public static final int LOWER_ROW_BUTTON_WIDTH = 74;
    public static final int FOOTER_HEIGHT = 64;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final Screen lastScreen;
    protected ServerSelectionList serverSelectionList;
    private ServerList servers;
    private Button editButton;
    private Button selectButton;
    private Button deleteButton;
    private ServerData editingServer;
    private LanServerDetection.LanServerList lanServerList;
    @Nullable
    private LanServerDetection.LanServerDetector lanServerDetector;
    private boolean initedOnce;
    private EditBox searchBar;

    public CustomMultiplayerScreen(Screen pLastScreen) {
        super(Component.translatable("multiplayer.title"));
        this.lastScreen = pLastScreen;
    }

    protected void init() {
    if (this.initedOnce) {
        this.serverSelectionList.setRectangle(this.width, this.height - 64 - 32, 0, 32);
    } else {
        this.initedOnce = true;
        this.servers = new ServerList(this.minecraft);
        this.servers.load();
        this.lanServerList = new LanServerDetection.LanServerList();

        try {
            this.lanServerDetector = new LanServerDetection.LanServerDetector(this.lanServerList);
            this.lanServerDetector.start();
        } catch (Exception var8) {
            LOGGER.warn("Unable to start LAN server detection: {}", var8.getMessage());
        }

        this.serverSelectionList = new ServerSelectionList(this, this.minecraft, this.width, this.height - 64 - 28, 32, 36);
        this.serverSelectionList.updateOnlineServers(this.servers);
    }

    this.addRenderableWidget(this.serverSelectionList);

    this.searchBar = new EditBox(this.minecraft.font, this.width / 2 - 100, 5, 200, 20,
            Component.translatable("gabmod.search"));
    this.searchBar.setResponder((text) -> {
        if (text.isEmpty()) {
            this.serverSelectionList.updateOnlineServers(this.servers);
            return;
        }
        LOGGER.info("Search: {}", text);
        ServerApi.fetchServerData(text).thenAccept(serverDataList -> {
            LOGGER.info("Server data list: {}", serverDataList);
            ServerList serverList = new ServerList(this.minecraft);
            for (ServerData serverData : serverDataList) {
                LOGGER.info("Server data: {}", serverData);
                serverList.add(serverData, false);
            }
            this.minecraft.execute(() -> this.serverSelectionList.updateOnlineServers(serverList));
        });
    });
    this.addWidget(this.searchBar);

    this.selectButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.select"), (p_99728_) -> {
        this.joinSelectedServer();
    }).width(100).build());
    Button directButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.direct"), (p_296191_) -> {
        this.editingServer = new ServerData(I18n.get("selectServer.defaultName", new Object[0]), "", ServerData.Type.OTHER);
        this.minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, this.editingServer));
    }).width(100).build());
    Button addButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.add"), (p_296190_) -> {
        this.editingServer = new ServerData(I18n.get("selectServer.defaultName", new Object[0]), "", ServerData.Type.OTHER);
        this.minecraft.setScreen(new EditServerScreen(this, this::addServerCallback, this.editingServer));
    }).width(100).build());
    this.editButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.edit"), (p_99715_) -> {
        ServerSelectionList.Entry selectedEntry = (ServerSelectionList.Entry)this.serverSelectionList.getSelected();
        if (selectedEntry instanceof ServerSelectionList.OnlineServerEntry) {
            ServerData serverData = ((ServerSelectionList.OnlineServerEntry)selectedEntry).getServerData();
            this.editingServer = new ServerData(serverData.name, serverData.ip, ServerData.Type.OTHER);
            this.editingServer.copyFrom(serverData);
            this.minecraft.setScreen(new EditServerScreen(this, this::editServerCallback, this.editingServer));
        }

    }).width(74).build());
    this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("selectServer.delete"), (p_99710_) -> {
        ServerSelectionList.Entry selectedEntry = (ServerSelectionList.Entry)this.serverSelectionList.getSelected();
        if (selectedEntry instanceof ServerSelectionList.OnlineServerEntry) {
            String serverName = ((ServerSelectionList.OnlineServerEntry)selectedEntry).getServerData().name;
            Component deleteQuestion = Component.translatable("selectServer.deleteQuestion");
            Component deleteWarning = Component.translatable("selectServer.deleteWarning", new Object[]{serverName});
            Component deleteButton = Component.translatable("selectServer.deleteButton");
            Component cancelButton = CommonComponents.GUI_CANCEL;
            this.minecraft.setScreen(new ConfirmScreen(this::deleteCallback, deleteQuestion, deleteWarning, deleteButton, cancelButton));
        }

    }).width(74).build());
    Button refreshButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("selectServer.refresh"), (p_99706_) -> {
        this.refreshServerList();
    }).width(74).build());
    Button backButton = (Button)this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (p_325384_) -> {
        this.onClose();
    }).width(74).build());
    LinearLayout layout = LinearLayout.vertical();
    EqualSpacingLayout topRowLayout = (EqualSpacingLayout)layout.addChild(new EqualSpacingLayout(308, 20, EqualSpacingLayout.Orientation.HORIZONTAL));
    topRowLayout.addChild(this.selectButton);
    topRowLayout.addChild(directButton);
    topRowLayout.addChild(addButton);
    layout.addChild(SpacerElement.height(4));
    EqualSpacingLayout bottomRowLayout = (EqualSpacingLayout)layout.addChild(new EqualSpacingLayout(308, 20, EqualSpacingLayout.Orientation.HORIZONTAL));
    bottomRowLayout.addChild(this.editButton);
    bottomRowLayout.addChild(this.deleteButton);
    bottomRowLayout.addChild(refreshButton);
    bottomRowLayout.addChild(backButton);
    layout.arrangeElements();
    FrameLayout.centerInRectangle(layout, 0, this.height - 64, this.width, 64);
    this.onSelectedChange();
}

    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    public void tick() {
        super.tick();
        List<LanServer> $$0 = this.lanServerList.takeDirtyServers();
        if ($$0 != null) {
            this.serverSelectionList.updateNetworkServers($$0);
        }

        this.pinger.tick();
    }

    public void removed() {
        if (this.lanServerDetector != null) {
            this.lanServerDetector.interrupt();
            this.lanServerDetector = null;
        }

        this.pinger.removeAll();
        this.serverSelectionList.removed();
    }

    private void refreshServerList() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void deleteCallback(boolean p_99712_) {
        ServerSelectionList.Entry $$1 = (ServerSelectionList.Entry)this.serverSelectionList.getSelected();
        if (p_99712_ && $$1 instanceof ServerSelectionList.OnlineServerEntry) {
            this.servers.remove(((ServerSelectionList.OnlineServerEntry)$$1).getServerData());
            this.servers.save();
            this.serverSelectionList.setSelected(null);
            this.serverSelectionList.updateOnlineServers(this.servers);
        }

        this.minecraft.setScreen(this);
    }

    private void editServerCallback(boolean p_99717_) {
        ServerSelectionList.Entry $$1 = (ServerSelectionList.Entry)this.serverSelectionList.getSelected();
        if (p_99717_ && $$1 instanceof ServerSelectionList.OnlineServerEntry) {
            ServerData $$2 = ((ServerSelectionList.OnlineServerEntry)$$1).getServerData();
            $$2.name = this.editingServer.name;
            $$2.ip = this.editingServer.ip;
            $$2.copyFrom(this.editingServer);
            this.servers.save();
            this.serverSelectionList.updateOnlineServers(this.servers);
        }

        this.minecraft.setScreen(this);
    }

    private void addServerCallback(boolean p_99722_) {
        if (p_99722_) {
            ServerData $$1 = this.servers.unhide(this.editingServer.ip);
            if ($$1 != null) {
                $$1.copyNameIconFrom(this.editingServer);
                this.servers.save();
            } else {
                this.servers.add(this.editingServer, false);
                this.servers.save();
            }

            this.serverSelectionList.setSelected(null);
            this.serverSelectionList.updateOnlineServers(this.servers);
        }

        this.minecraft.setScreen(this);
    }

    private void directJoinCallback(boolean p_99726_) {
        if (p_99726_) {
            ServerData $$1 = this.servers.get(this.editingServer.ip);
            if ($$1 == null) {
                this.servers.add(this.editingServer, true);
                this.servers.save();
                this.join(this.editingServer);
            } else {
                this.join($$1);
            }
        } else {
            this.minecraft.setScreen(this);
        }

    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (pKeyCode == 294) {
            this.refreshServerList();
            return true;
        } else if (this.serverSelectionList.getSelected() != null) {
            if (CommonInputs.selected(pKeyCode)) {
                this.joinSelectedServer();
                return true;
            } else {
                return this.serverSelectionList.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
        } else {
            return false;
        }
    }

    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        //pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
        this.searchBar.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    public void joinSelectedServer() {
        ServerSelectionList.Entry $$0 = (ServerSelectionList.Entry)this.serverSelectionList.getSelected();
        if ($$0 instanceof ServerSelectionList.OnlineServerEntry) {
            this.join(((ServerSelectionList.OnlineServerEntry)$$0).getServerData());
        } else if ($$0 instanceof ServerSelectionList.NetworkServerEntry) {
            LanServer $$1 = ((ServerSelectionList.NetworkServerEntry)$$0).getServerData();
            this.join(new ServerData($$1.getMotd(), $$1.getAddress(), ServerData.Type.LAN));
        }

    }

    private void join(ServerData pServer) {
        ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(pServer.ip), pServer, false, (TransferState)null);
    }

    public void setSelected(ServerSelectionList.Entry pSelected) {
        this.serverSelectionList.setSelected(pSelected);
        this.onSelectedChange();
    }

    protected void onSelectedChange() {
        this.selectButton.active = false;
        this.editButton.active = false;
        this.deleteButton.active = false;
        ServerSelectionList.Entry $$0 = (ServerSelectionList.Entry)this.serverSelectionList.getSelected();
        if ($$0 != null && !($$0 instanceof ServerSelectionList.LANHeader)) {
            this.selectButton.active = true;
            if ($$0 instanceof ServerSelectionList.OnlineServerEntry) {
                this.editButton.active = true;
                this.deleteButton.active = true;
            }
        }
    }

    public ServerStatusPinger getPinger() {
        return this.pinger;
    }

    public ServerList getServers() {
        return this.servers;
    }
}