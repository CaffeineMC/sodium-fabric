package net.caffeinemc.mods.sodium.api.vertex.format;

public interface VertexFormatExtensions {
    /**
     * Returns an integer identifier that represents this vertex format in the global namespace. These identifiers
     * are valid only for the current process lifetime and should not be saved to disk.
     */
    int sodium$getGlobalId();
}
