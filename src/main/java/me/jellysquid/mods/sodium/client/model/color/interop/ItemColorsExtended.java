package me.jellysquid.mods.sodium.client.model.color.interop;

import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.item.ItemStack;

public interface ItemColorsExtended {
    ItemColorProvider sodium$getColorProvider(ItemStack stack);
}
