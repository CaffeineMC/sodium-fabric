package net.caffeinemc.sodium.render.chunk.draw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HeightLimitView;

public class ChunkCameraContext {
    private final MinecraftClient client;
    private final Camera camera;
    private final int sectionHeightMin;
    private final int sectionHeightMax;

    public ChunkCameraContext(MinecraftClient client) {
        this.client = client;
        this.camera = client.gameRenderer.getCamera();
        HeightLimitView heightLimitView = client.world;
        
        if (heightLimitView == null) {
            throw new IllegalStateException("World doesn't exist");
        }
        
        this.sectionHeightMin = heightLimitView.getBottomSectionCoord();
        this.sectionHeightMax = heightLimitView.getTopSectionCoord() - 1;
    }
    
    public boolean isCameraInitialized() {
//        return this.camera.getFocusedEntity() == null;
        return this.camera.isReady();
    }
    
    private void updateIfNeeded() {
//        Entity newFocusedEntity = this.client.getCameraEntity() == null ? this.client.player : this.client.getCameraEntity();
//        if (this.camera.getFocusedEntity() == null && newFocusedEntity != null) {
//            // attempt an update on the camera
//            this.camera.update(
//                    this.client.world,
//                    newFocusedEntity,
//                    !this.client.options.getPerspective().isFirstPerson(),
//                    this.client.options.getPerspective().isFrontView(),
//                    this.client.getTickDelta()
//            );
//        }
    }
    
    public BlockPos getBlockPos() {
        this.updateIfNeeded();
        return this.camera.getBlockPos();
    }
    
    public Vec3d getPos() {
        this.updateIfNeeded();
        return this.camera.getPos();
    }
    
    public int getBlockX() {
        return this.getBlockPos().getX();
    }
    
    public int getBlockY() {
        return this.getBlockPos().getY();
    }
    
    public int getBlockZ() {
        return this.getBlockPos().getZ();
    }
    
    public int getSectionX() {
        return ChunkSectionPos.getSectionCoord(this.getBlockX());
    }

    public int getSectionY() {
        return ChunkSectionPos.getSectionCoord(this.getBlockY());
    }
    
    public int getSectionYClamped() {
        // effectively clamps, but because this method is extremely hot, we use the intrinsics for min and max rather
        // than typical branching with MathHelper.clamp
        return Math.max(
                this.sectionHeightMin,
                Math.min(
                        this.sectionHeightMax,
                        ChunkSectionPos.getSectionCoord(this.getBlockY())
                )
        );
    }
    
    public int getSectionZ() {
        return ChunkSectionPos.getSectionCoord(this.getBlockZ());
    }
    
    
    // Reduce camera delta precision to 14 bits to avoid seams along chunk/region boundaries
    public float getDeltaX() {
        this.updateIfNeeded();
        return (float) Math.round((this.camera.getPos().getX() - this.camera.getBlockPos().getX()) * 0x1p14) * 0x1p-14f;
    }
    
    public float getDeltaY() {
        this.updateIfNeeded();
        return (float) Math.round((this.camera.getPos().getY() - this.camera.getBlockPos().getY()) * 0x1p14) * 0x1p-14f;
    }
    
    public float getDeltaZ() {
        this.updateIfNeeded();
        return (float) Math.round((this.camera.getPos().getZ() - this.camera.getBlockPos().getZ()) * 0x1p14) * 0x1p-14f;
    }
    
    public double getPosX() {
        return this.getPos().getX();
    }
    
    public double getPosY() {
        return this.getPos().getY();
    }
    
    public double getPosZ() {
        return this.getPos().getZ();
    }
    
}
