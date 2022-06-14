package net.caffeinemc.sodium.render.terrain.color.blender;

/**
 * Defines an object that supports color blending in sodium.
 */
public interface SodiumColorBlendable {
    /**
     * Returns a value indicating if to enable color blending for this object.
     */
    default boolean enableSodiumColorBlending() {
        return true;
    }
}
