package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.matrix.MatrixStackExtended;
import me.jellysquid.mods.sodium.render.entity.GlobalModelUtils;
import me.jellysquid.mods.sodium.render.entity.data.InstanceBatch;
import me.jellysquid.mods.sodium.render.entity.part.BakeablePart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public abstract class MixinModelPart implements BakeablePart {

    @Unique
    private int bmm$id;

    @Unique
    private boolean bmm$usingSmartRenderer;

    @Shadow
    public boolean visible;

    @Shadow protected abstract void renderCuboids(MatrixStack.Entry entry, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha);

    @Shadow public float roll;

    @Shadow public float pitch;

    @Shadow public float yaw;

    @Shadow @Final private List<ModelPart.Cuboid> cuboids;

    @Shadow @Final private Map<String, ModelPart> children;

    @Shadow public abstract void rotate(MatrixStack matrix);

    @Override
    public void setId(int id) {
        bmm$id = id;
    }

    @Override
    public int getId() {
        return bmm$id;
    }

    // potential loops unrolled for performance
    @SuppressWarnings("DuplicatedCode")
    @Inject(method = "rotate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    public void setSsboRotation(MatrixStack matrices, CallbackInfo ci) {
        if (bmm$usingSmartRenderer) {
            MatrixStack.Entry currentStackEntry = matrices.peek();
            Matrix4f modelMat = currentStackEntry.getModel();

            float sx = MathHelper.sin(pitch);
            float cx = MathHelper.cos(pitch);
            float sy = MathHelper.sin(yaw);
            float cy = MathHelper.cos(yaw);
            float sz = MathHelper.sin(roll);
            float cz = MathHelper.cos(roll);

            float rot00 = cy * cz;
            float rot01 = (sx * sy * cz) - (cx * sz);
            float rot02 = (cx * sy * cz) + (sx * sz);
            float rot10 = cy * sz;
            float rot11 = (sx * sy * sz) + (cx * cz);
            float rot12 = (cx * sy * sz) - (sx * cz);
            float rot20 = -sy;
            float rot21 = sx * cy;
            float rot22 = cx * cy;

            float newModel00 = modelMat.a00 * rot00 + modelMat.a01 * rot10 + modelMat.a02 * rot20;
            float newModel01 = modelMat.a00 * rot01 + modelMat.a01 * rot11 + modelMat.a02 * rot21;
            float newModel02 = modelMat.a00 * rot02 + modelMat.a01 * rot12 + modelMat.a02 * rot22;
            float newModel10 = modelMat.a10 * rot00 + modelMat.a11 * rot10 + modelMat.a12 * rot20;
            float newModel11 = modelMat.a10 * rot01 + modelMat.a11 * rot11 + modelMat.a12 * rot21;
            float newModel12 = modelMat.a10 * rot02 + modelMat.a11 * rot12 + modelMat.a12 * rot22;
            float newModel20 = modelMat.a20 * rot00 + modelMat.a21 * rot10 + modelMat.a22 * rot20;
            float newModel21 = modelMat.a20 * rot01 + modelMat.a21 * rot11 + modelMat.a22 * rot21;
            float newModel22 = modelMat.a20 * rot02 + modelMat.a21 * rot12 + modelMat.a22 * rot22;
            float newModel30 = modelMat.a30 * rot00 + modelMat.a31 * rot10 + modelMat.a32 * rot20;
            float newModel31 = modelMat.a30 * rot01 + modelMat.a31 * rot11 + modelMat.a32 * rot21;
            float newModel32 = modelMat.a30 * rot02 + modelMat.a31 * rot12 + modelMat.a32 * rot22;

            Matrix3f normalMat = currentStackEntry.getNormal();
            // TODO: are the checks really faster?
            if (modelMat.a00 == normalMat.a00 && modelMat.a01 == normalMat.a01 && modelMat.a02 == normalMat.a02) {
                normalMat.a00 = newModel00;
                normalMat.a01 = newModel01;
                normalMat.a02 = newModel02;
            } else {
                float newNormal00 = normalMat.a00 * rot00 + normalMat.a01 * rot10 + normalMat.a02 * rot20;
                float newNormal01 = normalMat.a00 * rot01 + normalMat.a01 * rot11 + normalMat.a02 * rot21;
                float newNormal02 = normalMat.a00 * rot02 + normalMat.a01 * rot12 + normalMat.a02 * rot22;
                normalMat.a00 = newNormal00;
                normalMat.a01 = newNormal01;
                normalMat.a02 = newNormal02;
            }

            if (modelMat.a10 == normalMat.a10 && modelMat.a11 == normalMat.a11 && modelMat.a12 == normalMat.a12) {
                normalMat.a10 = newModel10;
                normalMat.a11 = newModel11;
                normalMat.a12 = newModel12;
            } else {
                float newNormal10 = normalMat.a10 * rot00 + normalMat.a11 * rot10 + normalMat.a12 * rot20;
                float newNormal11 = normalMat.a10 * rot01 + normalMat.a11 * rot11 + normalMat.a12 * rot21;
                float newNormal12 = normalMat.a10 * rot02 + normalMat.a11 * rot12 + normalMat.a12 * rot22;
                normalMat.a10 = newNormal10;
                normalMat.a11 = newNormal11;
                normalMat.a12 = newNormal12;
            }

            if (modelMat.a20 == normalMat.a20 && modelMat.a21 == normalMat.a21 && modelMat.a22 == normalMat.a22) {
                normalMat.a20 = newModel20;
                normalMat.a21 = newModel21;
                normalMat.a22 = newModel22;
            } else {
                float newNormal20 = normalMat.a20 * rot00 + normalMat.a21 * rot10 + normalMat.a22 * rot20;
                float newNormal21 = normalMat.a20 * rot01 + normalMat.a21 * rot11 + normalMat.a22 * rot21;
                float newNormal22 = normalMat.a20 * rot02 + normalMat.a21 * rot12 + normalMat.a22 * rot22;
                normalMat.a20 = newNormal20;
                normalMat.a21 = newNormal21;
                normalMat.a22 = newNormal22;
            }

            modelMat.a00 = newModel00;
            modelMat.a01 = newModel01;
            modelMat.a02 = newModel02;
            modelMat.a10 = newModel10;
            modelMat.a11 = newModel11;
            modelMat.a12 = newModel12;
            modelMat.a20 = newModel20;
            modelMat.a21 = newModel21;
            modelMat.a22 = newModel22;
            modelMat.a30 = newModel30;
            modelMat.a31 = newModel31;
            modelMat.a32 = newModel32;

            InstanceBatch batch = ((MatrixStackExtended) matrices).getBatch();
            // FIXME: is this always ok to do? think this is bad with skeleton holding bows
            if (batch != null) {
                batch.getMatrices().set(bmm$id, this.visible ? currentStackEntry : null); // TODO: does this method ever get called when the part is not visible?
            }
            ci.cancel();
        }
    }

    /**
     * Used to manipulate visibility, matrices, and drawing
     *
     * @author burgerdude
     */
    @Overwrite
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (!this.cuboids.isEmpty() || !this.children.isEmpty()) {
            boolean rotateOnly = vertexConsumer == null;
            SmartBufferBuilderWrapper smartBufferBuilderWrapper = null;
            if (vertexConsumer instanceof SmartBufferBuilderWrapper converted) {
                smartBufferBuilderWrapper = converted;
            }
            bmm$usingSmartRenderer = (rotateOnly || smartBufferBuilderWrapper != null) && MinecraftClient.getInstance().getWindow() != null;

            // force render when constructing the vbo
            if (this.visible || (!rotateOnly && bmm$usingSmartRenderer)) {
                matrices.push();

                this.rotate(matrices);

                if (bmm$usingSmartRenderer) {
                    if (!rotateOnly) {
                        // this will never be null because the check for smart render only passes if this isn't null
                        //noinspection ConstantConditions
                        smartBufferBuilderWrapper.setId(this.getId());
                        this.renderCuboids(GlobalModelUtils.IDENTITY_STACK_ENTRY, vertexConsumer, light, overlay, red, green, blue, alpha);
                    }
                } else {
                    this.renderCuboids(matrices.peek(), vertexConsumer, light, overlay, red, green, blue, alpha);
                }

                for(ModelPart modelPart : this.children.values()) {
                    modelPart.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
                }

                matrices.pop();
            } else if (bmm$usingSmartRenderer) {
                recurseSetNullMatrix(((MatrixStackExtended) matrices).getBatch(), (ModelPart) (Object) this);
            }
        }
    }

    private void recurseSetNullMatrix(InstanceBatch batch, ModelPart modelPart) {
        if ((Object) modelPart instanceof MixinModelPart modelPartMixin) {
            batch.getMatrices().set(modelPartMixin.getId(), null);
            for (ModelPart child : modelPartMixin.children.values()) {
                recurseSetNullMatrix(batch, child);
            }
        }
    }

}
