package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public interface ItemColorProviderRegistry {
    ItemColor getColorProvider(ItemStack stack);
}
