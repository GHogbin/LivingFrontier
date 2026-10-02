package dev.livingfrontier.client;

import dev.livingfrontier.FrontierEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
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
    static final ModelLayerLocation BOAR = layer("boar");
    static final ModelLayerLocation PROWLER = layer("prowler");
    static final ModelLayerLocation SKY_WRAITH = layer("sky_wraith");
    static final ModelLayerLocation TRAVELLER = layer("traveller");
    static final ModelLayerLocation TRADER = layer("trader");
    static final ModelLayerLocation VILLAGE_GUARD = layer("village_guard");

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
        event.registerLayerDefinition(BOAR, BoarModel::createBodyLayer);
        event.registerLayerDefinition(PROWLER, ProwlerModel::createBodyLayer);
        event.registerLayerDefinition(SKY_WRAITH, SkyWraithModel::createBodyLayer);
        event.registerLayerDefinition(TRAVELLER, () -> FrontierPeopleModel.createBodyLayer(false));
        event.registerLayerDefinition(TRADER, () -> FrontierPeopleModel.createBodyLayer(false));
        event.registerLayerDefinition(VILLAGE_GUARD, () -> FrontierPeopleModel.createBodyLayer(true));
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
        event.registerEntityRenderer(FrontierEntities.BOAR.get(), FrontierRenderers.Boar::new);
        event.registerEntityRenderer(FrontierEntities.PROWLER.get(), FrontierRenderers.Prowler::new);
        event.registerEntityRenderer(FrontierEntities.SKY_WRAITH.get(), FrontierRenderers.SkyWraith::new);
        event.registerEntityRenderer(FrontierEntities.TRAVELLER.get(), FrontierRenderers.Traveller::new);
        event.registerEntityRenderer(FrontierEntities.TRADER.get(), FrontierRenderers.Trader::new);
        event.registerEntityRenderer(FrontierEntities.VILLAGE_GUARD.get(), FrontierRenderers.VillageGuard::new);
        event.registerEntityRenderer(FrontierEntities.GUARD_ARROW.get(), TippableArrowRenderer::new);
    }
}
