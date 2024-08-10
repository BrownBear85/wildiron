package com.bonker.wildiron;

import com.bonker.wildiron.entity.Bullet;
import com.bonker.wildiron.item.BulletItem;
import com.bonker.wildiron.item.CowboyHatItem;
import com.bonker.wildiron.item.WildIronItem;
import com.bonker.wildiron.item.AddItemModifier;
import com.bonker.wildiron.networking.WildIronNetwork;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    public static final RegistryObject<Item> WILD_IRON_ITEM = ITEMS.register("wild_iron",
            () -> new WildIronItem(new Item.Properties().durability(41)));

    public static final RegistryObject<CowboyHatItem> COWBOY_HAT = ITEMS.register("cowboy_hat",
            () -> new CowboyHatItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BulletItem> IRON_BULLET = ITEMS.register("iron_bullet",
            () -> new BulletItem(45.0F, 0.0F, new ResourceLocation(MODID, "textures/entity/iron_bullet.png"), new Item.Properties()));

    public static final RegistryObject<BulletItem> GOLD_BULLET = ITEMS.register("gold_bullet",
            () -> new BulletItem(45.0F, 0.5F, new ResourceLocation(MODID, "textures/entity/gold_bullet.png"), new Item.Properties()));

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
            }).build());

    public WildIron() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        ENTITY_TYPES.register(bus);
        LOOT_MODIFIER_TYPES.register(bus);

        WildIronNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(this::configureVillagerTrades);
        MinecraftForge.EVENT_BUS.addListener(this::setAnvilRepairCost);
    }

    private void configureVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.LEATHERWORKER) {
            event.getTrades().get(5).add(new BasicItemListing(50, new ItemStack(WildIron.COWBOY_HAT.get()), 1, 50));
        }
    }

    private void setAnvilRepairCost(AnvilUpdateEvent event) {
        if (event.getLeft().is(WILD_IRON_ITEM.get()) && event.getLeft().getItem().isValidRepairItem(event.getLeft(), event.getRight())) {
            int damage = event.getLeft().getDamageValue();
            int cost = damage / 2;
            if (event.getName() != null && !event.getName().isEmpty()) {
                cost++;
            }

            if (damage > 0) {
                ItemStack output = event.getLeft().copy();
                output.setDamageValue(0);
                event.setOutput(output);
                event.setCost(cost);
            }
        }
    }
}
