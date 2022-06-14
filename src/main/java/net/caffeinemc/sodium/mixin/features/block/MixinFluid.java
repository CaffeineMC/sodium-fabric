package net.caffeinemc.sodium.mixin.features.block;

import net.caffeinemc.sodium.render.terrain.color.blender.SodiumColorBlendable;
import net.minecraft.fluid.Fluid;
import org.spongepowered.asm.mixin.Mixin;

/** Injects the {@link SodiumColorBlendable} interface. */
@Mixin(Fluid.class)
public class MixinFluid implements SodiumColorBlendable {}
