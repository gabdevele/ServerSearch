package com.gabdevele.serversearch.mixin;

import com.gabdevele.serversearch.ISearchParameter;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerSelectionList.class)
public class ServerSelectionMixin implements ISearchParameter {
    @Shadow @Final
    private List<ServerSelectionList.OnlineServerEntry> onlineServers;
    @Mutable @Final @Shadow static Component SCANNING_LABEL;
    @Unique private boolean serverSearch$searching = false;

    @Inject( method = "updateOnlineServers", at = @At("TAIL"))
    public void updateOnlineServers(ServerList pServers, CallbackInfo info) {
        List<ServerSelectionList.OnlineServerEntry> copy = this.onlineServers;
        for (int i = 0; i < pServers.size(); ++i) {
            ((ISearchParameter) copy.get(i)).serverSearch$setSearched(((ISearchParameter) pServers.get(i)).serverSearch$isSearched());
        }
        this.onlineServers.clear();
        this.onlineServers.addAll(copy);
    }

    @Override
    public boolean serverSearch$isSearched() {
        return serverSearch$searching;
    }

    @Override
    public void serverSearch$setSearched(boolean searching) {
        serverSearch$searching = searching;
        if(searching){
            SCANNING_LABEL = Component.translatable("serverSearch.searching");
        } else {
            SCANNING_LABEL = Component.translatable("lanServer.scanning");
        }
    }
}