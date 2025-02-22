package com.bonker.wildiron.client;

import com.bonker.wildiron.WildIron;
import com.bonker.wildiron.item.WildIronItem;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = WildIron.MODID, value = Dist.CLIENT)
public class ClientForgeBusEvents {
    @SubscribeEvent
    public static void interactionMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.getKeyMapping() == Minecraft.getInstance().options.keyUse) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof WildIronItem &&
                    WildIronClient.getAnim(player.getMainArm() == HumanoidArm.RIGHT).spin.getInterpolated(0) < 1) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }

    @SubscribeEvent
    public static void keyPressed(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_RELEASE && Minecraft.getInstance().screen == null && event.getKey() == Minecraft.getInstance().options.keySwapOffhand.getKey().getValue()) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            boolean spinMain = player.getMainHandItem().getItem() instanceof WildIronItem;
            boolean spinOff = player.getOffhandItem().getItem() instanceof WildIronItem;
            boolean mainIsRight = player.getMainArm() == HumanoidArm.RIGHT;

            if (spinMain && spinOff) return;

            boolean playSound = false;
            if (mainIsRight ? spinMain : spinOff) {
                WildIronClient.rightAnim.spin.reset(0);
                WildIronClient.rightAnim.pullOut.reset(0);
                playSound = true;
            }
            if (mainIsRight ? spinOff : spinMain) {
                WildIronClient.leftAnim.spin.reset(0);
                WildIronClient.leftAnim.pullOut.reset(0);
                playSound = true;
            }

            if (playSound) {
                player.level().playSound(player, player.getX(), player.getY(), player.getZ(), WildIron.WHOOSH.get(), SoundSource.PLAYERS, 1.0F, 0.8F + player.getRandom().nextFloat() * 0.4F);
            }
        }
    }
}
