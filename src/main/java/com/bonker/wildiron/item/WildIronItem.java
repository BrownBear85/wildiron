package com.bonker.wildiron.item;

import com.bonker.wildiron.WildIron;
import com.bonker.wildiron.client.WildIronClient;
import com.bonker.wildiron.networking.FiredGunC2SPacket;
import com.bonker.wildiron.networking.WildIronNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WildIronItem extends ProjectileWeaponItem {
    public static final int FIRE_COOLDOWN = 24;
    public static final float MAX_INACCURACY = 10.0F;
    public static final int MAX_BULLETS = 6;
    public static final int BULLET_LOAD_TIME = 40;

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
        return pRepairCandidate.is(Items.FEATHER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long time = level.getGameTime();
        if (level.isClientSide && canFire(stack, level) && time - WildIronClient.lastFired > 3) {
            if (getBullets(stack).isEmpty()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SAND_HIT, SoundSource.PLAYERS, 1.0F, 0.7F + player.getRandom().nextFloat() * 0.6F);
                WildIronClient.startedLoading = time;
            } else {
                WildIronNetwork.sendToServer(new FiredGunC2SPacket(Mth.wrapDegrees(player.getXRot()), Mth.wrapDegrees(player.getYRot()), hand));
                WildIronClient.lastFired = level.getGameTime();
                return InteractionResultHolder.pass(stack);
            }
        }

        if (!level.isClientSide && getBulletCount(stack) == 0 && !player.getProjectile(stack).isEmpty() && time - stack.getOrCreateTag().getLong("lastFired") > 10) {
            player.startUsingItem(hand);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
        pRemainingUseDuration -= BULLET_LOAD_TIME;
        if (!pLevel.isClientSide) {
            int bullets = getBulletCount(pStack);
            if (pRemainingUseDuration < (MAX_BULLETS - bullets) * BULLET_LOAD_TIME && pRemainingUseDuration % BULLET_LOAD_TIME == 0) {
                loadBullet(pLevel, pLivingEntity, pStack);
            }
        }
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
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 0.6F + entity.getRandom().nextFloat() * 0.8F);
        boolean full = addBullet(stack, projectile);
        if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
            projectile.shrink(1);
        }
        if (full || entity.getProjectile(stack).isEmpty()) {
            entity.useItemRemaining = 20;
        }
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return (MAX_BULLETS - getBulletCount(pStack)) * BULLET_LOAD_TIME + BULLET_LOAD_TIME;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        List<ItemStack> bullets = getBullets(pStack);
        if (bullets.isEmpty()) {
            pTooltipComponents.add(Component.translatable("tooltip.wildiron.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            pTooltipComponents.add(Component.translatable("tooltip.wildiron.bullets").withStyle(ChatFormatting.GRAY));
            for (ItemStack bullet : bullets) {
                pTooltipComponents.add(Component.literal(" - ").append(bullet.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
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
