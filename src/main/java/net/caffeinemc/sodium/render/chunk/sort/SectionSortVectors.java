/*
 * The MIT License
 *
 * Copyright (c) 2015-2021 Richard Greenlees
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.caffeinemc.sodium.render.chunk.sort;

import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Math;
import org.joml.Vector3fc;

public class SectionSortVectors {
    private final double blockX, blockY, blockZ;
    
    // make a vector for each corner of the chunk to the camera position it was last sorted at
    private float c0x;
    private float c0y;
    private float c0z;

    private float c1x;
    private float c1y;
    private float c1z;

    private float c2x;
    private float c2y;
    private float c2z;

    private float c3x;
    private float c3y;
    private float c3z;

    private float c4x;
    private float c4y;
    private float c4z;

    private float c5x;
    private float c5y;
    private float c5z;

    private float c6x;
    private float c6y;
    private float c6z;

    private float c7x;
    private float c7y;
    private float c7z;
    
    private boolean hasRun;
    
    public SectionSortVectors(int sectionX, int sectionY, int sectionZ) {
        this.blockX = ChunkSectionPos.getBlockCoord(sectionX);
        this.blockY = ChunkSectionPos.getBlockCoord(sectionY);
        this.blockZ = ChunkSectionPos.getBlockCoord(sectionZ);
    }
    
    public boolean compareAndSet(ChunkCameraContext camera, float angleCosThreshold) {
        float n0x = ((float) (this.blockX - camera.posX));
        float n0y = ((float) (this.blockY - camera.posY));
        float n0z = ((float) (this.blockZ - camera.posZ));
        
        float n1x = ((float) (this.blockX - camera.posX + 16.0));
        float n1y = ((float) (this.blockY - camera.posY));
        float n1z = ((float) (this.blockZ - camera.posZ));
        
        float n2x = ((float) (this.blockX - camera.posX));
        float n2y = ((float) (this.blockY - camera.posY));
        float n2z = ((float) (this.blockZ - camera.posZ + 16.0));
    
        float n3x = ((float) (this.blockX - camera.posX + 16.0));
        float n3y = ((float) (this.blockY - camera.posY));
        float n3z = ((float) (this.blockZ - camera.posZ + 16.0));
        
        float n4x = ((float) (this.blockX - camera.posX));
        float n4y = ((float) (this.blockY - camera.posY + 16.0));
        float n4z = ((float) (this.blockZ - camera.posZ));
    
        float n5x = ((float) (this.blockX - camera.posX + 16.0));
        float n5y = ((float) (this.blockY - camera.posY + 16.0));
        float n5z = ((float) (this.blockZ - camera.posZ));
    
        float n6x = ((float) (this.blockX - camera.posX));
        float n6y = ((float) (this.blockY - camera.posY + 16.0));
        float n6z = ((float) (this.blockZ - camera.posZ + 16.0));
    
        float n7x = ((float) (this.blockX - camera.posX + 16.0));
        float n7y = ((float) (this.blockY - camera.posY + 16.0));
        float n7z = ((float) (this.blockZ - camera.posZ + 16.0));
        
        if (!this.hasRun ||
            angleCos(this.c0x, this.c0y, this.c0z, n0x, n0y, n0z) < angleCosThreshold ||
            angleCos(this.c1x, this.c1y, this.c1z, n1x, n1y, n1z) < angleCosThreshold ||
            angleCos(this.c2x, this.c2y, this.c2z, n2x, n2y, n2z) < angleCosThreshold ||
            angleCos(this.c3x, this.c3y, this.c3z, n3x, n3y, n3z) < angleCosThreshold ||
            angleCos(this.c4x, this.c4y, this.c4z, n4x, n4y, n4z) < angleCosThreshold ||
            angleCos(this.c5x, this.c5y, this.c5z, n5x, n5y, n5z) < angleCosThreshold ||
            angleCos(this.c6x, this.c6y, this.c6z, n6x, n6y, n6z) < angleCosThreshold ||
            angleCos(this.c7x, this.c7y, this.c7z, n7x, n7y, n7z) < angleCosThreshold) {
            
            this.hasRun = true;
            
            this.c0x = n0x;
            this.c0y = n0y;
            this.c0z = n0z;

            this.c1x = n1x;
            this.c1y = n1y;
            this.c1z = n1z;

            this.c2x = n2x;
            this.c2y = n2y;
            this.c2z = n2z;

            this.c3x = n3x;
            this.c3y = n3y;
            this.c3z = n3z;

            this.c4x = n4x;
            this.c4y = n4y;
            this.c4z = n4z;

            this.c5x = n5x;
            this.c5y = n5y;
            this.c5z = n5z;

            this.c6x = n6x;
            this.c6y = n6y;
            this.c6z = n6z;

            this.c7x = n7x;
            this.c7y = n7y;
            this.c7z = n7z;
            
            return true;
        }
        
        return false;
    }
    
    /**
     * An adaptation to JOML's {@link org.joml.Vector3f#angleCos(Vector3fc)} which has primitive
     * inputs for inlining and auto-vectorization.
     */
    private static float angleCos(float v1x, float v1y, float v1z, float v2x, float v2y, float v2z) {
        float length1Squared = Math.fma(v1x, v1x, Math.fma(v1y, v1y, v1z * v1z));
        float length2Squared = Math.fma(v2x, v2x, Math.fma(v2y, v2y, v2z * v2z));
        float dot = Math.fma(v1x, v2x, Math.fma(v1y, v2y, v1z * v2z));
        return dot / Math.sqrt(length1Squared * length2Squared);
    }
}
