package net.caffeinemc.sodium.render.terrain.format;

import net.caffeinemc.sodium.render.terrain.format.compact.CompactTerrainVertexType;
import net.caffeinemc.sodium.render.terrain.format.standard.StandardTerrainVertexType;

public class TerrainVertexFormats {
    public static final TerrainVertexType STANDARD = new StandardTerrainVertexType();
    public static final TerrainVertexType COMPACT = new CompactTerrainVertexType();
}
