package net.caffeinemc.sodium.interop.vanilla.math.frustum;

import org.joml.FrustumIntersection;
import org.joml.Math;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * Frustum implementation which extracts planes from a model-view-projection matrix, then allows the usage of skip masks
 * to speed up intersecting tests.
 */
public class JomlFrustum implements Frustum {
    private final float nxX, nxY, nxZ, nxW;
    private final float pxX, pxY, pxZ, pxW;
    private final float nyX, nyY, nyZ, nyW;
    private final float pyX, pyY, pyZ, pyW;
    private final float nzX, nzY, nzZ, nzW;
    private final float pzX, pzY, pzZ, pzW;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    
    /**
     * @param matrix The model-view-projection matrix of the camera
     * @param offset The position of the frustum in the world
     */
    public JomlFrustum(Matrix4fc matrix, Vector3f offset) {
        this.offsetX = offset.x;
        this.offsetY = offset.y;
        this.offsetZ = offset.z;
    
        this.nxX = matrix.m03() + matrix.m00();
        this.nxY = matrix.m13() + matrix.m10();
        this.nxZ = matrix.m23() + matrix.m20();
        this.nxW = matrix.m33() + matrix.m30();
        this.pxX = matrix.m03() - matrix.m00();
        this.pxY = matrix.m13() - matrix.m10();
        this.pxZ = matrix.m23() - matrix.m20();
        this.pxW = matrix.m33() - matrix.m30();
        this.nyX = matrix.m03() + matrix.m01();
        this.nyY = matrix.m13() + matrix.m11();
        this.nyZ = matrix.m23() + matrix.m21();
        this.nyW = matrix.m33() + matrix.m31();
        this.pyX = matrix.m03() - matrix.m01();
        this.pyY = matrix.m13() - matrix.m11();
        this.pyZ = matrix.m23() - matrix.m21();
        this.pyW = matrix.m33() - matrix.m31();
        this.nzX = matrix.m03() + matrix.m02();
        this.nzY = matrix.m13() + matrix.m12();
        this.nzZ = matrix.m23() + matrix.m22();
        this.nzW = matrix.m33() + matrix.m32();
        this.pzX = matrix.m03() - matrix.m02();
        this.pzY = matrix.m13() - matrix.m12();
        this.pzZ = matrix.m23() - matrix.m22();
        this.pzW = matrix.m33() - matrix.m32();
    }
    
