package com.bonker.wildiron.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BulletItem extends Item {
    public final float damage;
    public final float criticalChance;
    public final ResourceLocation entityTexture;

    public BulletItem(float damage, float criticalChance, ResourceLocation entityTexture, Properties properties) {
        super(properties);
        this.damage = damage;
        this.criticalChance = criticalChance;
        this.entityTexture = entityTexture;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.wildiron.damage", damage).withStyle(ChatFormatting.DARK_GRAY));
        if (criticalChance > 0) {
            pTooltipComponents.add(Component.translatable("tooltip.wildiron.critChance", Mth.floor(criticalChance * 100)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
