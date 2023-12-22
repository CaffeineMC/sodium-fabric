package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Models a partition of the space into a set of quads that lie inside or on the
 * plane with the specified distance. If the distance is -1 this is the "end"
 * partition after the last partition plane.
 */
record Partition(float distance, IntArrayList quadsBefore, IntArrayList quadsOn) {
}
