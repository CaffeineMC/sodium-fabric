package net.caffeinemc.sodium.interop.vanilla.mixin;

import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.item.ItemStack;

public interface ItemColorProviderRegistry {
    ItemColorProvider getColorProvider(ItemStack stack);
}
