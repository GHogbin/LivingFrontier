package dev.livingfrontier.client;

import dev.livingfrontier.FrontierEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "livingfrontier", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LivingFrontierClient {
    static final ModelLayerLocation DEER = layer("deer");
    static final ModelLayerLocation SONGBIRD = layer("songbird");
    static final ModelLayerLocation FIREFLY = layer("firefly");
    static final ModelLayerLocation RAIDER = layer("raider");
    static final ModelLayerLocation WARLORD = layer("warlord");

    private LivingFrontierClient() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(FrontierEntities.id(name), "main");
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DEER, DeerModel::createBodyLayer);
        event.registerLayerDefinition(SONGBIRD, SongbirdModel::createBodyLayer);
        event.registerLayerDefinition(FIREFLY, FireflyModel::createBodyLayer);
        event.registerLayerDefinition(RAIDER, () -> RaiderModel.createBodyLayer(false));
        event.registerLayerDefinition(WARLORD, () -> RaiderModel.createBodyLayer(true));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FrontierEntities.DEER.get(), FrontierRenderers.Deer::new);
        event.registerEntityRenderer(FrontierEntities.SONGBIRD.get(), FrontierRenderers.Songbird::new);
        event.registerEntityRenderer(FrontierEntities.FIREFLY.get(), FrontierRenderers.Firefly::new);
        event.registerEntityRenderer(FrontierEntities.RAIDER.get(),
                context -> new FrontierRenderers.Raider<>(context, false));
        event.registerEntityRenderer(FrontierEntities.WARLORD.get(),
                context -> new FrontierRenderers.Raider<>(context, true));
    }
}
