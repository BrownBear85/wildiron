package com.bonker.wildiron;

import com.bonker.wildiron.entity.Bullet;
import com.bonker.wildiron.item.BulletItem;
import com.bonker.wildiron.item.CowboyHatItem;
import com.bonker.wildiron.item.WildIronItem;
import com.bonker.wildiron.item.AddItemModifier;
import com.bonker.wildiron.networking.WildIronNetwork;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(WildIron.MODID)
public class WildIron {
    public static final String MODID = "wildiron";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_TYPES = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<Item> WILD_IRON_ITEM = ITEMS.register("wild_iron",
            () -> new WildIronItem(new Item.Properties().durability(65)));

    public static final RegistryObject<CowboyHatItem> COWBOY_HAT = ITEMS.register("cowboy_hat",
            () -> new CowboyHatItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BulletItem> IRON_BULLET = ITEMS.register("iron_bullet",
            () -> new BulletItem(10.0F, 0.0F, new ResourceLocation(MODID, "textures/entity/iron_bullet.png"), new Item.Properties()));

    public static final RegistryObject<BulletItem> GOLD_BULLET = ITEMS.register("gold_bullet",
            () -> new BulletItem(10.0F, 0.5F, new ResourceLocation(MODID, "textures/entity/gold_bullet.png"), new Item.Properties()));

    public static final RegistryObject<BulletItem> DIAMOND_BULLET = ITEMS.register("diamond_bullet",
            () -> new BulletItem(35.0F, 0.0F, new ResourceLocation(MODID, "textures/entity/diamond_bullet.png"), new Item.Properties()));

    public static final RegistryObject<BulletItem> NETHERITE_BULLET = ITEMS.register("netherite_bullet",
            () -> new BulletItem(45.0F, 0.5F, new ResourceLocation(MODID, "textures/entity/netherite_bullet.png"), new Item.Properties()));

    public static final RegistryObject<EntityType<Bullet>> BULLET_ENTITY_TYPE = ENTITY_TYPES.register("bullet",
            () -> EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(16)
                    .updateInterval(10)
                    .build("wildiron:bullet"));

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM_MODIFIER_TYPE = LOOT_MODIFIER_TYPES.register("add_item",
            () -> AddItemModifier.CODEC);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(MODID, () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(WILD_IRON_ITEM.get()))
            .title(Component.translatable("itemGroup.wildiron"))
            .displayItems((parameters, output) -> {
                output.accept(WILD_IRON_ITEM.get());
                output.accept(COWBOY_HAT.get());
                output.accept(IRON_BULLET.get());
                output.accept(GOLD_BULLET.get());
                output.accept(DIAMOND_BULLET.get());
                output.accept(NETHERITE_BULLET.get());
            }).build());

    public static final RegistryObject<SoundEvent> WHOOSH = SOUND_EVENTS.register("whoosh",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "whoosh")));

    public static final RegistryObject<SoundEvent> LOAD = SOUND_EVENTS.register("load",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "load")));

    public static final RegistryObject<SoundEvent> FIRE = SOUND_EVENTS.register("fire",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "fire")));

    public static final RegistryObject<SoundEvent> EMPTY = SOUND_EVENTS.register("empty",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "empty")));

    public WildIron() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        ENTITY_TYPES.register(bus);
        LOOT_MODIFIER_TYPES.register(bus);
        SOUND_EVENTS.register(bus);

        WildIronNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(this::configureVillagerTrades);
        MinecraftForge.EVENT_BUS.addListener(this::setAnvilRepairCost);
        MinecraftForge.EVENT_BUS.addListener(this::swapHandItems);
    }

    private void configureVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.LEATHERWORKER) {
            event.getTrades().get(5).add(new BasicItemListing(56, new ItemStack(WildIron.COWBOY_HAT.get()), 1, 20));
        }
        if (event.getType() == VillagerProfession.WEAPONSMITH) {
            event.getTrades().get(1).add(new BasicItemListing(32, new ItemStack(WildIron.WILD_IRON_ITEM.get()), 3, 15));
        }
    }

    private void setAnvilRepairCost(AnvilUpdateEvent event) {
        if (event.getRight().getItem() instanceof WildIronItem) {
            event.setCanceled(true);
        } else if (event.getLeft().getItem() instanceof WildIronItem item &&
                item.isDamageable(event.getLeft()) &&
                event.getLeft().isDamaged() &&
                item.isValidRepairItem(event.getLeft(), event.getRight())) {
            int cost = 0;
            int damage = event.getLeft().getDamageValue();

            // charge 1 material and 1 level per quarter durability repaired
            int increment = event.getLeft().getMaxDamage() / 4;
            while (damage > 0 && event.getRight().getCount() > cost) {
                damage = Math.max(0, damage - increment);
                cost++;
            }

            ItemStack output = event.getLeft().copy();
            output.setDamageValue(damage);

            // add 1 to cost if the name is changed
            String name = event.getName();
            if (name != null && !Util.isBlank(name)) {
                if (!name.equals(event.getLeft().getHoverName().getString())) {
                    cost++;
                    output.setHoverName(Component.literal(name));
                }
            } else if (event.getLeft().hasCustomHoverName()) {
                cost++;
                output.resetHoverName();
            }

            event.setCost(cost);
            event.setMaterialCost(cost);
            event.setOutput(output);
        }
    }

    private void swapHandItems(LivingSwapItemsEvent.Hands event) {
    }
}
