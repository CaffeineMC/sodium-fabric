package net.caffeinemc.sodium.render.chunk.sort;

import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class SectionSortVectors {
    private final double blockX, blockY, blockZ;
    
    // make a vector for each corner of the chunk to the camera position it was last sorted at
    private final Vector3f c0, c1, c2, c3, c4, c5, c6, c7;
    
    private boolean hasRun;
    
    public SectionSortVectors(int sectionX, int sectionY, int sectionZ) {
        this.blockX = ChunkSectionPos.getBlockCoord(sectionX);
        this.blockY = ChunkSectionPos.getBlockCoord(sectionY);
        this.blockZ = ChunkSectionPos.getBlockCoord(sectionZ);
        
        this.c0 = new Vector3f();
        this.c1 = new Vector3f();
        this.c2 = new Vector3f();
        this.c3 = new Vector3f();
        this.c4 = new Vector3f();
        this.c5 = new Vector3f();
        this.c6 = new Vector3f();
        this.c7 = new Vector3f();
    }
    
    public boolean compareAndSet(ChunkCameraContext camera, float angleCosThreshold) {
        Vector3fc n0 = new Vector3f(
                (float) (this.blockX - camera.posX),
                (float) (this.blockY - camera.posY),
                (float) (this.blockZ - camera.posZ)
        );
        
        Vector3fc n1 = new Vector3f(
                (float) (this.blockX - camera.posX + 16.0),
                (float) (this.blockY - camera.posY),
                (float) (this.blockZ - camera.posZ)
        );
        
        Vector3fc n2 = new Vector3f(
                (float) (this.blockX - camera.posX),
                (float) (this.blockY - camera.posY),
                (float) (this.blockZ - camera.posZ + 16.0)
        );
    
        Vector3fc n3 = new Vector3f(
                (float) (this.blockX - camera.posX + 16.0),
                (float) (this.blockY - camera.posY),
                (float) (this.blockZ - camera.posZ + 16.0)
        );
        
        Vector3fc n4 = new Vector3f(
                (float) (this.blockX - camera.posX),
                (float) (this.blockY - camera.posY + 16.0),
                (float) (this.blockZ - camera.posZ)
        );
    
        Vector3fc n5 = new Vector3f(
                (float) (this.blockX - camera.posX + 16.0),
                (float) (this.blockY - camera.posY + 16.0),
                (float) (this.blockZ - camera.posZ)
        );
    
        Vector3fc n6 = new Vector3f(
                (float) (this.blockX - camera.posX),
                (float) (this.blockY - camera.posY + 16.0),
                (float) (this.blockZ - camera.posZ + 16.0)
        );
    
        Vector3fc n7 = new Vector3f(
                (float) (this.blockX - camera.posX + 16.0),
                (float) (this.blockY - camera.posY + 16.0),
                (float) (this.blockZ - camera.posZ + 16.0)
        );
        
        if (!this.hasRun ||
            this.c0.angleCos(n0) < angleCosThreshold ||
            this.c1.angleCos(n1) < angleCosThreshold ||
            this.c2.angleCos(n2) < angleCosThreshold ||
            this.c3.angleCos(n3) < angleCosThreshold ||
            this.c4.angleCos(n4) < angleCosThreshold ||
            this.c5.angleCos(n5) < angleCosThreshold ||
            this.c6.angleCos(n6) < angleCosThreshold ||
            this.c7.angleCos(n7) < angleCosThreshold) {
            
            this.hasRun = true;
            this.c0.set(n0);
            this.c1.set(n1);
            this.c2.set(n2);
            this.c3.set(n3);
            this.c4.set(n4);
            this.c5.set(n5);
            this.c6.set(n6);
            this.c7.set(n7);
            
            return true;
        }
        
        return false;
    }
}
