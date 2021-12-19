package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import me.jellysquid.mods.sodium.render.entity.part.BakeablePart;
import me.jellysquid.mods.sodium.render.entity.part.BakeablePartBuilder;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ModelPartData.class)
public class MixinModelPartData implements BakeablePartBuilder {
    @Unique
    private int bmm$id = 0;

    @Unique
    private int bmm$nextId = 1;

    @Unique
    private IntSortedSet bmm$recycledIdQueue;

    @Unique
    @Nullable
    private BakeablePartBuilder bmm$parent;

    @Override
    public void setId(int id) {
        bmm$id = id;
        bmm$nextId = id + 1;
    }

    @Override
    public int getId() {
        return bmm$id;
    }

    @Override
    public void setParent(BakeablePartBuilder parent) {
        bmm$parent = parent;
    }

    @Override
    public @Nullable BakeablePartBuilder getParent() {
        return bmm$parent;
    }

    private IntSortedSet bmm$getOrCreateRecycledIdQueue() {
        if (bmm$recycledIdQueue == null) {
            bmm$recycledIdQueue = new IntRBTreeSet();
        }
        return bmm$recycledIdQueue;
    }

    @Override
    public int getNextAvailableModelId() {
        MixinModelPartData topLevelParent = ((MixinModelPartData) bmm$getTopLevelParent());
        IntSortedSet recycledIds = topLevelParent.bmm$getOrCreateRecycledIdQueue();
        if (recycledIds.isEmpty()) {
            return topLevelParent.bmm$nextId++;
        } else {
            int id = recycledIds.firstInt();
            recycledIds.remove(id);
            return id;
        }
    }

    @Inject(method = "addChild", at = @At("RETURN"))
    public void addChildID(String name, ModelPartBuilder builder, ModelTransform rotationData, CallbackInfoReturnable<ModelPartData> cir) {
        BakeablePartBuilder child = (BakeablePartBuilder) cir.getReturnValue();
        child.setParent(this);
        child.setId(this.getNextAvailableModelId());
    }

    @Inject(method = "addChild", at = @At(value = "INVOKE", target = "Ljava/util/Map;putAll(Ljava/util/Map;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void removeOldId(String name, ModelPartBuilder builder, ModelTransform rotationData, CallbackInfoReturnable<ModelPartData> cir, ModelPartData modelPartData, ModelPartData modelPartData2){
        if (modelPartData2 != null) {
            bmm$makeIdReusable(((BakeablePartBuilder) modelPartData2).getId());
        }
    }

    @Unique
    private void bmm$makeIdReusable(int id) {
        ((MixinModelPartData) bmm$getTopLevelParent()).bmm$getOrCreateRecycledIdQueue().add(id);
    }

    @Unique
    private BakeablePartBuilder bmm$getTopLevelParent() {
        BakeablePartBuilder parent = this;
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        return parent;
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "createPart", at = @At("RETURN"))
    public void setModelPartID(int textureWidth, int textureHeight, CallbackInfoReturnable<ModelPart> cir) {
        ((BakeablePart) (Object) cir.getReturnValue()).setId(this.bmm$id);
    }
}
