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

package me.jellysquid.mods.sodium.client.render.frapi.helper;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import org.joml.Vector3f;

import static net.minecraft.util.math.MathHelper.approximatelyEquals;

/**
 * Static routines of general utility for renderer implementations.
 * Renderers are not required to use these helpers, but they were
 * designed to be usable without the default renderer.
 */
public abstract class GeometryHelper {
    private GeometryHelper() { }

    /**
     * Returns true if quad is parallel to the given face.
     * Does not validate quad winding order.
     * Expects convex quads with all points co-planar.
     */
    public static boolean isQuadParallelToFace(Direction face, QuadView quad) {
        int i = face.getAxis().ordinal();
        final float val = quad.posByIndex(0, i);
        return approximatelyEquals(val, quad.posByIndex(1, i)) && approximatelyEquals(val, quad.posByIndex(2, i)) && approximatelyEquals(val, quad.posByIndex(3, i));
    }

    /**
     * Identifies the face to which the quad is most closely aligned.
     * This mimics the value that {@link BakedQuad#getFace()} returns, and is
     * used in the vanilla renderer for all diffuse lighting.
     *
     * <p>Derived from the quad face normal and expects convex quads with all points co-planar.
     */
    public static Direction lightFace(QuadView quad) {
        final Vector3f normal = quad.faceNormal();
        switch (GeometryHelper.longestAxis(normal)) {
        case X:
            return normal.x() > 0 ? Direction.EAST : Direction.WEST;

        case Y:
            return normal.y() > 0 ? Direction.UP : Direction.DOWN;

        case Z:
            return normal.z() > 0 ? Direction.SOUTH : Direction.NORTH;

        default:
            // handle WTF case
            return Direction.UP;
        }
    }

    /**
     * @see #longestAxis(float, float, float)
     */
    public static Axis longestAxis(Vector3f vec) {
        return longestAxis(vec.x(), vec.y(), vec.z());
    }

    /**
     * Identifies the largest (max absolute magnitude) component (X, Y, Z) in the given vector.
     */
    public static Axis longestAxis(float normalX, float normalY, float normalZ) {
        Axis result = Axis.Y;
        float longest = Math.abs(normalY);
        float a = Math.abs(normalX);

        if (a > longest) {
            result = Axis.X;
            longest = a;
        }

        return Math.abs(normalZ) > longest
                ? Axis.Z : result;
    }
}
