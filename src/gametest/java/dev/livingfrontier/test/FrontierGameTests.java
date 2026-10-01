package dev.livingfrontier.test;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.entity.DeerEntity;
import dev.livingfrontier.entity.RaiderEntity;
import dev.livingfrontier.entity.WarlordEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class FrontierGameTests {
    @GameTest(template = "test/empty")
    public static void wildlifeAndEggs(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.GRASS_BLOCK);
        DeerEntity deer = helper.spawn(FrontierEntities.DEER.get(), 2, 2, 2);
        helper.assertTrue(deer.getMaxHealth() == 18, "Deer health");
        helper.assertTrue(deer.isFood(new ItemStack(Items.WHEAT)), "Deer eat wheat");
        helper.assertTrue(deer.isFood(new ItemStack(Items.SWEET_BERRIES)), "Deer eat berries");
        helper.assertTrue(!deer.isFood(new ItemStack(Items.STONE)), "Deer do not eat stone");
        helper.assertTrue(deer.getBreedOffspring(helper.getLevel(), deer) != null, "Deer offspring");
        var bird = helper.spawn(FrontierEntities.SONGBIRD.get(), 4, 3, 2);
        helper.assertTrue(bird.isFood(new ItemStack(Items.WHEAT_SEEDS)), "Bird seed food");
        helper.assertTrue(bird.getBreedOffspring(helper.getLevel(), bird) != null, "Bird offspring");
        helper.assertTrue(!bird.causeFallDamage(100, 1, bird.damageSources().fall()), "Bird fall immunity");
        var firefly = helper.spawn(FrontierEntities.FIREFLY.get(), 6, 3, 2);
        helper.assertTrue(firefly.isNoGravity(), "Fireflies hover");
        var boar = helper.spawn(FrontierEntities.BOAR.get(), 2, 2, 5);
        helper.assertTrue(boar.getMaxHealth() == 24 && boar.getTarget() == null, "Boars start neutral");
        helper.assertTrue(boar.isFood(new ItemStack(Items.CARROT))
                && boar.getBreedOffspring(helper.getLevel(), boar) != null, "Boar breeding");
        var prowler = helper.spawn(FrontierEntities.PROWLER.get(), 4, 2, 5);
        helper.assertTrue(prowler.getMaxHealth() == 20, "Prowler health");
        var wraith = helper.spawn(FrontierEntities.SKY_WRAITH.get(), 6, 4, 5);
        helper.assertTrue(wraith.getMaxHealth() == 18 && wraith.isNoGravity() && !wraith.isDiving(), "Flying wraith starts idle");
        helper.assertTrue(!wraith.causeFallDamage(100, 1, wraith.damageSources().fall()), "Wraith fall immunity");
        for (String name : List.of("deer", "songbird", "firefly", "raider", "warlord", "boar", "prowler", "sky_wraith")) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(FrontierEntities.id(name + "_spawn_egg")), name + " egg");
        }
        helper.succeed();
    }

    @GameTest(template = "test/empty")
    public static void forgeBiomeSpawns(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var forest = biomes.getOrThrow(Biomes.FOREST).getMobSettings();
        var swamp = biomes.getOrThrow(Biomes.SWAMP).getMobSettings();
        helper.assertTrue(forest.getMobs(MobCategory.CREATURE).unwrap().stream().anyMatch(
                entry -> entry.type == FrontierEntities.DEER.get() && entry.getWeight().asInt() == FrontierConfig.DEER_WEIGHT.get()),
                "Forge modifier adds configured deer spawning");
        helper.assertTrue(forest.getMobs(MobCategory.CREATURE).unwrap().stream().anyMatch(
                entry -> entry.type == FrontierEntities.SONGBIRD.get()), "Forge modifier adds songbirds");
        helper.assertTrue(swamp.getMobs(MobCategory.AMBIENT).unwrap().stream().anyMatch(
                entry -> entry.type == FrontierEntities.FIREFLY.get()), "Forge modifier adds fireflies");
        helper.assertTrue(forest.getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(
                entry -> entry.type == FrontierEntities.RAIDER.get() && entry.minCount == 3 && entry.maxCount == 5),
                "Forge modifier adds raider groups");
        helper.assertTrue(forest.getMobs(MobCategory.MONSTER).unwrap().stream().noneMatch(
                entry -> entry.type == FrontierEntities.WARLORD.get()), "Boss never spawns naturally");
        helper.assertTrue(forest.getMobs(MobCategory.CREATURE).unwrap().stream().anyMatch(
                entry -> entry.type == FrontierEntities.BOAR.get()), "Boar biome spawning");
        helper.assertTrue(forest.getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(
                entry -> entry.type == FrontierEntities.PROWLER.get() && entry.minCount == 2 && entry.maxCount == 3), "Prowler pack spawning");
        helper.assertTrue(forest.getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(
                entry -> entry.type == FrontierEntities.SKY_WRAITH.get()), "Flying hostile biome spawning");
        helper.succeed();
    }

    @GameTest(template = "test/empty")
    public static void campHasGeneratedRoot(GameTestHelper helper) {
        generatedRoot(helper, "raider_camp");
        helper.succeed();
    }

    @GameTest(template = "test/empty")
    public static void keepHasGeneratedRoot(GameTestHelper helper) {
        generatedRoot(helper, "ruined_keep");
        helper.succeed();
    }

    private static void generatedRoot(GameTestHelper helper, String name) {
        ServerLevel level = helper.getLevel();
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, FrontierEntities.id(name)));
        var biomeSource = new FixedBiomeSource(level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
        var settings = level.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator = new NoiseBasedChunkGenerator(biomeSource, settings);
        for (long seed : new long[]{0, 31, 124}) {
            var randomState = RandomState.create(settings.value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var start = holder.value().generate(level.registryAccess(), generator, biomeSource,
                    randomState, level.getStructureManager(), seed, new ChunkPos(4, 4),
                    0, level, holder.value().biomes()::contains);
            helper.assertTrue(start.isValid() && start.getPieces().size() == 1,
                    name + " generates its root template for seed " + seed);
        }
    }

    @GameTest(template = "test/empty", timeoutTicks = 200)
    public static void naturalOverworldBases(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registries = level.registryAccess();
        var biomes = MultiNoiseBiomeSource.createFromPreset(registries.registryOrThrow(
                Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator = new NoiseBasedChunkGenerator(biomes, settings);
        for (long seed : new long[]{0, 123456789}) {
            var randomState = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
            var state = generator.createState(registries.lookupOrThrow(Registries.STRUCTURE_SET), randomState, seed);
            for (String name : List.of("raider_camp", "ruined_keep")) {
                var holder = registries.registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(
                        ResourceKey.create(Registries.STRUCTURE, FrontierEntities.id(name)));
                var placements = state.getPlacementsForStructure(holder);
                helper.assertTrue(placements.size() == 1 && placements.get(0) instanceof RandomSpreadStructurePlacement,
                        name + " has a natural placement");
                var placement = (RandomSpreadStructurePlacement) placements.get(0);
                int spacing = name.equals("raider_camp") ? 28 : 52;
                boolean found = false;
                for (int x = -6; x <= 6 && !found; x++) {
                    for (int z = -6; z <= 6 && !found; z++) {
                        ChunkPos candidate = placement.getPotentialStructureChunk(seed, x * spacing, z * spacing);
                        if (!placement.isStructureChunk(state, candidate.x, candidate.z)) {
                            continue;
                        }
                        Structure structure = holder.value();
                        var start = structure.generate(registries, generator, biomes, randomState,
                                level.getStructureManager(), seed, candidate, 0, level, structure.biomes()::contains);
                        found = start.isValid() && start.getPieces().size() == 1;
                    }
                }
                helper.assertTrue(found, name + " has natural generated candidates for seed " + seed);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test/arena", timeoutTicks = 150)
    public static void guardedSitesAndBossPersistence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos campOrigin = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos keepOrigin = helper.absolutePos(new BlockPos(29, 1, 1));
        place(helper, "raider_camp", campOrigin);
        place(helper, "ruined_keep", keepOrigin);
        AABB campArea = new AABB(campOrigin).expandTowards(21, 9, 21);
        AABB keepArea = new AABB(keepOrigin).expandTowards(29, 15, 29);
        List<RaiderEntity> campGuards = level.getEntitiesOfClass(RaiderEntity.class, campArea);
        List<RaiderEntity> keepGuards = level.getEntitiesOfClass(RaiderEntity.class, keepArea,
                guard -> !(guard instanceof WarlordEntity));
        List<WarlordEntity> bosses = level.getEntitiesOfClass(WarlordEntity.class, keepArea);
        helper.assertTrue(campGuards.size() == 4, "Four camp guards");
        helper.assertTrue(keepGuards.size() == 6, "Six keep guards");
        helper.assertTrue(bosses.size() == 1, "Exactly one keep boss");
        helper.assertTrue(campGuards.stream().allMatch(guard -> guard.isGuard() && guard.isPersistenceRequired()), "Guards persist");
        WarlordEntity boss = bosses.get(0);
        helper.assertTrue(boss.getHealth() == 160 && boss.isGuard() && boss.isPersistenceRequired(), "Persistent boss");
        CompoundTag saved = new CompoundTag();
        boss.save(saved);
        Entity restored = EntityType.create(saved, level).orElseThrow();
        helper.assertTrue(restored instanceof WarlordEntity restoredBoss && restoredBoss.isGuard()
                && restoredBoss.isPersistenceRequired() && restoredBoss.getHealth() == 160, "Boss save round trip");
        chest(helper, campOrigin.offset(5, 2, 6), "raider_camp");
        chest(helper, keepOrigin.offset(10, 1, 7), "ruined_keep");
        boss.hurt(boss.damageSources().genericKill(), 1000);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(level.getEntitiesOfClass(WarlordEntity.class, keepArea).stream().noneMatch(Entity::isAlive),
                    "Defeated boss stays defeated");
            var drops = level.getEntitiesOfClass(ItemEntity.class, keepArea);
            helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.DIAMOND)), "Boss drops diamonds");
            helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE)),
                    "Boss drops sentry trim");
            helper.succeed();
        });
    }

    private static void place(GameTestHelper helper, String name, BlockPos origin) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager().get(FrontierEntities.id(name)).orElseThrow();
        BoundingBox bounds = BoundingBox.fromCorners(origin, origin.offset(template.getSize()).offset(-1, -1, -1));
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false).setFinalizeEntities(true)
                .setBoundingBox(bounds).setKnownShape(true);
        helper.assertTrue(template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2), name + " placed");
        helper.assertTrue(level.getBlockState(origin).is(name.equals("raider_camp") ? Blocks.GRASS_BLOCK : Blocks.COBBLESTONE),
                name + " floor decoded");
    }

    private static void chest(GameTestHelper helper, BlockPos position, String name) {
        var blockEntity = helper.getLevel().getBlockEntity(position);
        helper.assertTrue(blockEntity instanceof ChestBlockEntity, name + " chest exists");
        ChestBlockEntity chest = (ChestBlockEntity) blockEntity;
        chest.unpackLootTable(null);
        helper.assertTrue(!chest.isEmpty(), name + " chest loot");
    }
}
