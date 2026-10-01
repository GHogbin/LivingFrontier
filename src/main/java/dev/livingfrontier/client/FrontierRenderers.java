package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.entity.BoarEntity;
import dev.livingfrontier.entity.DeerEntity;
import dev.livingfrontier.entity.FireflyEntity;
import dev.livingfrontier.entity.ProwlerEntity;
import dev.livingfrontier.entity.RaiderEntity;
import dev.livingfrontier.entity.SkyWraithEntity;
import dev.livingfrontier.entity.SongbirdEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

final class FrontierRenderers {
    private FrontierRenderers() {
    }

    private abstract static class FrontierRenderer<T extends Mob, M extends EntityModel<T>>
            extends MobRenderer<T, M> {
        private final ResourceLocation texture;
        private final float visualScale;

        FrontierRenderer(EntityRendererProvider.Context context, M model, String texture,
                float shadow, float visualScale) {
            super(context, model, shadow * visualScale);
            this.texture = FrontierEntities.id("textures/entity/" + texture + ".png");
            this.visualScale = visualScale;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }

        @Override
        protected void scale(T entity, PoseStack poses, float partialTick) {
            // These plain EntityModels render their root directly, without vanilla young-model scaling.
            float size = visualScale * (entity.isBaby() ? 0.5F : 1.0F);
            poses.scale(size, size, size);
        }
    }

    static final class Deer extends FrontierRenderer<DeerEntity, DeerModel> {
        Deer(EntityRendererProvider.Context context) {
            super(context, new DeerModel(context.bakeLayer(LivingFrontierClient.DEER)), "deer", 0.45F, 1.0F);
        }
    }

    static final class Songbird extends FrontierRenderer<SongbirdEntity, SongbirdModel> {
        Songbird(EntityRendererProvider.Context context) {
            super(context, new SongbirdModel(context.bakeLayer(LivingFrontierClient.SONGBIRD)),
                    "songbird", 0.16F, 1.0F);
        }
    }

    static final class Firefly extends FrontierRenderer<FireflyEntity, FireflyModel> {
        Firefly(EntityRendererProvider.Context context) {
            super(context, new FireflyModel(context.bakeLayer(LivingFrontierClient.FIREFLY)),
                    "firefly", 0.08F, 1.0F);
        }

        @Override
        protected int getBlockLightLevel(FireflyEntity entity, BlockPos pos) {
            // Fullbright geometry, not dynamic light placed into the world.
            return entity.isNight() ? 15 : super.getBlockLightLevel(entity, pos);
        }
    }

    static final class Boar extends FrontierRenderer<BoarEntity, BoarModel> {
        Boar(EntityRendererProvider.Context context) {
            super(context, new BoarModel(context.bakeLayer(LivingFrontierClient.BOAR)),
                    "boar", 0.5F, 1.0F);
        }
    }

    static final class Prowler extends FrontierRenderer<ProwlerEntity, ProwlerModel> {
        Prowler(EntityRendererProvider.Context context) {
            super(context, new ProwlerModel(context.bakeLayer(LivingFrontierClient.PROWLER)),
                    "prowler", 0.4F, 1.0F);
        }
    }

    static final class SkyWraith extends FrontierRenderer<SkyWraithEntity, SkyWraithModel> {
        SkyWraith(EntityRendererProvider.Context context) {
            super(context, new SkyWraithModel(context.bakeLayer(LivingFrontierClient.SKY_WRAITH)),
                    "sky_wraith", 0.18F, 1.0F);
        }

        @Override
        protected int getBlockLightLevel(SkyWraithEntity entity, BlockPos pos) {
            // Emissive geometry only; this does not place light into the world.
            return 15;
        }

        @Override
        protected int getSkyLightLevel(SkyWraithEntity entity, BlockPos pos) {
            return 15;
        }
    }

    static final class Raider<T extends RaiderEntity> extends FrontierRenderer<T, RaiderModel<T>> {
        Raider(EntityRendererProvider.Context context, boolean warlord) {
            super(context, new RaiderModel<>(context.bakeLayer(
                    warlord ? LivingFrontierClient.WARLORD : LivingFrontierClient.RAIDER)),
                    warlord ? "warlord" : "raider", 0.4F, warlord ? 1.32F : 1.0F);
        }
    }
}
