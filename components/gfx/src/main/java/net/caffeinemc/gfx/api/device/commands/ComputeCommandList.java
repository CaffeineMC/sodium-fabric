package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.buffer.Buffer;

public interface ComputeCommandList {
    void dispatchCompute(int numGroupsX, int numGroupsY, int numGroupsZ);
    
    void bindDispatchIndirectBuffer(Buffer buffer);
    
    void dispatchComputeIndirect(long indirectOffset);
}
