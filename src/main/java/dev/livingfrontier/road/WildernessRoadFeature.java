package dev.livingfrontier.road;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.LivingFrontier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WildernessRoadFeature extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, LivingFrontier.MOD_ID);
    public static final RegistryObject<WildernessRoadFeature> ROADS =
            FEATURES.register("wilderness_roads", WildernessRoadFeature::new);

    public WildernessRoadFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!FrontierConfig.WILDERNESS_ROADS.get() || !(context.level() instanceof WorldGenRegion region)
                || !region.getLevel().dimension().equals(Level.OVERWORLD)
                || !region.getLevel().structureManager().shouldGenerateStructures()
                || !(region.getChunk(region.getCenter().x, region.getCenter().z) instanceof ProtoChunk chunk)
                || chunk.getStatus().isOrAfter(ChunkStatus.FEATURES)) return false;
        WildernessRoadNetwork network = WildernessRoadNetwork.forLevel(region.getLevel());
        List<BoundingBox> protectedAreas = protectedStructures(region);
        int placed = buildRoads(region, network.routesForChunk(region.getCenter()), protectedAreas);
        if (FrontierConfig.ROADSIDE_SITES.get()) {
            for (var site : network.sitesForChunk(region.getCenter())) {
                placed += buildSite(region, site, protectedAreas);
            }
        }
        return placed > 0;
    }

    static int buildRoads(WorldGenRegion region, List<List<BlockPos>> routes, List<BoundingBox> protectedAreas) {
        ChunkPos bounds = region.getCenter();
        Map<Long, BlockPos> columns = new LinkedHashMap<>();
        for (List<BlockPos> route : routes) {
            for (BlockPos point : route) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = point.getX() + dx;
                        int z = point.getZ() + dz;
                        if (!inside(bounds, x, z)) continue;
                        BlockPos pos = new BlockPos(x, point.getY(), z);
                        columns.merge(ChunkPos.asLong(x, z), pos,
                                (first, second) -> first.getY() >= second.getY() ? first : second);
                    }

                }
            }
        }
        int placed = 0;
        for (BlockPos pos : columns.values()) {
            if (protectedAreas.stream().anyMatch(box -> inside(box, pos.getX(), pos.getZ(), 0))) continue;
            if (buildRoadColumn(region, pos)) placed++;
        }
        return placed;
    }

    static int buildSite(WorldGenRegion region, WildernessRoadNetwork.RoadsideSite site,
            List<BoundingBox> protectedAreas) {
        int radius = site.type() == WildernessRoadNetwork.SiteType.HAMLET ? 8 : 5;
        BoundingBox footprint = new BoundingBox(site.center().getX() - radius, site.center().getY() - 3,
                site.center().getZ() - radius, site.center().getX() + radius, site.center().getY() + 6,
                site.center().getZ() + radius);
        if (protectedAreas.stream().anyMatch(footprint::intersects)) return 0;
        return site.type() == WildernessRoadNetwork.SiteType.HAMLET
                ? buildHamlet(region, site.center(), protectedAreas)
                : buildCamp(region, site.center(), protectedAreas);
    }

    private static int buildCamp(WorldGenRegion region, BlockPos center, List<BoundingBox> protectedAreas) {
        int placed = prepareGround(region, center, 5, protectedAreas, Blocks.COARSE_DIRT.defaultBlockState());
        placed += set(region, center.above(), Blocks.CAMPFIRE.defaultBlockState(), protectedAreas);
        placed += set(region, center.offset(2, 1, 1), Blocks.BARREL.defaultBlockState(), protectedAreas);
        placed += set(region, center.offset(-2, 1, -1), Blocks.CRAFTING_TABLE.defaultBlockState(), protectedAreas);
        placed += set(region, center.offset(3, 1, -2), Blocks.HAY_BLOCK.defaultBlockState(), protectedAreas);
        placed += tent(region, center.offset(-3, 0, 2), DyeColor.WHITE, protectedAreas);
        placed += tent(region, center.offset(3, 0, 2), DyeColor.GREEN, protectedAreas);
        for (int[] offset : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            BlockPos post = center.offset(offset[0], 1, offset[1]);
            placed += set(region, post, Blocks.OAK_FENCE.defaultBlockState(), protectedAreas);
            placed += set(region, post.above(), Blocks.LANTERN.defaultBlockState(), protectedAreas);
        }
        return placed;
    }

    private static int buildHamlet(WorldGenRegion region, BlockPos center, List<BoundingBox> protectedAreas) {
        SitePalette palette = palette(region.getBiome(center));
        int placed = prepareGround(region, center, 8, protectedAreas, Blocks.DIRT_PATH.defaultBlockState());
        placed += hut(region, center.offset(-5, 0, 0), palette, Direction.SOUTH, protectedAreas);
        placed += hut(region, center.offset(5, 0, 0), palette, Direction.NORTH, protectedAreas);
        placed += set(region, center, Blocks.COBBLESTONE.defaultBlockState(), protectedAreas);
        placed += set(region, center.above(), Blocks.BELL.defaultBlockState(), protectedAreas);
        for (int x = -2; x <= 2; x++) {
            placed += set(region, center.offset(x, 0, -4), Blocks.DIRT_PATH.defaultBlockState(), protectedAreas);
            placed += set(region, center.offset(x, 0, 4), Blocks.DIRT_PATH.defaultBlockState(), protectedAreas);
        }
        placed += set(region, center.offset(-1, 1, 3), Blocks.COMPOSTER.defaultBlockState(), protectedAreas);
        placed += set(region, center.offset(1, 1, 3), Blocks.FLETCHING_TABLE.defaultBlockState(), protectedAreas);
        placed += set(region, center.offset(0, 1, -4), Blocks.BARREL.defaultBlockState(), protectedAreas);
        for (int[] offset : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            BlockPos post = center.offset(offset[0], 1, offset[1]);
            placed += set(region, post, Blocks.OAK_FENCE.defaultBlockState(), protectedAreas);
            placed += set(region, post.above(), Blocks.LANTERN.defaultBlockState(), protectedAreas);
        }
        return placed;
    }

    private static int tent(WorldGenRegion region, BlockPos center, DyeColor color,
            List<BoundingBox> protectedAreas) {
        BlockState wool = color == DyeColor.GREEN ? Blocks.GREEN_WOOL.defaultBlockState()
                : Blocks.WHITE_WOOL.defaultBlockState();
        int placed = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 1; z++) {
                int height = Math.abs(x) == 2 ? 2 : 3;
                placed += set(region, center.offset(x, height, z), wool, protectedAreas);
            }
        }
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-1, 1}) {
                placed += set(region, center.offset(x, 1, z), Blocks.OAK_FENCE.defaultBlockState(), protectedAreas);
            }
        }
        placed += set(region, center.offset(0, 1, 0), Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.NORTH), protectedAreas);
        placed += set(region, center.offset(0, 1, -1), Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.NORTH), protectedAreas);
        return placed;
    }

    private static int hut(WorldGenRegion region, BlockPos center, SitePalette palette, Direction door,
            List<BoundingBox> protectedAreas) {
        int placed = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                placed += set(region, center.offset(x, 0, z), palette.floor(), protectedAreas);
                boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
                boolean doorway = door == Direction.SOUTH && z == 2 && x == 0
                        || door == Direction.NORTH && z == -2 && x == 0;
                if (edge && !doorway) {
                    for (int y = 1; y <= 3; y++) {
                        BlockState wall = Math.abs(x) == 2 && Math.abs(z) == 2 ? palette.log() : palette.wall();
                        placed += set(region, center.offset(x, y, z), wall, protectedAreas);
                    }
                } else {
                    for (int y = 1; y <= 3; y++) placed += clear(region, center.offset(x, y, z), protectedAreas);
                }
                placed += set(region, center.offset(x, 4, z), palette.roof(), protectedAreas);
            }
        }
        BlockPos foot = center.offset(-1, 1, 0);
        placed += set(region, foot, Blocks.YELLOW_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.EAST), protectedAreas);
        placed += set(region, foot.east(), Blocks.YELLOW_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.EAST), protectedAreas);
        placed += set(region, center.offset(1, 1, 1), Blocks.BARREL.defaultBlockState(), protectedAreas);
        BlockPos lower = center.relative(door, 2).above();
        placed += set(region, lower, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, door)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), protectedAreas);
        placed += set(region, lower.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, door)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), protectedAreas);
        return placed;
    }

    private static int prepareGround(WorldGenRegion region, BlockPos center, int radius,
            List<BoundingBox> protectedAreas, BlockState ground) {
        int placed = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos floor = center.offset(x, 0, z);
                if (!supported(region, floor, 3)) continue;
                for (int y = -3; y < 0; y++) {
                    BlockPos support = floor.above(y);
                    if (region.getBlockState(support).isAir()) {
                        placed += set(region, support, Blocks.DIRT.defaultBlockState(), protectedAreas);
                    }
                }
                placed += set(region, floor, ground, protectedAreas);
                for (int y = 1; y <= 5; y++) placed += clear(region, floor.above(y), protectedAreas);
            }
        }
        return placed;
    }

    private static boolean buildRoadColumn(WorldGenRegion region, BlockPos surface) {
        if (region.isOutsideBuildHeight(surface.below(3)) || region.isOutsideBuildHeight(surface.above(3))
                || !region.getWorldBorder().isWithinBounds(surface)
                || region.getBiome(surface).is(BiomeTags.IS_OCEAN)) return false;
        boolean bridge = false;
        boolean supported = false;
        for (int dy = -3; dy <= 3; dy++) {
            BlockPos pos = surface.above(dy);
            BlockState state = region.getBlockState(pos);
            if (state.hasBlockEntity() || !natural(state) || state.getFluidState().is(FluidTags.LAVA)) return false;
            if (dy <= 0 && state.getFluidState().is(FluidTags.WATER)) bridge = true;
            if (dy <= 0 && !state.isAir() && state.getFluidState().isEmpty()) supported = true;
        }
        if (!bridge && !supported) return false;
        for (int dy = 1; dy <= 3; dy++) region.setBlock(surface.above(dy), Blocks.AIR.defaultBlockState(), 2);
        if (!bridge) {
            for (int dy = -3; dy < 0; dy++) {
                BlockPos support = surface.above(dy);
                if (region.getBlockState(support).isAir()) region.setBlock(support, Blocks.DIRT.defaultBlockState(), 2);
            }
        }
        region.setBlock(surface, surfaceFor(region.getBiome(surface), bridge), 2);
        return true;
    }

    private static boolean supported(WorldGenRegion region, BlockPos floor, int depth) {
        for (int offset = 0; offset >= -depth; offset--) {
            BlockState state = region.getBlockState(floor.above(offset));
            if (!state.isAir() && state.getFluidState().isEmpty()) return true;
        }
        return false;
    }

    public static BlockState surfaceFor(Holder<Biome> biome, boolean bridge) {
        if (bridge) return (biome.is(BiomeTags.IS_TAIGA) || biome.value().coldEnoughToSnow(new BlockPos(0, 64, 0))
                ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS).defaultBlockState();
        if (biome.is(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("desert"))))
            return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        if (biome.is(BiomeTags.IS_BADLANDS)) return Blocks.RED_SANDSTONE.defaultBlockState();
        if (biome.is(BiomeTags.IS_TAIGA) || biome.value().coldEnoughToSnow(new BlockPos(0, 64, 0)))
            return Blocks.GRAVEL.defaultBlockState();
        return Blocks.DIRT_PATH.defaultBlockState();
    }

    private static SitePalette palette(Holder<Biome> biome) {
        if (biome.is(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("desert")))) {
            return new SitePalette(Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
                    Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(),
                    Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        }
        if (biome.is(BiomeTags.IS_TAIGA) || biome.value().coldEnoughToSnow(new BlockPos(0, 64, 0))) {
            return new SitePalette(Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_LOG.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState());
        }
        return new SitePalette(Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState());
    }

    private static int set(WorldGenRegion region, BlockPos pos, BlockState state, List<BoundingBox> protectedAreas) {
        if (!inside(region.getCenter(), pos.getX(), pos.getZ()) || region.isOutsideBuildHeight(pos)
                || !region.getWorldBorder().isWithinBounds(pos)
                || protectedAreas.stream().anyMatch(box -> inside(box, pos.getX(), pos.getZ(), 0))) return 0;
        BlockState current = region.getBlockState(pos);
        if (current.hasBlockEntity()) return 0;
        region.setBlock(pos, state, 2);
        return 1;
    }

    private static int clear(WorldGenRegion region, BlockPos pos, List<BoundingBox> protectedAreas) {
        BlockState state = region.getBlockState(pos);
        if (!state.isAir() && !natural(state)) return 0;
        return set(region, pos, Blocks.AIR.defaultBlockState(), protectedAreas);
    }

    private static boolean natural(BlockState state) {
        return state.isAir() || state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.SAND) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.WATER) || state.is(Blocks.SMOOTH_SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.OAK_PLANKS) || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.GRASS_BLOCK);
    }

    private static boolean inside(ChunkPos chunk, int x, int z) {
        return x >= chunk.getMinBlockX() && x <= chunk.getMaxBlockX()
                && z >= chunk.getMinBlockZ() && z <= chunk.getMaxBlockZ();
    }

    private static boolean inside(BoundingBox box, int x, int z, int margin) {
        return x >= box.minX() - margin && x <= box.maxX() + margin
                && z >= box.minZ() - margin && z <= box.maxZ() + margin;
    }

    private static List<BoundingBox> protectedStructures(WorldGenRegion region) {
        List<BoundingBox> result = new ArrayList<>();
        var center = region.getChunk(region.getCenter().x, region.getCenter().z);
        center.getAllReferences().forEach((structure, references) -> {
            for (long reference : references) {
                ChunkPos position = new ChunkPos(reference);
                if (!region.hasChunk(position.x, position.z)) continue;
                var chunk = region.getChunk(position.x, position.z, ChunkStatus.STRUCTURE_STARTS, false);
                if (chunk == null) continue;
                var start = chunk.getStartForStructure(structure);
                if (start == null || !start.isValid()) continue;
                for (var piece : start.getPieces()) {
                    if (!(piece instanceof PoolElementStructurePiece pool) || !isVillageStreet(pool)) {
                        result.add(piece.getBoundingBox());
                    }
                }
            }
        });
        return List.copyOf(result);
    }

    private static boolean isVillageStreet(PoolElementStructurePiece piece) {
        return piece.getElement().toString().contains("minecraft:village/")
                && piece.getElement().toString().contains("/streets/");
    }

    private record SitePalette(BlockState wall, BlockState log, BlockState roof, BlockState floor) {
    }
}
