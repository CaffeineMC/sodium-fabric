package net.caffeinemc.gfx.api.array;

/**
 * A vertex array defines the way in which vertex attributes for a shader can be sourced from one or more generic
 * buffer objects. The vertex array itself is immutable and only acts as a specification for rendering commands.
 *
 * @param <T> The enumeration over the generic buffer names this vertex array sources from
 */
public interface VertexArray<T extends Enum<T>> {
    T[] getBufferTargets();
}
