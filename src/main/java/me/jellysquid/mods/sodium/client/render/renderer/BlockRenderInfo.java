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

package me.jellysquid.mods.sodium.client.render.renderer;

import me.jellysquid.mods.sodium.client.model.quad.QuadColorizer;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.interop.vanilla.colors.BlockColorsExtended;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Holds, manages and provides access to the block/world related state
 * needed by fallback and mesh consumers.
 */
public class BlockRenderInfo {
    protected final BlockColorsExtended blockColors = (BlockColorsExtended) MinecraftClient.getInstance().getBlockColors();
    protected final BlockOcclusionCache blockOcclusionCache;

    private final Random random = new Random();

    public BlockRenderView blockView;
    public BlockPos blockPos;
    public BlockState blockState;

    private long seed;
    private boolean defaultAo;

    private BlendMode defaultLayer;
    private QuadColorizer<BlockState> blockColorProvider;

    private final Supplier<Random> randomSupplier = () -> {
        if (this.seed == -1L) {
            this.seed = this.blockState.getRenderingSeed(this.blockPos);
        }

        final Random random = this.random;
        random.setSeed(this.seed);

        return random;
    };

    public BlockRenderInfo(BlockOcclusionCache blockOcclusionCache) {
        this.blockOcclusionCache = blockOcclusionCache;
    }

    public void setBlockView(BlockRenderView blockView) {
        this.blockView = blockView;
    }

    public void prepareForBlock(BlockState blockState, BlockPos blockPos, boolean modelAO) {
        this.blockPos = blockPos;
        this.blockState = blockState;
        // in the unlikely case seed actually matches this, we'll simply retrieve it more than one
        this.seed = -1L;
        this.defaultAo = modelAO && MinecraftClient.isAmbientOcclusionEnabled() && blockState.getLuminance() == 0;

        this.blockColorProvider = null;
        this.defaultLayer = null;
    }

    Supplier<Random> getRandomSupplier() {
        return this.randomSupplier;
    }

    BlendMode getDefaultLayer() {
        if (this.defaultLayer == null) {
            this.defaultLayer = BlendMode.fromRenderLayer(RenderLayers.getBlockLayer(this.blockState));
        }

        return this.defaultLayer;
    }

    QuadColorizer<BlockState> getColorProvider() {
        if (this.blockColorProvider == null) {
            this.blockColorProvider = this.blockColors.getColorProvider(this.blockState);
        }

        return this.blockColorProvider;
    }

    boolean shouldCullFace(Direction face) {
        return false;
    }

    public boolean getAmbientOcclusionDefault() {
        return this.defaultAo;
    }
}
