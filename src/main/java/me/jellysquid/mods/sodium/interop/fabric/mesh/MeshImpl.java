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

package me.jellysquid.mods.sodium.interop.fabric.mesh;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;

import java.util.function.Consumer;

/**
 * Implementation of {@link Mesh}.
 * The way we encode meshes makes it very simple.
 */
public class MeshImpl implements Mesh {
    /**
     * Used to satisfy external calls to {@link #forEach(Consumer)}.
     */
    final ThreadLocal<QuadViewImpl> POOL = ThreadLocal.withInitial(QuadViewImpl::new);

    final int[] data;

    MeshImpl(int[] data) {
        this.data = data;
    }

    public int[] data() {
        return this.data;
    }

    @Override
    public void forEach(Consumer<QuadView> consumer) {
        this.forEach(consumer, this.POOL.get());
    }

    /**
     * The renderer will call this with it's own cursor
     * to avoid the performance hit of a thread-local lookup.
     * Also means renderer can hold final references to quad buffers.
     */
    void forEach(Consumer<QuadView> consumer, QuadViewImpl cursor) {
        final int limit = this.data.length;
        int index = 0;

        while (index < limit) {
            cursor.load(this.data, index);
            consumer.accept(cursor);
            index += EncodingFormat.TOTAL_STRIDE;
        }
    }
}
