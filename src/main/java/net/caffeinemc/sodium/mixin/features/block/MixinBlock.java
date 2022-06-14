package net.caffeinemc.sodium.mixin.features.block;

import net.caffeinemc.sodium.render.terrain.color.blender.SodiumColorBlendable;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;

/** Injects the {@link SodiumColorBlendable} interface. */
@Mixin(Block.class)
public class MixinBlock implements SodiumColorBlendable {}
