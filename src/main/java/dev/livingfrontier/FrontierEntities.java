package dev.livingfrontier;

import dev.livingfrontier.entity.DeerEntity;
import dev.livingfrontier.entity.BoarEntity;
import dev.livingfrontier.entity.ProwlerEntity;
import dev.livingfrontier.entity.SkyWraithEntity;
import dev.livingfrontier.entity.TravellerEntity;
import dev.livingfrontier.entity.TraderEntity;
import dev.livingfrontier.entity.VillageGuardEntity;
import dev.livingfrontier.entity.GuardArrowEntity;
import dev.livingfrontier.entity.FireflyEntity;
import dev.livingfrontier.entity.RaiderEntity;
import dev.livingfrontier.entity.SongbirdEntity;
import dev.livingfrontier.entity.WarlordEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FrontierEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LivingFrontier.MOD_ID);
    public static final RegistryObject<EntityType<DeerEntity>> DEER =
            register("deer", DeerEntity::new, MobCategory.CREATURE, 0.75F, 1.5F);
    public static final RegistryObject<EntityType<SongbirdEntity>> SONGBIRD =
            register("songbird", SongbirdEntity::new, MobCategory.CREATURE, 0.35F, 0.45F);
    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY =
            register("firefly", FireflyEntity::new, MobCategory.AMBIENT, 0.2F, 0.2F);
    public static final RegistryObject<EntityType<RaiderEntity>> RAIDER =
            register("raider", RaiderEntity::new, MobCategory.MONSTER, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<WarlordEntity>> WARLORD =
            register("warlord", WarlordEntity::new, MobCategory.MONSTER, 0.9F, 2.7F);
    public static final RegistryObject<EntityType<BoarEntity>> BOAR =
            register("boar", BoarEntity::new, MobCategory.CREATURE, 0.85F, 0.9F);
    public static final RegistryObject<EntityType<ProwlerEntity>> PROWLER =
            register("prowler", ProwlerEntity::new, MobCategory.MONSTER, 0.7F, 0.9F);
    public static final RegistryObject<EntityType<SkyWraithEntity>> SKY_WRAITH =
            register("sky_wraith", SkyWraithEntity::new, MobCategory.MONSTER, 0.8F, 0.7F);
    public static final RegistryObject<EntityType<TravellerEntity>> TRAVELLER =
            register("traveller", TravellerEntity::new, MobCategory.CREATURE, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<TraderEntity>> TRADER =
            register("trader", TraderEntity::new, MobCategory.CREATURE, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<VillageGuardEntity>> VILLAGE_GUARD =
            register("village_guard", VillageGuardEntity::new, MobCategory.CREATURE, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<GuardArrowEntity>> GUARD_ARROW =
            register("guard_arrow", GuardArrowEntity::new, MobCategory.MISC, 0.5F, 0.5F);

    private FrontierEntities() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(LivingFrontier.MOD_ID, path);
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> register(
            String name, EntityType.EntityFactory<T> factory, MobCategory category, float width, float height) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.of(factory, category)
                .sized(width, height).clientTrackingRange(10).build(id(name).toString()));
    }

    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(DEER.get(), DeerEntity.createAttributes().build());
        event.put(SONGBIRD.get(), SongbirdEntity.createAttributes().build());
        event.put(FIREFLY.get(), FireflyEntity.createAttributes().build());
        event.put(RAIDER.get(), RaiderEntity.createAttributes().build());
        event.put(WARLORD.get(), WarlordEntity.createAttributes().build());
        event.put(BOAR.get(), BoarEntity.createAttributes().build());
        event.put(PROWLER.get(), ProwlerEntity.createAttributes().build());
        event.put(SKY_WRAITH.get(), SkyWraithEntity.createAttributes().build());
        event.put(TRAVELLER.get(), TravellerEntity.createAttributes().build());
        event.put(TRADER.get(), TraderEntity.createAttributes().build());
        event.put(VILLAGE_GUARD.get(), VillageGuardEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        var heightmap = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        var operation = SpawnPlacementRegisterEvent.Operation.REPLACE;
        event.register(DEER.get(), SpawnPlacements.Type.ON_GROUND, heightmap, DeerEntity::canSpawn, operation);
        event.register(SONGBIRD.get(), SpawnPlacements.Type.NO_RESTRICTIONS, heightmap, SongbirdEntity::canSpawn, operation);
        event.register(FIREFLY.get(), SpawnPlacements.Type.NO_RESTRICTIONS, heightmap, FireflyEntity::canSpawn, operation);
        event.register(RAIDER.get(), SpawnPlacements.Type.ON_GROUND, heightmap, RaiderEntity::canSpawn, operation);
        event.register(WARLORD.get(), SpawnPlacements.Type.ON_GROUND, heightmap,
                (type, level, reason, pos, random) -> false, operation);
        event.register(BOAR.get(), SpawnPlacements.Type.ON_GROUND, heightmap, BoarEntity::canSpawn, operation);
        event.register(PROWLER.get(), SpawnPlacements.Type.ON_GROUND, heightmap, ProwlerEntity::canSpawn, operation);
        event.register(SKY_WRAITH.get(), SpawnPlacements.Type.NO_RESTRICTIONS, heightmap, SkyWraithEntity::canSpawn, operation);
        event.register(TRAVELLER.get(), SpawnPlacements.Type.ON_GROUND, heightmap, TravellerEntity::canSpawn, operation);
        event.register(TRADER.get(), SpawnPlacements.Type.ON_GROUND, heightmap,
                (type, level, reason, pos, random) -> false, operation);
        event.register(VILLAGE_GUARD.get(), SpawnPlacements.Type.ON_GROUND, heightmap,
                (type, level, reason, pos, random) -> false, operation);
    }
}
