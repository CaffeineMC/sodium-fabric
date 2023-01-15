package net.caffeinemc.sodium.mixin.features.debug;

import java.util.function.Supplier;
import net.caffeinemc.sodium.SodiumClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.TickDurationMonitor;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.SampleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    
    private static final Profiler GFX_DEBUGGER = new Profiler() {
        @Override
        public void startTick() {
            this.push(Profiler.ROOT_NAME);
        }
    
        @Override
        public void endTick() {
            this.pop();
        }
    
        @Override
        public void push(String location) {
            SodiumClientMod.DEVICE.pushDebugGroup(location);
        }
    
        @Override
        public void push(Supplier<String> locationGetter) {
            this.push(locationGetter.get());
        }
    
        @Override
        public void pop() {
            SodiumClientMod.DEVICE.popDebugGroup();
        }
    
        @Override
        public void swap(String location) {
            this.pop();
            this.push(location);
        }
    
        @Override
        public void swap(Supplier<String> locationGetter) {
            this.swap(locationGetter.get());
        }
    
        @Override
        public void markSampleType(SampleType type) {
            // noop
        }
    
        @Override
        public void visit(String marker, int i) {
            // noop
        }
    
        @Override
        public void visit(Supplier<String> markerGetter, int i) {
            // noop
        }
    };
    
    @Inject(method = "startMonitor", at = @At("RETURN"), cancellable = true)
    private void addDebugger(boolean active, TickDurationMonitor monitor, CallbackInfoReturnable<Profiler> cir) {
        cir.setReturnValue(Profiler.union(cir.getReturnValue(), GFX_DEBUGGER));
    }
    
}
