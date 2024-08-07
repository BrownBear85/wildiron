package com.bonker.wildiron.client;

import com.bonker.wildiron.WildIron;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.armortrim.ArmorTrim;

import java.util.Map;

public class WildIronTrimManager extends TextureAtlasHolder {
    private static final ResourceLocation BASE_LOCATION = new ResourceLocation(WildIron.MODID, "item/wild_iron");
    private static final Map<ArmorTrim, ResourceLocation> SPRITE_LOOKUP = new Object2ObjectArrayMap<>();

    public WildIronTrimManager(TextureManager pTextureManager) {
        super(pTextureManager, new ResourceLocation(WildIron.MODID, "textures/atlas/wild_iron_trims.png"), new ResourceLocation(WildIron.MODID, "wild_iron_trims"));
    }

    public TextureAtlasSprite get(ArmorTrim trim) {
        return getSprite(SPRITE_LOOKUP.computeIfAbsent(trim, armorTrim -> new ResourceLocation(WildIron.MODID, "trims/" + trim.pattern().value().assetId().getPath() + "_" + trim.material().value().assetName())));
    }

    public TextureAtlasSprite getBase() {
        return getSprite(BASE_LOCATION);
    }
}
