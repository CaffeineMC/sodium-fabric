package net.caffeinemc.mods.sodium.neoforge.mixin;

import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelData.class)
public class ModelDataMixin implements SodiumModelData {
}
