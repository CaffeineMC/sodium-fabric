#define _MAT_CUTOUT 0

#define _MAT_CUTOUT_SHIFT 1
#define _MAT_CUTOUT_MASK 3

#define _MAT_CUTOUT_NONE 0
#define _MAT_CUTOUT_HALF 1
#define _MAT_CUTOUT_TENTH 2
#define _MAT_CUTOUT_ZERO 3

float _mat_cutoutThreshold(int flags) {
    switch ((flags >> _MAT_CUTOUT_SHIFT) & _MAT_CUTOUT_MASK) {
        default:
        case _MAT_CUTOUT_NONE: return -1.0;
        case _MAT_CUTOUT_HALF: return 0.5;
        case _MAT_CUTOUT_TENTH: return 0.1;
        case _MAT_CUTOUT_ZERO: return 0.0;
    }
}