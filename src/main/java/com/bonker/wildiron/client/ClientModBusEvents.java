package com.bonker.wildiron.client;

import com.bonker.wildiron.WildIron;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = WildIron.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModBusEvents {
    private static WildIronTrimManager trimManager;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        WildIronBEWLR.instance = new WildIronBEWLR(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels(), trimManager);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        trimManager = new WildIronTrimManager(Minecraft.getInstance().getTextureManager());
        event.registerReloadListener(trimManager);
        event.registerReloadListener((ResourceManagerReloadListener) pResourceManager ->
                WildIronBEWLR.instance.model = new WildIronModel(WildIronBEWLR.instance.entityModelSet.bakeLayer(WildIronModel.LAYER_LOCATION)));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WildIronModel.LAYER_LOCATION, WildIronModel::createLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WildIron.BULLET_ENTITY_TYPE.get(), BulletRenderer::new);
    }
}
