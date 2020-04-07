package me.jellysquid.mods.sodium.client.render.model.quad;

public interface ModelQuadViewMutable extends ModelQuadView {
    void setX(int idx, float x);

    void setY(int idx, float y);

    void setZ(int idx, float z);

    void setColor(int idx, int color);

    void setTexU(int idx, float u);

    void setTexV(int idx, float v);

    void setLight(int idx, int light);

    void setNormal(int idx, int norm);

    int getLight(int idx);

    int getNormal(int idx);
}
