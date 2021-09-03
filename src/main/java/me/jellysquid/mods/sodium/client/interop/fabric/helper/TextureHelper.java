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

package me.jellysquid.mods.sodium.client.interop.fabric.helper;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

/**
 * Handles most texture-baking use cases for model loaders and model libraries
 * via {@link #bakeSprite(MutableQuadView, int, Sprite, int)}. Also used by the API
 * itself to implement automatic block-breaking models for enhanced models.
 */
public class TextureHelper {
    private TextureHelper() {
    }

    private static final float NORMALIZER = 1f / 16f;

    /**
     * Bakes textures in the provided vertex data, handling UV locking,
     * rotation, interpolation, etc. Textures must not be already baked.
     */
    public static void bakeSprite(MutableQuadView quad, int spriteIndex, Sprite sprite, int bakeFlags) {
        if (quad.nominalFace() != null && (MutableQuadView.BAKE_LOCK_UV & bakeFlags) != 0) {
            // Assigns normalized UV coordinates based on vertex positions
            applyModifier(quad, spriteIndex, UVLOCKERS[quad.nominalFace().getId()]);
        } else if ((MutableQuadView.BAKE_NORMALIZED & bakeFlags) == 0) {
            // Scales from 0-16 to 0-1
            applyModifier(quad, spriteIndex, (q, i, t) -> q.sprite(i, t, q.spriteU(i, t) * NORMALIZER, q.spriteV(i, t) * NORMALIZER));
        }

        final int rotation = bakeFlags & 3;

        if (rotation != 0) {
            // Rotates texture around the center of sprite.
            // Assumes normalized coordinates.
            applyModifier(quad, spriteIndex, ROTATIONS[rotation]);
        }

        if ((MutableQuadView.BAKE_FLIP_U & bakeFlags) != 0) {
            // Inverts U coordinates.  Assumes normalized (0-1) values.
            applyModifier(quad, spriteIndex, (q, i, t) -> q.sprite(i, t, 1 - q.spriteU(i, t), q.spriteV(i, t)));
        }

        if ((MutableQuadView.BAKE_FLIP_V & bakeFlags) != 0) {
            // Inverts V coordinates.  Assumes normalized (0-1) values.
            applyModifier(quad, spriteIndex, (q, i, t) -> q.sprite(i, t, q.spriteU(i, t), 1 - q.spriteV(i, t)));
        }

        interpolate(quad, spriteIndex, sprite);
    }

    /**
     * Faster than sprite method. Sprite computes span and normalizes inputs each call,
     * so we'd have to denormalize before we called, only to have the sprite renormalize immediately.
     */
    private static void interpolate(MutableQuadView q, int spriteIndex, Sprite sprite) {
        final float uMin = sprite.getMinU();
        final float uSpan = sprite.getMaxU() - uMin;
        final float vMin = sprite.getMinV();
        final float vSpan = sprite.getMaxV() - vMin;

        for (int i = 0; i < 4; i++) {
            q.sprite(i, spriteIndex, uMin + q.spriteU(i, spriteIndex) * uSpan, vMin + q.spriteV(i, spriteIndex) * vSpan);
        }
    }

    @FunctionalInterface
    private interface VertexModifier {
        void apply(MutableQuadView quad, int vertexIndex, int spriteIndex);
    }

    private static void applyModifier(MutableQuadView quad, int spriteIndex, VertexModifier modifier) {
        for (int i = 0; i < 4; i++) {
            modifier.apply(quad, i, spriteIndex);
        }
    }

    private static final VertexModifier[] ROTATIONS = new VertexModifier[] { null, (q, i, t) -> q.sprite(i, t, q.spriteV(i, t), q.spriteU(i, t)), //90
            (q, i, t) -> q.sprite(i, t, 1 - q.spriteU(i, t), 1 - q.spriteV(i, t)), //180
            (q, i, t) -> q.sprite(i, t, 1 - q.spriteV(i, t), q.spriteU(i, t)) // 270
    };

    private static final VertexModifier[] UVLOCKERS = new VertexModifier[6];

    static {
        UVLOCKERS[Direction.EAST.getId()] = (q, i, t) -> q.sprite(i, t, 1 - q.z(i), 1 - q.y(i));
        UVLOCKERS[Direction.WEST.getId()] = (q, i, t) -> q.sprite(i, t, q.z(i), 1 - q.y(i));
        UVLOCKERS[Direction.NORTH.getId()] = (q, i, t) -> q.sprite(i, t, 1 - q.x(i), 1 - q.y(i));
        UVLOCKERS[Direction.SOUTH.getId()] = (q, i, t) -> q.sprite(i, t, q.x(i), 1 - q.y(i));
        UVLOCKERS[Direction.DOWN.getId()] = (q, i, t) -> q.sprite(i, t, q.x(i), 1 - q.z(i));
        UVLOCKERS[Direction.UP.getId()] = (q, i, t) -> q.sprite(i, t, q.x(i), q.z(i));
    }
}
