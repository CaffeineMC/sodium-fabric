package me.jellysquid.mods.sodium.mixin.light_unloading;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.jellysquid.mods.sodium.client.world.LightStorageExtended;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.LightStorage;
import net.minecraft.world.chunk.light.SkyLightStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SkyLightStorage.class)
public abstract class MixinSkyLightStorage extends LightStorage implements LightStorageExtended {
    @Shadow
    @Final
    private LongSet lightEnabled;

    private MixinSkyLightStorage(LightType type, ChunkProvider chunks, ChunkToNibbleArrayMap light) {
        super(type, chunks, light);
    }

    @Override
    public void runPendingUpdates() {
        this.updateAll();
    }

    @Override
    public void removeColumnWithoutUpdate(long columnPos) {
        this.lightEnabled.remove(columnPos);
    }
}
