package com.bonker.wildiron.client;

import com.bonker.wildiron.item.WildIronItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class WildIronClient {
    public static long lastFired = 0;
    public static long startedLoading = 0;

    private static final HumanoidModel.ArmPose WILD_IRON_ARM_POSE = HumanoidModel.ArmPose.create("wildiron", false, WildIronClient::pose);

    public static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return WildIronBEWLR.instance;
        }

        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
            return WILD_IRON_ARM_POSE;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
            float time = player.level().getGameTime() + partialTick;
            float timeSinceFired = time - itemInHand.getOrCreateTag().getLong("lastFired");

            if (timeSinceFired < 5) {
                float recoilAnim = Mth.sin(timeSinceFired * 0.2F * Mth.PI) * Mth.HALF_PI * 0.2F;

                poseStack.translate(0, -0.2F, recoilAnim * 0.7F - 0.5F);
                poseStack.mulPose(Axis.XP.rotation(recoilAnim));
                poseStack.translate(0, 0.2F, 0.5F);
            } else if (player.isUsingItem() && player.getUsedItemHand().ordinal() != arm.ordinal()) {
                float remaining = (player.getUseItemRemainingTicks() - WildIronItem.BULLET_LOAD_TIME - partialTick) % WildIronItem.BULLET_LOAD_TIME;
                float loadAnim = (WildIronItem.BULLET_LOAD_TIME - remaining) / WildIronItem.BULLET_LOAD_TIME;
                float loadTime = time - startedLoading;

                if (loadTime < 10) {
                    poseStack.rotateAround(Axis.YP.rotationDegrees(Mth.sin(loadAnim * Mth.TWO_PI) * 75), 0.7F, 0, -0.4F);
                } else {
                    poseStack.rotateAround(Axis.YP.rotationDegrees(75), 0.7F, 0, -0.4F);
                }
                poseStack.rotateAround(Axis.XP.rotationDegrees((Mth.sin(loadAnim * Mth.TWO_PI) - 0.5F) * 5), 0, -0.2F, -0.5F);
            }
            return false;
        }
    };

    private static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean isRight = arm == HumanoidArm.RIGHT;
        ModelPart armPart = isRight ? model.rightArm : model.leftArm;

        armPart.xRot *= 0.1F;
        if (entity.isUsingItem() && entity.getUsedItemHand().ordinal() != arm.ordinal()) {
            armPart.yRot -= Mth.HALF_PI * 0.5F;
            armPart.xRot += model.head.xRot - Mth.HALF_PI * 0.5F;
        } else {
            float recoil = 0;
            float timeSinceFired = entity.level().getGameTime() + Minecraft.getInstance().getFrameTime() - entity.getItemInHand(isRight ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND).getOrCreateTag().getLong("lastFired");
            if (timeSinceFired < 5) {
                recoil = Mth.sin(timeSinceFired * 0.2F * Mth.PI) * Mth.HALF_PI * 0.2F;
            }

            armPart.xRot += model.head.xRot - Mth.HALF_PI - recoil;
            armPart.yRot += model.head.yRot - (arm == HumanoidArm.RIGHT ? 0.08F : -0.08F);
        }
    }
}
