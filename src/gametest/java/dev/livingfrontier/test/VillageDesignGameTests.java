package dev.livingfrontier.test;

import com.google.gson.Gson;
import dev.livingfrontier.entity.TravellerEntity;
import dev.livingfrontier.entity.TraderEntity;
import dev.livingfrontier.entity.VillageGuardEntity;
import dev.livingfrontier.village.VillageLife;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class VillageDesignGameTests {
    private static final Layouts LAYOUTS = readLayouts();

    @GameTest(template = "test/empty", timeoutTicks = 200)
    public static void biomeVillagesKeepVanillaRoadsAndHouses(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (Village village : LAYOUTS.villages()) {
            var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            var holder = structures.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE,
                    vanilla("village_" + village.biome())));
            var settings = level.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS)
                    .getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
            var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                    ResourceKey.create(Registries.BIOME, vanilla(village.biome().equals("snowy") ? "snowy_plains" : village.biome())));
            var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), settings);
            var random = RandomState.create(settings.value(), level.registryAccess().lookupOrThrow(Registries.NOISE), 31);
            var state = generator.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET), random, 31);
            helper.assertTrue(!state.getPlacementsForStructure(holder).isEmpty(),
                    village.biome() + " retains vanilla natural village placement");
            for (long seed : new long[]{0, 31, 124}) {
                StructureStart start = generate(level, village, seed);
                helper.assertTrue(start.isValid(), village.biome() + " village generates for seed " + seed);
                if (!isFrontier(root(start))) {
                    helper.assertTrue(root(start).getElement().toString().contains("/zombie/"),
                            "Only rare abandoned villages retain the vanilla centre");
                    continue;
                }
                long roads = start.getPieces().stream().filter(piece -> piece instanceof PoolElementStructurePiece pool
                        && pool.getElement().toString().contains("minecraft:village/" + village.biome() + "/streets/")).count();
                long houses = start.getPieces().stream().filter(piece -> piece instanceof PoolElementStructurePiece pool
                        && pool.getElement().toString().contains("minecraft:village/" + village.biome() + "/houses/")).count();
                helper.assertTrue(roads >= 4 && houses > 0,
                        village.biome() + " connects its fortified hub to vanilla streets and outer houses");
                helper.assertTrue(root(start).getJunctions().size() >= 8,
                        village.biome() + " has populated resident and street junctions");
                var context = StructurePieceSerializationContext.fromLevel(level);
                var restored = StructureStart.loadStaticStart(context, start.createTag(context, start.getChunkPos()), seed);
                helper.assertTrue(restored != null && restored.isValid()
                        && restored.getPieces().size() == start.getPieces().size() && isFrontier(root(restored)),
                        "New village layouts survive saved structure starts");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test/village_arena", batch = "villageDesign", timeoutTicks = 120)
    public static void plainsHubHasWorkingHomesMarketsAndNativeResidents(GameTestHelper helper) {
        assertHub(helper, "plains");
    }

    @GameTest(template = "test/village_arena", batch = "villageDesign", timeoutTicks = 120)
    public static void desertHubHasWorkingHomesMarketsAndNativeResidents(GameTestHelper helper) {
        assertHub(helper, "desert");
    }

    @GameTest(template = "test/village_arena", batch = "villageDesign", timeoutTicks = 120)
    public static void savannaHubHasWorkingHomesMarketsAndNativeResidents(GameTestHelper helper) {
        assertHub(helper, "savanna");
    }

    @GameTest(template = "test/village_arena", batch = "villageDesign", timeoutTicks = 120)
    public static void taigaHubHasWorkingHomesMarketsAndNativeResidents(GameTestHelper helper) {
        assertHub(helper, "taiga");
    }

    @GameTest(template = "test/village_arena", batch = "villageDesign", timeoutTicks = 120)
    public static void snowyHubHasWorkingHomesMarketsAndNativeResidents(GameTestHelper helper) {
        assertHub(helper, "snowy");
    }

    private static void assertHub(GameTestHelper helper, String biome) {
        ServerLevel level = helper.getLevel();
        Village selected = null;
        for (Village candidate : LAYOUTS.villages()) {
            if (candidate.biome().equals(biome)) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) throw new IllegalArgumentException("Unknown village biome " + biome);
        Village village = selected;
        StructureStart start = frontierStart(level, village);
        PoolElementStructurePiece hub = root(start);
        BlockPos testPosition = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos origin = new BlockPos(testPosition.getX(), level.getMaxBuildHeight() - 40, testPosition.getZ());
        BoundingBox old = hub.getBoundingBox();
        int dx = origin.getX() - old.minX();
        int dy = origin.getY() - old.minY();
        int dz = origin.getZ() - old.minZ();
        start.getPieces().forEach(piece -> piece.move(dx, dy, dz));
        BoundingBox bounds = hub.getBoundingBox();
        RandomSource placementRandom = RandomSource.create(0x5E771EL + biome.hashCode());
        for (var piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pool && (pool == hub
                    || pool.getElement().toString().contains("/villagers/")
                    || pool.getElement().toString().contains("/common/")
                    || pool.getElement().toString().contains("/camel"))) {
                pool.place(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        placementRandom, pool.getBoundingBox(), BlockPos.ZERO, false);
            }
        }
        AABB area = AABB.of(bounds).inflate(16);
        var villagers = level.getEntitiesOfClass(Villager.class, area);
        helper.assertTrue(villagers.size() >= 2, village.biome() + " spawns actual vanilla villagers");
        String inhabitantPieces = start.getPieces().stream()
                .filter(piece -> piece instanceof PoolElementStructurePiece)
                .map(piece -> ((PoolElementStructurePiece) piece).getElement().toString())
                .filter(name -> name.contains("/villagers/") || name.contains("/common/") || name.contains("/camel"))
                .toList().toString();
        helper.runAfterDelay(1, () -> helper.assertTrue(
                !level.getEntities(EntityType.IRON_GOLEM, area, entity -> true).isEmpty(),
                village.biome() + " preserves the native iron golem; inhabitant pieces " + inhabitantPieces));
        villagers.forEach(resident -> resident.setNoAi(true));
        for (int[] pos : village.beds()) {
            var bed = level.getBlockState(at(hub, pos));
            helper.assertTrue(bed.getBlock() instanceof BedBlock && bed.getValue(BedBlock.PART) == BedPart.HEAD,
                    village.biome() + " has four intact usable beds");
        }
        helper.assertTrue(level.getBlockState(at(hub, village.bell())).is(Blocks.BELL),
                village.biome() + " has a supported meeting bell");
        for (int[] pos : village.jobs()) {
            helper.assertTrue(level.getBlockState(at(hub, pos)).is(Blocks.COMPOSTER)
                    || level.getBlockState(at(hub, pos)).is(Blocks.FLETCHING_TABLE)
                    || level.getBlockState(at(hub, pos)).is(Blocks.CARTOGRAPHY_TABLE)
                    || level.getBlockState(at(hub, pos)).is(Blocks.GRINDSTONE),
                    village.biome() + " preserves usable vanilla job sites");
        }
        for (int[] pos : village.chests()) {
            helper.assertTrue(level.getBlockEntity(at(hub, pos)) instanceof ChestBlockEntity,
                    village.biome() + " has vanilla house loot");
            ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(at(hub, pos));
            chest.unpackLootTable(null);
            helper.assertTrue(!chest.isEmpty(), village.biome() + " resolves the vanilla house loot table");
        }
        for (int[] pos : village.farms()) {
            helper.assertTrue(level.getBlockState(at(hub, pos)).is(Blocks.FARMLAND),
                    village.biome() + " has planted irrigated farms");
        }
        for (int[] pos : village.towers()) {
            helper.assertTrue(level.getBlockState(at(hub, pos)).is(Blocks.SPRUCE_PLANKS)
                    || level.getBlockState(at(hub, pos)).is(Blocks.OAK_PLANKS)
                    || level.getBlockState(at(hub, pos)).is(Blocks.ACACIA_PLANKS)
                    || level.getBlockState(at(hub, pos)).is(Blocks.JUNGLE_PLANKS),
                    village.biome() + " has accessible watchtower platforms");
        }
        helper.assertTrue(!level.getBlockState(at(hub, village.landmark())).isAir(),
                village.biome() + " has its fantasy landmark");
        if (village.biome().equals("snowy")) {
            helper.assertTrue(VillageLife.improve(level, at(hub, village.bell())),
                    "Snowy and paved village centres still gain frontier residents");
            var guards = level.getEntitiesOfClass(VillageGuardEntity.class, area);
            helper.assertTrue(guards.size() == 2 && guards.stream().filter(VillageGuardEntity::isArcher).count() == 1,
                    "The new plaza retains one swordsman and one archer");
            helper.assertTrue(level.getEntitiesOfClass(TravellerEntity.class, area).size() == 1,
                    "The new village retains its peaceful visitor");
            helper.assertTrue(level.getEntitiesOfClass(TraderEntity.class, area).size() == 1,
                    "The new village joins the cargo trade network");
        }
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(level.getBlockState(at(hub, village.bell())).is(Blocks.BELL),
                    village.biome() + " bell survives neighbor updates");
            for (int[] pos : village.beds()) {
                helper.assertTrue(level.getBlockState(at(hub, pos)).getBlock() instanceof BedBlock,
                        village.biome() + " beds survive neighbor updates");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "test/empty")
    public static void savedVanillaVillagesNeverSwitchToNewTemplates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var context = StructurePieceSerializationContext.fromLevel(level);
        for (Village village : LAYOUTS.villages()) {
            String oldName = village.biome().equals("plains") ? "plains_fountain_01" : village.biome() + "_meeting_point_1";
            String oldTemplate = "minecraft:village/" + village.biome() + "/town_centers/" + oldName;
            var element = StructurePoolElement.legacy(oldTemplate).apply(StructureTemplatePool.Projection.RIGID);
            var piece = new PoolElementStructurePiece(level.getStructureManager(), element, new BlockPos(8, 64, 8),
                    element.getGroundLevelDelta(), Rotation.NONE,
                    element.getBoundingBox(level.getStructureManager(), new BlockPos(8, 64, 8), Rotation.NONE));
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(
                    vanilla("village_" + village.biome()));
            var oldStart = new StructureStart(Objects.requireNonNull(structure), new ChunkPos(0, 0), 0,
                    new PiecesContainer(List.of(piece)));
            CompoundTag saved = oldStart.createTag(context, oldStart.getChunkPos());
            var restored = StructureStart.loadStaticStart(context, saved, 31);
            helper.assertTrue(restored != null && restored.isValid()
                    && root(restored).getElement().toString().contains(oldTemplate) && !isFrontier(root(restored)),
                    village.biome() + " saved village keeps its original template despite the new generation pool");
            helper.assertTrue(level.getStructureManager().get(vanilla(oldTemplate.substring("minecraft:".length()))).isPresent(),
                    "Original templates remain available for already generated village starts");
        }
        helper.succeed();
    }

    private static StructureStart frontierStart(ServerLevel level, Village village) {
        for (long seed = 0; seed < 10; seed++) {
            StructureStart start = generate(level, village, seed);
            if (start.isValid() && isFrontier(root(start))) {
                return start;
            }
        }
        throw new IllegalStateException("No new village centre generated for " + village.biome());
    }

    private static StructureStart generate(ServerLevel level, Village village, long seed) {
        var registries = level.registryAccess();
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var biome = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME,
                vanilla(village.biome().equals("snowy") ? "snowy_plains" : village.biome())));
        var source = new FixedBiomeSource(biome);
        var generator = new NoiseBasedChunkGenerator(source, settings);
        var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
        var structure = registries.registryOrThrow(Registries.STRUCTURE).getOrThrow(
                ResourceKey.create(Registries.STRUCTURE, vanilla("village_" + village.biome())));
        return structure.generate(registries, generator, source, random, level.getStructureManager(),
                seed, new ChunkPos(4, 4), 0, level, structure.biomes()::contains);
    }

    private static PoolElementStructurePiece root(StructureStart start) {
        return (PoolElementStructurePiece) start.getPieces().get(0);
    }

    private static boolean isFrontier(PoolElementStructurePiece piece) {
        return piece.getElement().toString().contains("livingfrontier:village/");
    }

    private static BlockPos at(PoolElementStructurePiece piece, int[] pos) {
        return piece.getPosition().offset(StructureTemplate.transform(
                SiteLayouts.pos(pos), Mirror.NONE, piece.getRotation(), BlockPos.ZERO));
    }

    private static ResourceLocation vanilla(String path) {
        return new ResourceLocation("minecraft", path);
    }

    private static Layouts readLayouts() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(VillageDesignGameTests.class.getResourceAsStream(
                "/data/livingfrontier/layouts/villages.json")), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Layouts.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read generated village layouts", exception);
        }
    }

    private record Layouts(Village[] villages) {
    }

    private record Village(String biome, String title, int[] size, String ground, String stone, String wood,
            int[][] beds, int[][] doors, int[][] chests, int[][] farms, int[][] jobs, int[][] towers,
            int[][] ladders, int[][] roads, int[][] residents, int[] bell, int[] landmark) {
    }
}
