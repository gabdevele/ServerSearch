package com.gabdevele.serversearch.mixin;

import com.gabdevele.serversearch.ISearchParameter;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(OnlineServerEntry.class)
public class ServerEntryMixin implements ISearchParameter {
    @Unique private boolean serverSearch$searched;

    @Override
    public boolean serverSearch$isSearched() {
        return serverSearch$searched;
    }

    @Override
    public void serverSearch$setSearched(boolean searched) {
        this.serverSearch$searched = searched;
    }

    @Inject(at = @At("HEAD"), method = "swap(II)V", cancellable = true)
    private void gabMod$swap(CallbackInfo info) {
        if(serverSearch$isSearched()) info.cancel();
    }
}