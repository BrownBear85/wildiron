package com.bonker.wildiron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.joml.Quaternionf;

import java.util.Optional;

public class WildIronBEWLR extends BlockEntityWithoutLevelRenderer {
    static WildIronBEWLR instance;

    private final WildIronTrimManager textures;
    final EntityModelSet entityModelSet;
    WildIronModel model;

    public WildIronBEWLR(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet, WildIronTrimManager textures) {
        super(blockEntityRenderDispatcher, entityModelSet);
        this.entityModelSet = entityModelSet;
        this.textures = textures;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Optional<ArmorTrim> trim = Optional.ofNullable(Minecraft.getInstance().level).flatMap(level -> ArmorTrim.getTrim(level.registryAccess(), stack));

        poseStack.popPose(); // remove translations from ItemRenderer
        poseStack.pushPose();

        poseStack.pushPose();
        transform(poseStack, displayContext);
        render(stack, textures.getBase(), poseStack, bufferSource, packedLight, packedOverlay);
        trim.ifPresent(armorTrim -> {
            render(stack, textures.get(armorTrim), poseStack, bufferSource, packedLight, packedOverlay);
        });
        poseStack.popPose();
    }

    private void render(ItemStack stack, TextureAtlasSprite texture, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        VertexConsumer vertexConsumer = texture.wrap(ItemRenderer.getFoilBufferDirect(bufferSource, model.renderType(texture.atlasLocation()), false, stack.hasFoil()));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void transform(PoseStack poseStack, ItemDisplayContext displayContext) {
        switch (displayContext) {
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0, 1.0F, -0.2F);
                poseStack.mulPose((new Quaternionf()).rotationXYZ(0, Mth.PI, Mth.PI));
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.1, 1.3F, -0.3F);
                poseStack.mulPose((new Quaternionf()).rotationXYZ(0, Mth.PI, Mth.PI));
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.1, 1.3F, -0.3F);
                poseStack.mulPose((new Quaternionf()).rotationXYZ(0, Mth.PI, Mth.PI));
            }
            case GROUND -> {
                poseStack.translate(0, 0.67F, 0);
                poseStack.mulPose((new Quaternionf()).rotationXYZ(0, Mth.PI, Mth.PI));
                poseStack.scale(0.67F, 0.67F, 0.67F);
            }
            case GUI -> {
                poseStack.scale(1.3F, -1.3F, -1.3F);
                poseStack.translate(0, -1.0F, 0);
                poseStack.mulPose((new Quaternionf()).rotationXYZ(15 * Mth.DEG_TO_RAD, -30 * Mth.DEG_TO_RAD, 0));
            }
            case HEAD -> {
                poseStack.translate(0, 1.5F, -0.5F);
                poseStack.mulPose((new Quaternionf()).rotationXYZ(0, Mth.PI, Mth.PI));
            }
            case FIXED -> {
                poseStack.translate(0, 1.0F, 0);
                poseStack.mulPose((new Quaternionf()).rotationXYZ(0, Mth.HALF_PI, Mth.PI));
            }
        }
    }
}