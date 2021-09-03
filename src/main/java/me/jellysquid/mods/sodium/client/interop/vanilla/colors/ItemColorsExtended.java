package me.jellysquid.mods.sodium.client.interop.vanilla.colors;

import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.item.ItemStack;

public interface ItemColorsExtended {
    ItemColorProvider getColorProvider(ItemStack stack);
}
