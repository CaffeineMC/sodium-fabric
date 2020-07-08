package me.jellysquid.mods.sodium.mixin.item_rendering;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.biome.ItemColorsExtended;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemColors.class)
public class MixinItemColors implements ItemColorsExtended {
    private Reference2ReferenceMap<ItemConvertible, ItemColorProvider> itemsToColor;

    private static final ItemColorProvider DEFAULT_PROVIDER = (stack, tintIdx) -> -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.itemsToColor = new Reference2ReferenceOpenHashMap<>();
        this.itemsToColor.defaultReturnValue(DEFAULT_PROVIDER);
    }

    @Inject(method = "register", at = @At("HEAD"))
    private void preRegisterColor(ItemColorProvider mapper, ItemConvertible[] convertibles, CallbackInfo ci) {
        for (ItemConvertible convertible : convertibles) {
            this.itemsToColor.put(convertible.asItem(), mapper);
        }
    }

    @Override
    public ItemColorProvider getColorProvider(ItemStack stack) {
        return this.itemsToColor.get(stack.getItem());
    }
}
