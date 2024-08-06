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

package net.caffeinemc.mods.sodium.client.render.frapi.helper;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Handles most texture-baking use cases for model loaders and model libraries
 * via {@link #bakeSprite(MutableQuadView, TextureAtlasSprite, int)}. Also used by the API
 * itself to implement automatic block-breaking models for enhanced models.
 */
public class TextureHelper {
    private TextureHelper() { }

    private static final float NORMALIZER = 1f / 16f;

    /**
     * Bakes textures in the provided vertex data, handling UV locking,
     * rotation, interpolation, etc. Textures must not be already baked.
     */
    public static void bakeSprite(MutableQuadView quad, TextureAtlasSprite sprite, int bakeFlags) {
        if (quad.nominalFace() != null && (MutableQuadView.BAKE_LOCK_UV & bakeFlags) != 0) {
            // Assigns normalized UV coordinates based on vertex positions
            applyModifier(quad, UVLOCKERS[quad.nominalFace().get3DDataValue()]);
        } else if ((MutableQuadView.BAKE_NORMALIZED & bakeFlags) == 0) { // flag is NOT set, UVs are assumed to not be normalized yet as is the default, normalize through dividing by 16
            // Scales from 0-16 to 0-1
            applyModifier(quad, (q, i) -> q.uv(i, q.u(i) * NORMALIZER, q.v(i) * NORMALIZER));
        }

        final int rotation = bakeFlags & 3;

        if (rotation != 0) {
            // Rotates texture around the center of sprite.
            // Assumes normalized coordinates.
            applyModifier(quad, ROTATIONS[rotation]);
        }

        if ((MutableQuadView.BAKE_FLIP_U & bakeFlags) != 0) {
            // Inverts U coordinates.  Assumes normalized (0-1) values.
            applyModifier(quad, (q, i) -> q.uv(i, 1 - q.u(i), q.v(i)));
        }

        if ((MutableQuadView.BAKE_FLIP_V & bakeFlags) != 0) {
            // Inverts V coordinates.  Assumes normalized (0-1) values.
            applyModifier(quad, (q, i) -> q.uv(i, q.u(i), 1 - q.v(i)));
        }

        interpolate(quad, sprite);
    }

    /**
     * Faster than sprite method. Sprite computes span and normalizes inputs each call,
     * so we'd have to denormalize before we called, only to have the sprite renormalize immediately.
     */
    private static void interpolate(MutableQuadView q, TextureAtlasSprite sprite) {
        final float uMin = sprite.getU0();
        final float uSpan = sprite.getU1() - uMin;
        final float vMin = sprite.getV0();
        final float vSpan = sprite.getV1() - vMin;

        for (int i = 0; i < 4; i++) {
            q.uv(i, uMin + q.u(i) * uSpan, vMin + q.v(i) * vSpan);
        }
    }

    @FunctionalInterface
    private interface VertexModifier {
        void apply(MutableQuadView quad, int vertexIndex);
    }

    private static void applyModifier(MutableQuadView quad, VertexModifier modifier) {
        for (int i = 0; i < 4; i++) {
            modifier.apply(quad, i);
        }
    }

    private static final VertexModifier[] ROTATIONS = new VertexModifier[] {
            null,
            (q, i) -> q.uv(i, q.v(i), 1 - q.u(i)), //90
            (q, i) -> q.uv(i, 1 - q.u(i), 1 - q.v(i)), //180
            (q, i) -> q.uv(i, 1 - q.v(i), q.u(i)) // 270
    };

    private static final VertexModifier[] UVLOCKERS = new VertexModifier[6];

    static {
        UVLOCKERS[Direction.EAST.get3DDataValue()] = (q, i) -> q.uv(i, 1 - q.z(i), 1 - q.y(i));
        UVLOCKERS[Direction.WEST.get3DDataValue()] = (q, i) -> q.uv(i, q.z(i), 1 - q.y(i));
        UVLOCKERS[Direction.NORTH.get3DDataValue()] = (q, i) -> q.uv(i, 1 - q.x(i), 1 - q.y(i));
        UVLOCKERS[Direction.SOUTH.get3DDataValue()] = (q, i) -> q.uv(i, q.x(i), 1 - q.y(i));
        UVLOCKERS[Direction.DOWN.get3DDataValue()] = (q, i) -> q.uv(i, q.x(i), 1 - q.z(i));
        UVLOCKERS[Direction.UP.get3DDataValue()] = (q, i) -> q.uv(i, q.x(i), q.z(i));
    }
}
