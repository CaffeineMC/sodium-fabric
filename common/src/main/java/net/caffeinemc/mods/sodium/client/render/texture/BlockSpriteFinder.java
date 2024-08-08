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

package net.caffeinemc.mods.sodium.client.render.texture;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Indexes an atlas sprite to allow fast lookup of Sprites from
 * baked vertex coordinates.  Implementation is a straightforward
 * quad tree. Other options that were considered were linear search
 * (slow) and direct indexing of fixed-size cells. Direct indexing
 * would be fastest but would be memory-intensive for large atlases
 * and unsuitable for any atlas that isn't consistently aligned to
 * a fixed cell size.
 */
public class BlockSpriteFinder {
    private final Node root;
    private final TextureAtlas spriteAtlasTexture;
    private int badSpriteCount = 0;

    public BlockSpriteFinder(Map<ResourceLocation, TextureAtlasSprite> sprites, TextureAtlas spriteAtlasTexture) {
        root = new Node(0.5f, 0.5f, 0.25f);
        this.spriteAtlasTexture = spriteAtlasTexture;
        sprites.values().forEach(root::add);
    }

    public TextureAtlasSprite find(float u, float v) {
        return root.find(u, v);
    }

    private class Node {
        static final float EPS = 0.00001f;
        final float midU;
        final float midV;
        final float cellRadius;
        Object lowLow = null;
        Object lowHigh = null;
        Object highLow = null;
        Object highHigh = null;

        Node(float midU, float midV, float radius) {
            this.midU = midU;
            this.midV = midV;
            cellRadius = radius;
        }

        void add(TextureAtlasSprite sprite) {
            if (sprite.getU0() < 0 - EPS || sprite.getU1() > 1 + EPS || sprite.getV0() < 0 - EPS || sprite.getV1() > 1 + EPS) {
                // Sprite has broken bounds. This SHOULD NOT happen, but in the past some mods have broken this.
                // Prefer failing with a log warning rather than risking a stack overflow.
                if (badSpriteCount++ < 5) {
                    String errorMessage = "SpriteFinderImpl: Skipping sprite {} with broken bounds [{}, {}]x[{}, {}]. Sprite bounds should be between 0 and 1.";
                    SodiumClientMod.logger().error(errorMessage, sprite.contents().name(), sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
                }

                return;
            }

            final boolean lowU = sprite.getU0() < midU - EPS;
            final boolean highU = sprite.getU1() > midU + EPS;
            final boolean lowV = sprite.getV0() < midV - EPS;
            final boolean highV = sprite.getV1() > midV + EPS;

            if (lowU && lowV) {
                addInner(sprite, lowLow, -1, -1, q -> lowLow = q);
            }

            if (lowU && highV) {
                addInner(sprite, lowHigh, -1, 1, q -> lowHigh = q);
            }

            if (highU && lowV) {
                addInner(sprite, highLow, 1, -1, q -> highLow = q);
            }

            if (highU && highV) {
                addInner(sprite, highHigh, 1, 1, q -> highHigh = q);
            }
        }

        private void addInner(TextureAtlasSprite sprite, Object quadrant, int uStep, int vStep, Consumer<Object> setter) {
            if (quadrant == null) {
                setter.accept(sprite);
            } else if (quadrant instanceof Node) {
                ((Node) quadrant).add(sprite);
            } else {
                Node n = new Node(midU + cellRadius * uStep, midV + cellRadius * vStep, cellRadius * 0.5f);

                if (quadrant instanceof TextureAtlasSprite) {
                    n.add((TextureAtlasSprite) quadrant);
                }

                n.add(sprite);
                setter.accept(n);
            }
        }

        private TextureAtlasSprite find(float u, float v) {
            if (u < midU) {
                return v < midV ? findInner(lowLow, u, v) : findInner(lowHigh, u, v);
            } else {
                return v < midV ? findInner(highLow, u, v) : findInner(highHigh, u, v);
            }
        }

        private TextureAtlasSprite findInner(Object quadrant, float u, float v) {
            if (quadrant instanceof TextureAtlasSprite) {
                return (TextureAtlasSprite) quadrant;
            } else if (quadrant instanceof Node) {
                return ((Node) quadrant).find(u, v);
            } else {
                return spriteAtlasTexture.getSprite(MissingTextureAtlasSprite.getLocation());
            }
        }
    }
}
