package com.gabdevele.serversearch.mixin;

import com.gabdevele.serversearch.ISearchParameter;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerData.class)
public class ServerDataMixin implements ISearchParameter {
    @Unique
    private boolean serverSearch$searched;

    @Override
    public boolean serverSearch$isSearched() {
        return serverSearch$searched;
    }

    @Override
    public void serverSearch$setSearched(boolean searched) {
        this.serverSearch$searched = searched;
    }
}