    @Override
    public int intersectBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skipMask) {
        return this.intersectBoxInternal(
                minX - this.offsetX,
                minY - this.offsetY,
                minZ - this.offsetZ,
                maxX - this.offsetX,
                maxY - this.offsetY,
                maxZ - this.offsetZ,
                skipMask
        );
    }
    
    private int intersectBoxInternal(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skipMask) {
        /*
         * This is a modified version of what joml uses so a mask
         * can be generated that can be passed down to any aab inside
         * this aab
         */
        int newMask = skipMask;
        if ((skipMask & FrustumIntersection.PLANE_MASK_NX) == 0) {
            float outsideBoundX;
            float outsideBoundY;
            float outsideBoundZ;
            float insideBoundX;
            float insideBoundY;
            float insideBoundZ;
    
            if (this.nxX < 0) {
                outsideBoundX = minX;
                insideBoundX = maxX;
            } else {
                outsideBoundX = maxX;
                insideBoundX = minX;
            }
            
            if (this.nxY < 0) {
                outsideBoundY = minY;
                insideBoundY = maxY;
            } else {
                outsideBoundY = maxY;
                insideBoundY = minY;
            }
    
            if (this.nxZ < 0) {
                outsideBoundZ = minZ;
                insideBoundZ = maxZ;
            } else {
                outsideBoundZ = maxZ;
                insideBoundZ = minZ;
            }
            
            if (Math.fma(this.nxX, outsideBoundX, Math.fma(this.nxY, outsideBoundY, this.nxZ * outsideBoundZ)) < -this.nxW) {
                return OUTSIDE;
            }
            if (Math.fma(this.nxX, insideBoundX, Math.fma(this.nxY, insideBoundY, this.nxZ * insideBoundZ)) >= -this.nxW) {
                newMask |= FrustumIntersection.PLANE_MASK_NX;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_PX) == 0) {
            float outsideBoundX;
            float outsideBoundY;
            float outsideBoundZ;
            float insideBoundX;
            float insideBoundY;
            float insideBoundZ;
    
            if (this.pxX < 0) {
                outsideBoundX = minX;
                insideBoundX = maxX;
            } else {
                outsideBoundX = maxX;
                insideBoundX = minX;
            }
    
            if (this.pxY < 0) {
                outsideBoundY = minY;
                insideBoundY = maxY;
            } else {
                outsideBoundY = maxY;
                insideBoundY = minY;
            }
    
            if (this.pxZ < 0) {
                outsideBoundZ = minZ;
                insideBoundZ = maxZ;
            } else {
                outsideBoundZ = maxZ;
                insideBoundZ = minZ;
            }
    
            if (Math.fma(this.pxX, outsideBoundX, Math.fma(this.pxY, outsideBoundY, this.pxZ * outsideBoundZ)) < -this.pxW) {
                return OUTSIDE;
            }
            if (Math.fma(this.pxX, insideBoundX, Math.fma(this.pxY, insideBoundY, this.pxZ * insideBoundZ)) >= -this.pxW) {
                newMask |= FrustumIntersection.PLANE_MASK_PX;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_NY) == 0) {
            float outsideBoundX;
            float outsideBoundY;
            float outsideBoundZ;
            float insideBoundX;
            float insideBoundY;
            float insideBoundZ;
    
            if (this.nyX < 0) {
                outsideBoundX = minX;
                insideBoundX = maxX;
            } else {
                outsideBoundX = maxX;
                insideBoundX = minX;
            }
    
            if (this.nyY < 0) {
                outsideBoundY = minY;
                insideBoundY = maxY;
            } else {
                outsideBoundY = maxY;
                insideBoundY = minY;
            }
    
            if (this.nyZ < 0) {
                outsideBoundZ = minZ;
                insideBoundZ = maxZ;
            } else {
                outsideBoundZ = maxZ;
                insideBoundZ = minZ;
            }
    
            if (Math.fma(this.nyX, outsideBoundX, Math.fma(this.nyY, outsideBoundY, this.nyZ * outsideBoundZ)) < -this.nyW) {
                return OUTSIDE;
            }
            if (Math.fma(this.nyX, insideBoundX, Math.fma(this.nyY, insideBoundY, this.nyZ * insideBoundZ)) >= -this.nyW) {
                newMask |= FrustumIntersection.PLANE_MASK_NY;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_PY) != 0) {
            float outsideBoundX;
            float outsideBoundY;
            float outsideBoundZ;
            float insideBoundX;
            float insideBoundY;
            float insideBoundZ;
    
            if (this.pyX < 0) {
                outsideBoundX = minX;
                insideBoundX = maxX;
            } else {
                outsideBoundX = maxX;
                insideBoundX = minX;
            }
    
            if (this.pyY < 0) {
                outsideBoundY = minY;
                insideBoundY = maxY;
            } else {
                outsideBoundY = maxY;
                insideBoundY = minY;
            }
    
            if (this.pyZ < 0) {
                outsideBoundZ = minZ;
                insideBoundZ = maxZ;
            } else {
                outsideBoundZ = maxZ;
                insideBoundZ = minZ;
            }
    
            if (Math.fma(this.pyX, outsideBoundX, Math.fma(this.pyY, outsideBoundY, this.pyZ * outsideBoundZ)) < -this.pyW) {
                return OUTSIDE;
            }
            if (Math.fma(this.pyX, insideBoundX, Math.fma(this.pyY, insideBoundY, this.pyZ * insideBoundZ)) >= -this.pyW) {
                newMask |= FrustumIntersection.PLANE_MASK_PY;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_NZ) != 0) {
            float outsideBoundX;
            float outsideBoundY;
            float outsideBoundZ;
            float insideBoundX;
            float insideBoundY;
            float insideBoundZ;
    
            if (this.nzX < 0) {
                outsideBoundX = minX;
                insideBoundX = maxX;
            } else {
                outsideBoundX = maxX;
                insideBoundX = minX;
            }
    
            if (this.nzY < 0) {
                outsideBoundY = minY;
                insideBoundY = maxY;
            } else {
                outsideBoundY = maxY;
                insideBoundY = minY;
            }
    
            if (this.nzZ < 0) {
                outsideBoundZ = minZ;
                insideBoundZ = maxZ;
            } else {
                outsideBoundZ = maxZ;
                insideBoundZ = minZ;
            }
    
            if (Math.fma(this.nzX, outsideBoundX, Math.fma(this.nzY, outsideBoundY, this.nzZ * outsideBoundZ)) < -this.nzW) {
                return OUTSIDE;
            }
            if (Math.fma(this.nzX, insideBoundX, Math.fma(this.nzY, insideBoundY, this.nzZ * insideBoundZ)) >= -this.nzW) {
                newMask |= FrustumIntersection.PLANE_MASK_NZ;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_PZ) != 0) {
            float outsideBoundX;
            float outsideBoundY;
            float outsideBoundZ;
            float insideBoundX;
            float insideBoundY;
            float insideBoundZ;
    
            if (this.pzX < 0) {
                outsideBoundX = minX;
                insideBoundX = maxX;
            } else {
                outsideBoundX = maxX;
                insideBoundX = minX;
            }
    
            if (this.pzY < 0) {
                outsideBoundY = minY;
                insideBoundY = maxY;
            } else {
                outsideBoundY = maxY;
                insideBoundY = minY;
            }
    
            if (this.pzZ < 0) {
                outsideBoundZ = minZ;
                insideBoundZ = maxZ;
            } else {
                outsideBoundZ = maxZ;
                insideBoundZ = minZ;
            }
    
            if (Math.fma(this.pzX, outsideBoundX, Math.fma(this.pzY, outsideBoundY, this.pzZ * outsideBoundZ)) < -this.pzW) {
                return OUTSIDE;
            }
            if (Math.fma(this.pzX, insideBoundX, Math.fma(this.pzY, insideBoundY, this.pzZ * insideBoundZ)) >= -this.pzW) {
                newMask |= FrustumIntersection.PLANE_MASK_PZ;
            }
        }
        return newMask;
    }
    
    @Override
    public boolean containsBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skipMask) {
        return this.containsBoxInternal(
                minX - this.offsetX,
                minY - this.offsetY,
                minZ - this.offsetZ,
                maxX - this.offsetX,
                maxY - this.offsetY,
                maxZ - this.offsetZ,
                skipMask
        );
    }
    
    public boolean containsBoxInternal(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skipMask) {
        /*
         * This is a modified version of what joml uses so a mask can be provided to skip some planes and take advantage
         * of fma intrinsics.
         */
        if ((skipMask & FrustumIntersection.PLANE_MASK_NX) == 0) {
            float outsideBoundX = this.nxX < 0 ? minX : maxX;
            float outsideBoundY = this.nxY < 0 ? minY : maxY;
            float outsideBoundZ = this.nxZ < 0 ? minZ : maxZ;
            
            if (Math.fma(this.nxX, outsideBoundX, Math.fma(this.nxY, outsideBoundY, this.nxZ * outsideBoundZ)) < -this.nxW) {
                return false;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_PX) == 0) {
            float outsideBoundX = this.pxX < 0 ? minX : maxX;
            float outsideBoundY = this.pxY < 0 ? minY : maxY;
            float outsideBoundZ = this.pxZ < 0 ? minZ : maxZ;
            
            if (Math.fma(this.pxX, outsideBoundX, Math.fma(this.pxY, outsideBoundY, this.pxZ * outsideBoundZ)) < -this.pxW) {
                return false;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_NY) == 0) {
            float outsideBoundX = this.nyX < 0 ? minX : maxX;
            float outsideBoundY = this.nyY < 0 ? minY : maxY;
            float outsideBoundZ = this.nyZ < 0 ? minZ : maxZ;
            
            if (Math.fma(this.nyX, outsideBoundX, Math.fma(this.nyY, outsideBoundY, this.nyZ * outsideBoundZ)) < -this.nyW) {
                return false;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_PY) != 0) {
            float outsideBoundX = this.pyX < 0 ? minX : maxX;
            float outsideBoundY = this.pyY < 0 ? minY : maxY;
            float outsideBoundZ = this.pyZ < 0 ? minZ : maxZ;
            
            if (Math.fma(this.pyX, outsideBoundX, Math.fma(this.pyY, outsideBoundY, this.pyZ * outsideBoundZ)) < -this.pyW) {
                return false;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_NZ) != 0) {
            float outsideBoundX = this.nzX < 0 ? minX : maxX;
            float outsideBoundY = this.nzY < 0 ? minY : maxY;
            float outsideBoundZ = this.nzZ < 0 ? minZ : maxZ;
            
            if (Math.fma(this.nzX, outsideBoundX, Math.fma(this.nzY, outsideBoundY, this.nzZ * outsideBoundZ)) < -this.nzW) {
                return false;
            }
        }
        if ((skipMask & FrustumIntersection.PLANE_MASK_PZ) != 0) {
            float outsideBoundX = this.pzX < 0 ? minX : maxX;
            float outsideBoundY = this.pzY < 0 ? minY : maxY;
            float outsideBoundZ = this.pzZ < 0 ? minZ : maxZ;
            
            // last check, just return if true
            return !(Math.fma(this.pzX, outsideBoundX, Math.fma(this.pzY, outsideBoundY, this.pzZ * outsideBoundZ)) < -this.pzW);
        }
        return true;
    }
}
