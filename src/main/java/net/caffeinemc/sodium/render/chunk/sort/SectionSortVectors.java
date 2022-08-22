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

import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

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
    
    public boolean compareAndSet(Vec3d cameraPos, float angleCosThreshold) {
        float nx0 = (float) (this.blockX - cameraPos.getX());
        float nx1 = (float) (this.blockX - cameraPos.getX() + 16.0);
        
        float ny0 = (float) (this.blockY - cameraPos.getY());
        float ny1 = (float) (this.blockY - cameraPos.getY() + 16.0);
        
        float nz0 = (float) (this.blockZ - cameraPos.getZ());
        float nz1 = (float) (this.blockZ - cameraPos.getZ() + 16.0);
    
        if (!this.hasRun ||
            MathUtil.angleCos(this.c0x, this.c0y, this.c0z, nx0, ny0, nz0) < angleCosThreshold ||
            MathUtil.angleCos(this.c1x, this.c1y, this.c1z, nx1, ny0, nz0) < angleCosThreshold ||
            MathUtil.angleCos(this.c2x, this.c2y, this.c2z, nx0, ny0, nz1) < angleCosThreshold ||
            MathUtil.angleCos(this.c3x, this.c3y, this.c3z, nx1, ny0, nz1) < angleCosThreshold ||
            MathUtil.angleCos(this.c4x, this.c4y, this.c4z, nx0, ny1, nz0) < angleCosThreshold ||
            MathUtil.angleCos(this.c5x, this.c5y, this.c5z, nx1, ny1, nz0) < angleCosThreshold ||
            MathUtil.angleCos(this.c6x, this.c6y, this.c6z, nx0, ny1, nz1) < angleCosThreshold ||
            MathUtil.angleCos(this.c7x, this.c7y, this.c7z, nx1, ny1, nz1) < angleCosThreshold) {
            
            this.hasRun = true;
            
            this.c0x = nx0;
            this.c0y = ny0;
            this.c0z = nz0;

            this.c1x = nx1;
            this.c1y = ny0;
            this.c1z = nz0;

            this.c2x = nx0;
            this.c2y = ny0;
            this.c2z = nz1;

            this.c3x = nx1;
            this.c3y = ny0;
            this.c3z = nz1;

            this.c4x = nx0;
            this.c4y = ny1;
            this.c4z = nz0;

            this.c5x = nx1;
            this.c5y = ny1;
            this.c5z = nz0;

            this.c6x = nx0;
            this.c6y = ny1;
            this.c6z = nz1;

            this.c7x = nx1;
            this.c7y = ny1;
            this.c7z = nz1;
            
            return true;
        }
        
        return false;
    }
}
