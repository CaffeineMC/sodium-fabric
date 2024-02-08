/*
 * Copyright (C) 2002-2017 Sebastiano Vigna
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
 *
 *
 *
 * For the sorting and binary search code:
 *
 * Copyright (C) 1999 CERN - European Organization for Nuclear Research.
 *
 *   Permission to use, copy, modify, distribute and sell this software and
 *   its documentation for any purpose is hereby granted without fee,
 *   provided that the above copyright notice appear in all copies and that
 *   both that copyright notice and this permission notice appear in
 *   supporting documentation. CERN makes no representations about the
 *   suitability of this software for any purpose. It is provided "as is"
 *   without expressed or implied warranty.
 */
package net.caffeinemc.mods.sodium.client.util.sorting;


/**
 * Based upon {@link it.unimi.dsi.fastutil.ints.IntArrays} implementation, but it eliminates the use of a user-supplied
 * function and instead sorts an array of floats directly. This helps to improve runtime performance.
 */
public class MergeSort extends AbstractSort {
    private static final int INSERTION_SORT_THRESHOLD = 16;

    public static int[] mergeSort(float[] keys) {
        var indices = createIndexBuffer(keys.length);
        mergeSort(indices, keys);

        return indices;
    }

    private static void mergeSort(final int[] indices, final float[] keys) {
        mergeSort(indices, keys, 0, indices.length, null);
    }

    private static void mergeSort(final int[] indices, final float[] keys, final int fromIndex, final int toIndex, int[] supp) {
        int len = toIndex - fromIndex;

        // Insertion sort on smallest arrays
        if (len < INSERTION_SORT_THRESHOLD) {
            InsertionSort.insertionSort(indices, fromIndex, toIndex, keys);
            return;
        }

        if (supp == null) {
            supp = indices.clone();
        }

        // Recursively sort halves of a into supp
        final int mid = (fromIndex + toIndex) >>> 1;
        mergeSort(supp, keys, fromIndex, mid, indices);
        mergeSort(supp, keys, mid, toIndex, indices);

        // If list is already sorted, just copy from supp to indices. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (keys[supp[mid]] <= keys[supp[mid - 1]]) {
            System.arraycopy(supp, fromIndex, indices, fromIndex, len);
            return;
        }

        // Merge sorted halves (now in supp) into indices
        int i = fromIndex, p = fromIndex, q = mid;

        while (i < toIndex) {
            if (q >= toIndex || p < mid && keys[supp[q]] <= keys[supp[p]]) {
                indices[i] = supp[p++];
            } else {
                indices[i] = supp[q++];
            }

            i++;
        }
    }
}
