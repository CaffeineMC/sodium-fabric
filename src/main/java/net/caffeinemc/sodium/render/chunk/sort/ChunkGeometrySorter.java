package net.caffeinemc.sodium.render.chunk.sort;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkGeometrySorter {
    private final RenderDevice device;
    private final float angleThreshold;
    private final Long2ObjectMap<Fence> existingSorts;
    
    public ChunkGeometrySorter(RenderDevice device, float angleThreshold) {
        this.device = device;
        this.angleThreshold = angleThreshold;
        // make an estimate for size based on inputs (render distance?)
        this.existingSorts = new Long2ObjectOpenHashMap<>();
    }
    
    public void sortGeometry(List<RenderSection> sortedSections, ChunkCameraContext camera) {
        for (RenderSection section : sortedSections) {
            long sectionPos = ChunkSectionPos.asLong(section.getChunkX(), section.getChunkY(), section.getChunkZ());
            Fence existingSort = this.existingSorts.get(sectionPos);
            
            if (existingSort != null && !existingSort.poll()) {
                // sort is not done, queue for later?
                continue;
            }
            
            boolean requiresSort = section.getSortVectors().compareAndSet(camera, this.angleThreshold);
            
            if (requiresSort) {
                this.existingSorts.put(sectionPos, this.device.createFence());
                
                SodiumClientMod.logger()
                               .info("Sort {}initiated for {}, {}, {}",
                                     existingSort != null ? "re" : "",
                                     section.getChunkX(),
                                     section.getChunkY(),
                                     section.getChunkZ()
                               );
            }
        }
    }
}
