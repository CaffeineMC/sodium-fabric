/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package me.jellysquid.mods.sodium.render.entity.part;

import org.jetbrains.annotations.Nullable;

public interface BakeablePartBuilder {
    void setId(int id);
    int getId();

    void setParent(BakeablePartBuilder parent);
    @Nullable BakeablePartBuilder getParent();

    int getNextAvailableModelId();
}
