package net.caffeinemc.mods.sodium.neoforge.mixin;

import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AuxiliaryLightManager.class)
public interface AuxiliaryLightManagerMixin extends SodiumAuxiliaryLightManager {
}
