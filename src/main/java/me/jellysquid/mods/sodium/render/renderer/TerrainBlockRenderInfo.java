/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.jellysquid.mods.sodium.render.renderer;

import me.jellysquid.mods.sodium.render.occlusion.BlockOcclusionCache;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class TerrainBlockRenderInfo extends BlockRenderInfo {
    private int cullCompletionFlags;
    private int cullResultFlags;

    private final BlockPos.Mutable relativeBlockPosition = new BlockPos.Mutable();
    private int chunkId;

    public TerrainBlockRenderInfo(BlockOcclusionCache blockOcclusionCache) {
        super(blockOcclusionCache);
    }

    @Override
    public void prepareForBlock(BlockState blockState, BlockPos blockPos, boolean modelAO) {
        super.prepareForBlock(blockState, blockPos, modelAO);

        this.cullCompletionFlags = 0;
        this.cullResultFlags = 0;

        this.relativeBlockPosition.set(
                this.blockPos.getX() & 15,
                this.blockPos.getY() & 15,
                this.blockPos.getZ() & 15
        );
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
    }

    @Override
    boolean shouldCullFace(Direction face) {
        if (face == null) {
            return false;
        }

        final int mask = (1 << face.getId());

        if ((this.cullCompletionFlags & mask) == 0) {
            if (this.blockOcclusionCache.shouldDrawSide(this.blockState, this.blockView, this.blockPos, face)) {
                this.cullResultFlags |= mask;
            }

            this.cullCompletionFlags |= mask;
        }

        return (this.cullResultFlags & mask) == 0;
    }

    public Vec3i getRelativeBlockPosition() {
        return this.relativeBlockPosition;
    }

    public int getChunkId() {
        return this.chunkId;
    }
}
