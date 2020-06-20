package me.jellysquid.mods.sodium.mixin.light_unloading;

import me.jellysquid.mods.sodium.client.world.LightStorageExtended;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkLightProvider.class)
public class MixinChunkLightProvider implements LightStorageExtended {
    @Shadow
    @Final
    protected LightStorage lightStorage;

    @Override
    public void runPendingUpdates() {
        if (this.lightStorage instanceof LightStorageExtended) {
            ((LightStorageExtended) this.lightStorage).runPendingUpdates();
        }
    }

    @Override
    public void removeColumnWithoutUpdate(long columnPos) {
        if (this.lightStorage instanceof LightStorageExtended) {
            ((LightStorageExtended) this.lightStorage).removeColumnWithoutUpdate(columnPos);
        }
    }
}
