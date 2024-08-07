package com.bonker.wildiron.client;

import com.bonker.wildiron.entity.Bullet;
import com.bonker.wildiron.item.BulletItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

public class BulletRenderer extends ThrownItemRenderer<Bullet> {
    protected BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Bullet entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ResourceLocation texture = getBulletTexture(entity);
        if (texture == null) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } else {
            poseStack.pushPose();
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(0, -0.25F, 0);
            PoseStack.Pose last = poseStack.last();
            Matrix4f pose = last.pose();
            Matrix3f normal = last.normal();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
            vertex(consumer, pose, normal, packedLight, 0.0F, 0, 0, 1);
            vertex(consumer, pose, normal, packedLight, 1.0F, 0, 1, 1);
            vertex(consumer, pose, normal, packedLight, 1.0F, 1, 1, 0);
            vertex(consumer, pose, normal, packedLight, 0.0F, 1, 0, 0);
            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int lightmapUV, float x, int y, int u, int v) {
        consumer.vertex(pose, x - 0.5F, y - 0.25F, 0.0F).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightmapUV).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Nullable
    public ResourceLocation getBulletTexture(Bullet entity) {
        ItemStack stack = entity.getItem();
        if (stack.getItem() instanceof BulletItem bulletItem) {
            return bulletItem.entityTexture;
        }
        return null;
    }
}
