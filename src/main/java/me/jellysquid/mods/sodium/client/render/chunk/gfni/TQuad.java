package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

/**
 * Represents a quad for the purposes of translucency sorting. Called TQuad to
 * avoid confusion with other quad classes.
 */
record TQuad(ModelQuadFacing facing, Vector3fc normal, Vector3f center, float[] extents) {
}
