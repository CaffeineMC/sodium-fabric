package me.jellysquid.mods.sodium.mixin.light_unloading;

import me.jellysquid.mods.sodium.client.world.ChunkLightUnloadQueue;
import me.jellysquid.mods.sodium.client.world.LightStorageExtended;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

@Mixin(ClientChunkManager.class)
public abstract class MixinClientChunkManager implements ChunkLightUnloadQueue {
    @Shadow
    @Final
    private ClientWorld world;

    @Shadow
    public abstract LightingProvider getLightingProvider();

    private static final int BATCH_SIZE = 16;

    private final Queue<ChunkPos> unloadQueue = new ConcurrentLinkedQueue<>();

    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        List<ChunkPos> batch = this.drainBatch();
        if (batch.isEmpty()) {
            return;
        }

        LightingProvider lightingProvider = this.getLightingProvider();

        LightStorageExtended sky = (LightStorageExtended) lightingProvider.get(LightType.SKY);

        for (ChunkPos column : batch) {
            this.unloadSections(column);
            sky.removeColumnWithoutUpdate(ChunkSectionPos.asLong(column.x, 0, column.z));
        }

        // only run updates after all chunks have been scheduled
        sky.runPendingUpdates();
    }

    private void unloadSections(ChunkPos column) {
        LightingProvider lightingProvider = this.getLightingProvider();

        for (int sectionY = 15; sectionY >= 0; sectionY--) {
            this.world.scheduleBlockRenders(column.x, sectionY, column.z);

            ChunkSectionPos section = ChunkSectionPos.from(column.x, sectionY, column.z);
            lightingProvider.updateSectionStatus(section, true);
        }
    }

    private List<ChunkPos> drainBatch() {
        if (this.unloadQueue.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChunkPos> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < BATCH_SIZE; i++) {
            ChunkPos column = this.unloadQueue.poll();
            if (column == null) break;

            batch.add(column);
        }

        return batch;
    }

    @Override
    public void enqueueUnload(ChunkPos columnPos) {
        this.unloadQueue.add(columnPos);
    }
}
