package com.bonker.wildiron.client;

import com.bonker.wildiron.WildIron;
import com.bonker.wildiron.item.WildIronItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildIron.MODID, value = Dist.CLIENT)
public class WildIronOverlay {
    private static final int TEXT_TIME = 10;
    private static final float TEXT_STEP = 1F / TEXT_TIME;
    private static final int SPIN_TIME = 10;
    private static final float SPIN_STEP = 1F / SPIN_TIME;

    private static int lastSelectedSlot = -1;

    static final IGuiOverlay OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.noPhysics) return;

        renderSide(true, gui, guiGraphics, partialTick, screenWidth, screenHeight);
        renderSide(false, gui, guiGraphics, partialTick, screenWidth, screenHeight);
    };

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            int newSlot = player.getInventory().selected;
            if (lastSelectedSlot != newSlot) {
                WildIronClient.rightAnim.spin.reset(-0.2F);
                WildIronClient.rightAnim.pullOut.reset(-0.2F);

                if (player.getInventory().getSelected().getItem() instanceof WildIronItem) {
                    player.level().playSound(player, player.getX(), player.getY(), player.getZ(), WildIron.WHOOSH.get(), SoundSource.PLAYERS, 1.0F, 0.8F + player.getRandom().nextFloat() * 0.4F);
                }
            }
            lastSelectedSlot = newSlot;

            boolean mainIsRight = player.getMainArm() == HumanoidArm.RIGHT;
            tickSide(true, mainIsRight ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, mainIsRight ? player.getMainHandItem() : player.getOffhandItem());
            tickSide(false, mainIsRight ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, mainIsRight ? player.getOffhandItem() : player.getMainHandItem());
        }
    }

    private static void tickSide(boolean right, InteractionHand hand, ItemStack stack) {
        if (Minecraft.getInstance().level == null) return;

        boolean hasGun = stack.getItem() instanceof WildIronItem;

        GunAnimationState state = WildIronClient.getAnim(right);
        state.textPos.addValue(hasGun ? TEXT_STEP : -TEXT_STEP * 2, -0.2F, 1);

        if (hasGun) {
            state.spin.addValue(SPIN_STEP, -0.2F, 1);
            state.pullOut.addValue(SPIN_STEP, -0.2F, 1);

            // update text
            if (state.textPos.getInterpolated(0) > 0) {
                if (ItemStack.isSameItemSameTags(stack, state.stack)) {
                    return;
                }

                Component message;
                if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
                    message = Component.translatable("tooltip.wildiron.broken").withStyle(ChatFormatting.ITALIC);
                } else {
                    message = Component.translatable("overlay.wildiron.bullets", WildIronItem.getBulletCount(stack), WildIronItem.MAX_BULLETS);
                }

                TextColor color = ArmorTrim.getTrim(Minecraft.getInstance().level.registryAccess(), stack)
                        .map(t -> t.material().value().description().getStyle())
                        .orElse(Style.EMPTY)
                        .getColor();

                state.stack = stack;
                state.textMessage = message;
                state.textColor = color == null ? 0xffffff : color.getValue();
            }
        }
    }

    private static void renderSide(boolean right, Gui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        GunAnimationState state = WildIronClient.getAnim(right);
        Component message = state.textMessage;
        int textWidth = gui.getFont().width(message);

        float value = state.textPos.getInterpolated(partialTick);
        value = 1 - (1 - value) * (1 - value);

        float scale = 2 * value;
        float recip = 1 / scale;

        int distance = screenWidth / 12;

        float x;
        if (right) {
            x = screenWidth - (textWidth + distance) * scale * value;
        } else {
            x = (-textWidth + (textWidth + distance) * value) * scale;
        }

        float y = screenHeight - 40 * value;

        int alpha = (int) (255 * value);
        if (alpha > 3) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.scale(scale, scale, 1);
            guiGraphics.drawString(gui.getFont(), message.getVisualOrderText(), x * recip, y * recip, alpha << 24 | state.textColor, true);
            poseStack.scale(recip, recip, 1);
        }
    }
}