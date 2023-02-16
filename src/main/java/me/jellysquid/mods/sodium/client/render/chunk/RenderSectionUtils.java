package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.util.BitArray;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;

public class RenderSectionUtils {
    private final int idxYShift;
    private final int idxZShift;
    private final int idxYMask;
    private final int idxZMask;
    private final int idxXMask;
    private final int sectionWidthMask;
    private final int sectionHeightOffset;

    private final int tblSize;

    private final long[] frustumChecks;
    private final long[] visibilityCheck;
    public RenderSectionUtils(int viewDistance, HeightLimitView heightLimitView) {
        int sectionWidth = MathHelper.smallestEncompassingPowerOfTwo(viewDistance * 2 + 1);
        int sectionWidthOffset = viewDistance;
        int sectionWidthSquared = sectionWidth * sectionWidth;

        sectionWidthMask = sectionWidth - 1;
        this.idxZShift = Integer.numberOfTrailingZeros(sectionWidth);
        this.idxYShift = this.idxZShift * 2;
        this.idxYMask = -(1 << this.idxYShift);
        this.idxXMask = sectionWidthMask;
        this.idxZMask = sectionWidthMask << this.idxZShift;

        int sectionHeight = heightLimitView.countVerticalSections();
        sectionHeightOffset = -heightLimitView.getBottomSectionCoord();

        tblSize = sectionWidthSquared * sectionHeight;

        frustumChecks = BitArray.create(tblSize*2);
        visibilityCheck = BitArray.create(tblSize);
    }

    public int getSectionId(int x, int y, int z) {
        int tableY = y + this.sectionHeightOffset;
        int tableZ = z & this.sectionWidthMask;
        int tableX = x & this.sectionWidthMask;
        return (tableY << this.idxYShift)
                | (tableZ << this.idxZShift)
                | tableX;
    }

    private Frustum frustum;

    public void setFrustum(Frustum frustum) {
        this.frustum = frustum;
    }

    public void clear() {
        BitArray.clear(frustumChecks);
        BitArray.clear(visibilityCheck);
    }

    public void setVisible(int x, int y, int z) {
        BitArray.set(visibilityCheck, getSectionId(x,y,z));
    }

    public boolean isVisible(int x, int y, int z) {
        return BitArray.get(visibilityCheck, getSectionId(x,y,z));
    }

    public int frustumCheck(int x, int y, int z) {
        int id = getSectionId(x,y,z);
        int check = BitArray.getPair(frustumChecks, id);
        if (check != 0) {
            return check - 4;
        }
        int res = frustum.testBox(x<<4, y<<4, z<<4, (x<<4) + 16.0f, (y<<4) + 16.0f, (z<<4) + 16.0f) + 4;
        BitArray.set(frustumChecks, res);
        return res-4;
    }

}
