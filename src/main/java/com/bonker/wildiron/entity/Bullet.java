package com.bonker.wildiron.entity;

import com.bonker.wildiron.WildIron;
import com.bonker.wildiron.item.BulletItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

public class Bullet extends Projectile implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(Bullet.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_IS_CRITICAL = SynchedEntityData.defineId(Bullet.class, EntityDataSerializers.BOOLEAN);
    public static final ResourceKey<DamageType> BULLET_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(WildIron.MODID, "wildiron"));
    private static final int LIFETIME = 40;

    private int age = 0;

    public Bullet(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public Bullet(Level level, Player player) {
        super(WildIron.BULLET_ENTITY_TYPE.get(), level);
        setOwner(player);
    }

    public void setItem(ItemStack pStack) {
        if (!pStack.is(WildIron.IRON_BULLET.get()) || pStack.hasTag()) {
            entityData.set(DATA_ITEM_STACK, pStack.copyWithCount(1));
        }
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = entityData.get(DATA_ITEM_STACK);
        return stack.isEmpty() ? new ItemStack(WildIron.IRON_BULLET.get()) : stack;
    }

    public void calculateCritical() {
        ItemStack stack = getItem();
        if (!stack.isEmpty() && stack.getItem() instanceof BulletItem bulletItem) {
            if (bulletItem.criticalChance > 0 && random.nextFloat() <= bulletItem.criticalChance) {
                entityData.set(DATA_IS_CRITICAL, true);
                if (getOwner() instanceof ServerPlayer player) {
                    player.connection.send(new ClientboundSoundPacket(level().registryAccess().registryOrThrow(Registries.SOUND_EVENT).wrapAsHolder(SoundEvents.PLAYER_ATTACK_CRIT), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.6F, 1.0F, random.nextLong()));
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM_STACK, new ItemStack(WildIron.IRON_BULLET.get()));
        entityData.define(DATA_IS_CRITICAL, false);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && ++age >= LIFETIME) {
            discard();
        }

        super.tick();

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        boolean teleported = false;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
            BlockState state = level().getBlockState(pos);
            if (state.is(Blocks.NETHER_PORTAL)) {
                handleInsidePortal(pos);
                teleported = true;
            } else if (state.is(Blocks.END_GATEWAY)) {
                if (level().getBlockEntity(pos) instanceof TheEndGatewayBlockEntity gateway && TheEndGatewayBlockEntity.canEntityTeleport(this)) {
                    TheEndGatewayBlockEntity.teleportEntity(level(), pos, state, this,gateway);
                }
                teleported = true;
            }
        }

        if (hitResult.getType() != HitResult.Type.MISS && !teleported && !ForgeEventFactory.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
        }

        checkInsideBlocks();
        Vec3 delta = getDeltaMovement();
        Vec3 newPos = position().add(delta);
        updateRotation();
        if (isInWater()) {
            for (int i = 0; i < 4; ++i) {
                float scale = 0.25F;
                level().addParticle(ParticleTypes.BUBBLE, newPos.x - delta.x * scale, newPos.y - delta.y * scale, newPos.z - delta.z * scale, delta.x, delta.y, delta.z);
            }
        }

        setPos(newPos);

        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
            if (entityData.get(DATA_IS_CRITICAL)) {
                level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (!getItem().isEmpty()) {
            pCompound.put("Item", getItem().save(new CompoundTag()));
        }
        pCompound.putInt("Age", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setItem(ItemStack.of(pCompound.getCompound("Item")));
        age = pCompound.getInt("Age");
    }

    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (!level().isClientSide && hitResult.getEntity().canBeHitByProjectile()) {
            ItemStack stack = getItem();
            float damage = 2;
            if (stack.getItem() instanceof BulletItem bulletItem) {
                damage = bulletItem.damage;

                if (entityData.get(DATA_IS_CRITICAL)) {
                    damage *= 1.2F;
                    if (getOwner() instanceof ServerPlayer player) {
                        player.crit(hitResult.getEntity());
                        playSound(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 1.2F, 0.7F + random.nextFloat() * 0.6F);
                    }
                }

                if (!isSilent() && getOwner() instanceof ServerPlayer player && hitResult.getEntity() instanceof Player) {
                    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
                }
            }
            hitResult.getEntity().hurt(makeDamageSource(), damage);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return pDistance <= 65536;
    }

    protected DamageSource makeDamageSource() {
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(BULLET_DAMAGE_TYPE), this, getOwner());
    }
}
