package me.jellysquid.mods.sodium.client.model.color.interop;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public interface ItemColorsExtended {
    ItemColor sodium$getColorProvider(ItemStack stack);
}
