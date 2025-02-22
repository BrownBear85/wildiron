package com.bonker.wildiron.item;

import com.bonker.wildiron.WildIron;
import com.bonker.wildiron.client.WildIronClient;
import com.bonker.wildiron.networking.FiredGunC2SPacket;
import com.bonker.wildiron.networking.WildIronNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WildIronItem extends ProjectileWeaponItem {
    public static final int FIRE_COOLDOWN = 15;
    public static final float MAX_INACCURACY = 10.0F;
    public static final int MAX_BULLETS = 6;
    public static final int BULLET_LOAD_TIME = 20;

    public WildIronItem(Item.Properties properties) {
        super(properties);
    }

    public static float getInaccuracyValue(Player player, ItemStack stack) {
        float inaccuracy = player.getItemBySlot(EquipmentSlot.HEAD).is(WildIron.COWBOY_HAT.get()) ? 0.0F : 2.0F;

        int durability = stack.getMaxDamage() - stack.getDamageValue();
        if (durability <= 15) {
            inaccuracy += ((15 - durability) / 15F) * MAX_INACCURACY;
        }

        return inaccuracy;
    }

    public static boolean canFire(ItemStack stack, Level level) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1 && level.getGameTime() - stack.getOrCreateTag().getLong("lastFired") >= FIRE_COOLDOWN;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> stack.getItem() instanceof BulletItem;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack pStack, ItemStack pRepairCandidate) {
        return pRepairCandidate.is(Items.IRON_INGOT);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long time = level.getGameTime();

        if (level.isClientSide) {
            if (WildIronClient.getAnim(handToArm(player, hand) == HumanoidArm.RIGHT).spin.getInterpolated(1) < 1) {
                return InteractionResultHolder.pass(stack);
            }

            WildIronClient.startedLoading = time;
            if (canFire(stack, level) && Math.abs(time - WildIronClient.lastFired) > 3 && !getBullets(stack).isEmpty()) {
                WildIronNetwork.sendToServer(new FiredGunC2SPacket(Mth.wrapDegrees(player.getXRot()), Mth.wrapDegrees(player.getYRot()), hand));
                WildIronClient.lastFired = level.getGameTime();
                player.addDeltaMovement(player.getLookAngle().multiply(-0.1, -0.2, -0.1));
            }
        } else {
            if (hand == InteractionHand.OFF_HAND) {
                return InteractionResultHolder.pass(stack);
            }

            if (stack.getDamageValue() < stack.getMaxDamage() - 1 && getBulletCount(stack) == 0) {
                if (player.getProjectile(stack).isEmpty()) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), WildIron.EMPTY.get(), SoundSource.PLAYERS, 0.6F, 0.9F + player.getRandom().nextFloat() * 0.2F);
                } else if (time - stack.getOrCreateTag().getLong("lastFired") > 10) {
                    ItemStack otherStack = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                    if (otherStack.getItem() instanceof WildIronItem && WildIronItem.getBulletCount(otherStack) != 0) {
                        return InteractionResultHolder.pass(stack);
                    }

                    player.startUsingItem(hand);
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    public static HumanoidArm handToArm(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    @Override
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
        if (!pLevel.isClientSide) {
            int bullets = getBulletCount(pStack);
            if (pRemainingUseDuration < (MAX_BULLETS + 1 - bullets) * BULLET_LOAD_TIME && pRemainingUseDuration % BULLET_LOAD_TIME == 0) {
                loadBullet(pLevel, pLivingEntity, pStack);
                if (pLivingEntity.getProjectile(pStack).isEmpty()) {
                    pLivingEntity.stopUsingItem();
                }
            }
        }
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof Player player && player.level().isClientSide) {
            if (count < (MAX_BULLETS + 1) * BULLET_LOAD_TIME - WildIronClient.PULL_OUT_TIME) {
                WildIronClient.getAnim(handToArm(player, player.getUsedItemHand()) == HumanoidArm.RIGHT)
                        .spin.reset(0.3556F);
                player.level().playSound(player, player.getX(), player.getY(), player.getZ(), WildIron.WHOOSH.get(), SoundSource.PLAYERS, 1.0F, 0.8F + player.getRandom().nextFloat() * 0.4F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (pLivingEntity instanceof Player player && player.level().isClientSide) {
            WildIronClient.getAnim(handToArm(player, player.getUsedItemHand()) == HumanoidArm.RIGHT)
                    .spin.reset(0.3556F);
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(), WildIron.WHOOSH.get(), SoundSource.PLAYERS, 1.0F, 0.8F + player.getRandom().nextFloat() * 0.4F);
        }
        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    protected void loadBullet(Level level, LivingEntity entity, ItemStack stack) {
        ItemStack projectile = entity.getProjectile(stack);
        if (projectile.isEmpty()) {
            entity.releaseUsingItem();
            return;
        }
        if (projectile.is(Items.ARROW)) {
            projectile = new ItemStack(WildIron.IRON_BULLET.get());
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), WildIron.LOAD.get(), SoundSource.PLAYERS, 2.5F, 0.8F + entity.getRandom().nextFloat() * 0.5F);
        addBullet(stack, projectile);
        if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
            projectile.shrink(1);
        }
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return (MAX_BULLETS + 1) * BULLET_LOAD_TIME;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (pStack.getDamageValue() >= pStack.getMaxDamage() - 1) {
            pTooltipComponents.add(Component.translatable("tooltip.wildiron.broken").withStyle(ChatFormatting.RED));
        }

        List<ItemStack> bullets = getBullets(pStack);
        if (bullets.isEmpty()) {
            pTooltipComponents.add(Component.translatable("tooltip.wildiron.empty", MAX_BULLETS).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            pTooltipComponents.add(Component.translatable("tooltip.wildiron.bullets", bullets.size(), MAX_BULLETS).withStyle(ChatFormatting.GRAY));
            for (ItemStack bullet : bullets) {
                pTooltipComponents.add(Component.literal(" - ").append(bullet.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        boolean shift = FMLEnvironment.dist == Dist.CLIENT && Screen.hasShiftDown();
        pTooltipComponents.add(Component.translatable("tooltip.wildiron.hold_shift").withStyle(shift ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        if (shift) {
            for (int i = 0; i < 4; i++) {
                pTooltipComponents.add(Component.literal("  ").append(Component.translatable("tooltip.wildiron.description_" + i).withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(WildIronClient.EXTENSION);
    }

    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    public static ItemStack removeNextBullet(ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains("Bullets")) {
            return ItemStack.EMPTY;
        }
        ListTag list = stack.getOrCreateTag().getList("Bullets", ListTag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = list.getCompound(0);
        list.remove(0);
        return ItemStack.of(tag);
    }

    public static List<ItemStack> getBullets(ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains("Bullets")) {
            return List.of();
        }
        ListTag list = stack.getOrCreateTag().getList("Bullets", ListTag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return List.of();
        }
        List<ItemStack> bullets = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            bullets.add(ItemStack.of(list.getCompound(i)));
        }
        return bullets;
    }

    public static int getBulletCount(ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains("Bullets")) {
            return 0;
        }
        return stack.getOrCreateTag().getList("Bullets", ListTag.TAG_COMPOUND).size();
    }

    public static boolean addBullet(ItemStack gun, ItemStack bullet) {
        ListTag list;
        if (gun.getOrCreateTag().contains("Bullets")) {
            list = gun.getOrCreateTag().getList("Bullets", ListTag.TAG_COMPOUND);
        } else {
            list = new ListTag();
        }
        list.add(bullet.copyWithCount(1).save(new CompoundTag()));
        gun.getOrCreateTag().put("Bullets", list);
        return list.size() >= MAX_BULLETS;
    }
}
