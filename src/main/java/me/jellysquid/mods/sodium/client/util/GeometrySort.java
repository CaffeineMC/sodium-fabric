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
package me.jellysquid.mods.sodium.client.util;

import com.google.common.primitives.Floats;

/**
 * Based upon {@link it.unimi.dsi.fastutil.ints.IntArrays} implementation.
 */
public class GeometrySort {
    private static final int MERGESORT_NO_REC = 16;

    public static void mergeSort(final int[] indices, final float[] distance) {
        mergeSort(indices, 0, indices.length, distance);
    }

    public static void mergeSort(final int[] indices, final int from, final int to, final float[] distance) {
        mergeSort(indices, from, to, distance, indices.clone());
    }

    public static void mergeSort(final int[] indices, final int from, final int to, final float[] distance, final int[] supp) {
        int len = to - from;

        // Insertion sort on smallest arrays
        if (len < MERGESORT_NO_REC) {
            insertionSort(indices, from, to, distance);
            return;
        }

        // Recursively sort halves of a into supp
        final int mid = (from + to) >>> 1;
        mergeSort(supp, from, mid, distance, indices);
        mergeSort(supp, mid, to, distance, indices);

        // If list is already sorted, just copy from supp to indices. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (Floats.compare(distance[supp[mid]], distance[supp[mid - 1]]) <= 0) {
            System.arraycopy(supp, from, indices, from, len);
            return;
        }

        // Merge sorted halves (now in supp) into indices
        int i = from, p = from, q = mid;

        while (i < to) {
            if (q >= to || p < mid && Floats.compare(distance[supp[q]], distance[supp[p]]) <= 0) {
                indices[i] = supp[p++];
            } else {
                indices[i] = supp[q++];
            }

            i++;
        }
    }
    private static void insertionSort(final int[] a, final int from, final int to, final float[] distance) {
        int i = from;

        while (++i < to) {
            int t = a[i];
            int j = i;

            int u = a[j - 1];

            while (Floats.compare(distance[u], distance[t]) < 0) {
                a[j] = u;

                if (from == j - 1) {
                    --j;
                    break;
                }

                u = a[--j - 1];
            }

            a[j] = t;
        }
    }
}
