package com.bonker.wildiron.networking;

import com.bonker.wildiron.entity.Bullet;
import com.bonker.wildiron.item.WildIronItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FiredGunC2SPacket(float xRot, float yRot, InteractionHand hand) {
    public FiredGunC2SPacket(FriendlyByteBuf buf) {
        this(buf.readFloat(), buf.readFloat(), buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(xRot);
        buf.writeFloat(yRot);
        buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            ItemStack gunStack = player.getItemInHand(hand);

            if (!WildIronItem.canFire(gunStack, level)) {
                return;
            }

            boolean isCreative = player.getAbilities().instabuild;
            ItemStack bulletStack = WildIronItem.removeNextBullet(gunStack);
            if (isCreative || !bulletStack.isEmpty()) {
                Bullet bullet = new Bullet(level, player);
                bullet.setItem(bulletStack);
                bullet.calculateCritical();
                bullet.setPos(player.getEyePosition());
                float xd = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
                float yd = -Mth.sin((xRot) * Mth.DEG_TO_RAD);
                float zd = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
                bullet.shoot(xd, yd, zd, 4.0F, WildIronItem.getInaccuracyValue(player, gunStack));
                level.addFreshEntity(bullet);

                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4F, 1.7F + player.getRandom().nextFloat() * 0.3F);

                gunStack.getOrCreateTag().putLong("lastFired", level.getGameTime());
                gunStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

                if (!isCreative && !bulletStack.isEmpty()) {
                    bulletStack.shrink(1);
                }
            }
        });
    }
}
