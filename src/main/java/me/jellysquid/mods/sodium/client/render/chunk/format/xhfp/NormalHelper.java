package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

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

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

/**
 * Static routines of general utility for renderer implementations.
 * Renderers are not required to use these helpers, but they were
 * designed to be usable without the default renderer.
 */
public abstract class NormalHelper {
	private NormalHelper() { }

	/**
	 * Stores a normal plus an extra value as a quartet of signed bytes.
	 * This is the same normal format that vanilla item rendering expects.
	 * The extra value is for use by shaders.
	 */
	public static int packNormal(float x, float y, float z, float w) {
		x = MathHelper.clamp(x, -1, 1);
		y = MathHelper.clamp(y, -1, 1);
		z = MathHelper.clamp(z, -1, 1);
		w = MathHelper.clamp(w, -1, 1);

		return ((int) (x * 127) & 255) | (((int) (y * 127) & 255) << 8) | (((int) (z * 127) & 255) << 16) | (((int) (w * 127) & 255) << 24);
	}

	/**
	 * Version of {@link #packNormal(float, float, float, float)} that accepts a vector type.
	 */
	public static int packNormal(Vector3f normal, float w) {
		return packNormal(normal.getX(), normal.getY(), normal.getZ(), w);
	}

	/**
	 * Retrieves values packed by {@link #packNormal(float, float, float, float)}.
	 *
	 * <p>Components are x, y, z, w - zero based.
	 */
	public static float getPackedNormalComponent(int packedNormal, int component) {
		return ((byte) (packedNormal >> (8 * component))) / 127f;
	}

	/**
	 * Computes the face normal of the given quad and saves it in the provided non-null vector.
	 *
	 * <p>Will work with triangles also. Assumes counter-clockwise winding order, which is the norm.
	 * Expects convex quads with all points co-planar.
	 */
	public static void computeFaceNormal(@NotNull Vector3f saveTo, QuadView q) {
		//final Direction nominalFace = q.nominalFace();

		/*if (GeometryHelper.isQuadParallelToFace(nominalFace, q)) {
			Vec3i vec = nominalFace.getVector();
			saveTo.set(vec.getX(), vec.getY(), vec.getZ());
			return;
		}*/

		final float x0 = q.x(0);
		final float y0 = q.y(0);
		final float z0 = q.z(0);
		final float x1 = q.x(1);
		final float y1 = q.y(1);
		final float z1 = q.z(1);
		final float x2 = q.x(2);
		final float y2 = q.y(2);
		final float z2 = q.z(2);
		final float x3 = q.x(3);
		final float y3 = q.y(3);
		final float z3 = q.z(3);

		final float dx0 = x2 - x0;
		final float dy0 = y2 - y0;
		final float dz0 = z2 - z0;
		final float dx1 = x3 - x1;
		final float dy1 = y3 - y1;
		final float dz1 = z3 - z1;

		float normX = dy0 * dz1 - dz0 * dy1;
		float normY = dz0 * dx1 - dx0 * dz1;
		float normZ = dx0 * dy1 - dy0 * dx1;

		float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

		if (l != 0) {
			normX /= l;
			normY /= l;
			normZ /= l;
		}

		saveTo.set(normX, normY, normZ);
	}
}

