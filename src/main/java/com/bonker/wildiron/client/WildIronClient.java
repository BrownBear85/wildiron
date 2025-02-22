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
    public static final int PULL_OUT_TIME = 10;

    public static final GunAnimationState leftAnim = new GunAnimationState();
    public static final GunAnimationState rightAnim = new GunAnimationState();

    public static GunAnimationState getAnim(boolean right) {
        return right ? rightAnim : leftAnim;
    }

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

            boolean broken = itemInHand.getDamageValue() >= itemInHand.getMaxDamage() - 1;

            boolean right = arm == HumanoidArm.RIGHT;

            GunAnimationState state = getAnim(right);
            float spin = state.spin.getInterpolated(partialTick);
            spin = -(Mth.cos(Mth.PI * spin) - 1) / 2;
            float pullOut = state.pullOut.getInterpolated(partialTick);

            if (timeSinceFired < 5) {
                float recoilAnim = Mth.sin(timeSinceFired * 0.2F * Mth.PI) * Mth.HALF_PI * 0.2F;

                if (broken) {
                    poseStack.rotateAround(Axis.XP.rotation(-Mth.PI * 0.25F * timeSinceFired / 5F), 0.7F, -0.3F, -0.6F);
                }

                poseStack.translate(0, -0.2F, recoilAnim * 0.7F - 0.5F);
                poseStack.mulPose(Axis.XP.rotation(recoilAnim));
                poseStack.translate(0, 0.2F, 0.5F);
            } else {
                if (broken) {
                    poseStack.rotateAround(Axis.XP.rotation(-Mth.PI * 0.25F), 0.7F, -0.3F, -0.6F);
                } else if (player.isUsingItem() && player.getUsedItemHand().ordinal() != arm.ordinal()) {
                    float loadAnim = getLoadAnim(player, partialTick);
                    float loadTime = time - startedLoading;

                    if (spin == 1) {
                        if (loadTime <= PULL_OUT_TIME) {
                            float angle = (1 - Mth.cos(loadAnim * ((float) WildIronItem.BULLET_LOAD_TIME / PULL_OUT_TIME) * Mth.PI)) * 35F;
                            poseStack.rotateAround(Axis.XP.rotationDegrees(angle), 0.7F, -0.3F, -0.8F);
                        } else {
                            poseStack.rotateAround(Axis.XP.rotationDegrees(70), 0.7F, -0.3F, -0.8F);
                        }

                        if ((player.getUseItemRemainingTicks() >= WildIronItem.BULLET_LOAD_TIME && loadAnim > 0.8) || (loadTime >= WildIronItem.BULLET_LOAD_TIME && loadAnim < 0.2)) {
                            float anim;
                            if (loadAnim >= 0.8) {
                                anim = (1 - Mth.cos((loadAnim - 0.8F) * 2.5F * Mth.TWO_PI));
                            } else {
                                anim = (1 - Mth.cos((loadAnim + 0.2F) * 2.5F * Mth.TWO_PI));
                            }

                            poseStack.translate(0, 0, anim * 0.2F);
                        }
                    }
                }
            }

            // default transform
            poseStack.translate(right ? 0.56F : -0.56F, -0.52F, -0.72F);

            ItemStack currentItem = player.getItemInHand(arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            if (ItemStack.isSameItemSameTags(itemInHand, currentItem)) {
                // taking out
                pullOut = (1 - pullOut);
                pullOut *= pullOut;
                poseStack.translate(0, pullOut * -1.2F, pullOut * -0.6F);

                // spin
                poseStack.rotateAround(Axis.XP.rotationDegrees(-90 + spin * 450), 0F, 0.25F, -0.15F);
            } else {
                // putting away
                poseStack.translate(0, equipProcess * -0.6F, 0);
                poseStack.rotateAround(Axis.XP.rotationDegrees(360 - equipProcess * 180), 0F, 0.25F, -0.15F);
            }

            // arm swing attack animation
            float sign = right ? 1 : -1;
            float anim = Mth.sin(swingProcess * swingProcess * Mth.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees(sign * (45 + anim * -20)));
            float rAnim = Mth.sin(Mth.sqrt(swingProcess) * Mth.PI);
            poseStack.mulPose(Axis.ZP.rotationDegrees(sign * rAnim * -20));
            poseStack.mulPose(Axis.XP.rotationDegrees(rAnim * -20));
            poseStack.mulPose(Axis.YP.rotationDegrees(sign * -45));
            return true;
        }
    };

    private static float getLoadAnim(LocalPlayer player, float partialTick) {
        float remaining = (player.getUseItemRemainingTicks() - WildIronItem.BULLET_LOAD_TIME - partialTick) % WildIronItem.BULLET_LOAD_TIME;

        // handle end reloading animation
        if (remaining < 0) {
            remaining += WildIronItem.BULLET_LOAD_TIME;
        }

        // goes from 0 to 1 each time you load a bullet, goes from 0 to 0.25 while pulling out the gun
        return (WildIronItem.BULLET_LOAD_TIME - remaining) / WildIronItem.BULLET_LOAD_TIME;
    }

    private static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean isRight = arm == HumanoidArm.RIGHT;
        ModelPart armPart = isRight ? model.rightArm : model.leftArm;

        armPart.xRot *= 0.1F;
        if (entity.isUsingItem() && entity.getUsedItemHand().ordinal() != arm.ordinal()) {
            armPart.yRot -= Mth.HALF_PI * 0.5F;
            armPart.xRot += 0 - Mth.HALF_PI * 0.5F;
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
