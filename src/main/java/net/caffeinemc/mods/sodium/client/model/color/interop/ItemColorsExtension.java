package net.caffeinemc.mods.sodium.client.model.color.interop;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public interface ItemColorsExtension {
    ItemColor sodium$getColorProvider(ItemStack stack);
}
