package net.caffeinemc.sodium.render.entity.draw;

public interface EntityRenderer {

    void render(int frameIndex);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();
}
