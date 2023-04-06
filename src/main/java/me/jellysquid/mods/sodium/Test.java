package me.jellysquid.mods.sodium;

import java.util.Arrays;
import java.util.Random;

public class Test {
    public static void main(String[] args) {
        //int id = 1;
        //int scaleBits = 1;
        //int base = (id>>scaleBits)*(1<<(scaleBits+1))+(id&((1<<scaleBits)-1));
        //System.out.println(base);

        int arr[] = new int[256];
        int sorted[] = new int[256];
        Random r = new Random(0);
        for (int t = 0; t < 1; t++) {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = r.nextInt();
            }
            System.arraycopy(arr, 0, sorted, 0, arr.length);
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j <= i; j++) {
                    for (int q = 0; q < 128; q++) {
                        if (j != 0) {
                            localSortB(q, i - j, arr);
                        } else {
                            localSortA(q, i - j, arr);
                        }
                    }
                }

                //System.out.println("______");
            }

            System.out.println(Arrays.toString(arr));
            Arrays.sort(sorted);
            if (!Arrays.equals(arr, sorted)) {
                throw new IllegalStateException();
            }

        }
    }


    private static void localSortA(int id, int scaleBits, int[] arr) {
        int base = (id>>scaleBits)*(1<<(scaleBits+1));
        int offsetA = (id&((1<<scaleBits)-1));
        int offsetB = (1<<(scaleBits+1))-1-offsetA;
        //System.out.println(base+","+offsetA+","+offsetB);

        if (arr[base+offsetA] < arr[base+offsetB]) {
            int tmp = arr[base+offsetA];
            arr[base+offsetA] = arr[base+offsetB];
            arr[base+offsetB] = tmp;
        }
    }

    private static void localSortB(int id, int scaleBits, int[] arr) {
        int base = (id>>scaleBits)*(1<<(scaleBits+1))+(id&((1<<scaleBits)-1));
        int offset = 1<<scaleBits;
        if (arr[base] < arr[base+offset]) {
            int tmp = arr[base];
            arr[base] = arr[base+offset];
            arr[base+offset] = tmp;
        }
    }
}
