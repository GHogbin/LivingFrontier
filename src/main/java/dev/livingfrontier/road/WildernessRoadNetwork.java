package dev.livingfrontier.road;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.LivingFrontier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingFrontier.MOD_ID)
public final class WildernessRoadNetwork {
    private static final Map<ServerLevel, WildernessRoadNetwork> NETWORKS = new IdentityHashMap<>();
    private static final int[][] DIRECTIONS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static final int TERRAIN_STEP = 4;
    private static final int MAX_EARTHWORK = 3;
    private static final int MAX_WATER_DEPTH = 6;
    private static final int MAX_WATER_RUN = 24;
    private static final int MIN_ROAD_LENGTH = 64;

    private final ServerLevel level;
    private final ChunkGenerator generator;
    private final ChunkGeneratorStructureState state;
    private final LevelHeightAccessor heights;
    private final int regionSize;
    private final int spurLength;
    private final Map<Long, RoadCell> cells = cache(192);
    private final Map<Long, TerrainSample> terrain = cache(8192);
    private final Map<Long, RoadsideSite> sites = cache(1024);

    private WildernessRoadNetwork(ServerLevel level, int regionSize, int spurLength) {
        this.level = level;
        generator = level.getChunkSource().getGenerator();
        state = level.getChunkSource().getGeneratorState();
        heights = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
        this.regionSize = regionSize;
        this.spurLength = spurLength;
    }

    public static WildernessRoadNetwork forLevel(ServerLevel level) {
        synchronized (NETWORKS) {
            int regionSize = FrontierConfig.ROAD_REGION_SIZE.get();
            int spurLength = FrontierConfig.ROAD_SPUR_LENGTH.get();
            WildernessRoadNetwork network = NETWORKS.get(level);
            if (network == null || network.regionSize != regionSize || network.spurLength != spurLength) {
                network = new WildernessRoadNetwork(level, regionSize, spurLength);
                NETWORKS.put(level, network);
            }
            return network;
        }
    }

    public synchronized List<List<BlockPos>> routesForChunk(ChunkPos chunk) {
        List<List<BlockPos>> result = new ArrayList<>();
        int maximumSpoke = spurLength * 7 / 6 + 64;
        int radius = Math.max(1, Math.floorDiv(maximumSpoke + regionSize - 1, regionSize));
        for (RoadCell cell : cellsAround(chunk.getMiddleBlockPosition(0), radius)) {
            for (RoadPath path : cell.paths()) {
                if (intersects(path.points(), chunk, 1)) result.add(path.points());
            }
            for (RoadsideSite site : cell.sites()) {
                if (intersects(site.connector(), chunk, 1)) result.add(site.connector());
            }
        }
        return List.copyOf(result);
    }

    public synchronized List<RoadsideSite> sitesForChunk(ChunkPos chunk) {
        List<RoadsideSite> result = new ArrayList<>();
        int maximumSpoke = spurLength * 7 / 6 + 96;
        int searchRadius = Math.max(1, Math.floorDiv(maximumSpoke + regionSize - 1, regionSize));
        for (RoadCell cell : cellsAround(chunk.getMiddleBlockPosition(0), searchRadius)) {
            for (RoadsideSite site : cell.sites()) {
                int radius = site.type() == SiteType.HAMLET ? 8 : 5;
                if (site.center().getX() + radius >= chunk.getMinBlockX()
                        && site.center().getX() - radius <= chunk.getMaxBlockX()
                        && site.center().getZ() + radius >= chunk.getMinBlockZ()
                        && site.center().getZ() - radius <= chunk.getMaxBlockZ()) {
                    result.add(site);
                }
            }
        }
        return List.copyOf(result);
    }

    public synchronized List<RoadsideSite> sitesNear(BlockPos position, int radius) {
        List<RoadsideSite> result = new ArrayList<>();
        addNearbySites(result, sites.values(), position, radius);
        if (result.isEmpty()) {
            for (RoadCell cell : cellsAround(position, 1)) {
                addNearbySites(result, cell.sites(), position, radius);
            }
        }
        return result.stream().sorted(Comparator.comparingLong(site -> distance(site.center(), position)))
                .distinct().toList();
    }

    public synchronized Optional<RoadsideSite> nearestSite(BlockPos position, int radius) {
        return siteCandidates(position, radius, 1).stream().findFirst();
    }

    public synchronized List<RoadsideSite> siteCandidates(BlockPos position, int radius, int limit) {
        List<RoadsideSite> result = new ArrayList<>();
        addNearbySites(result, sites.values(), position, radius);
        int centerX = Math.floorDiv(position.getX(), regionSize);
        int centerZ = Math.floorDiv(position.getZ(), regionSize);
        int maximumRing = Math.max(1, Math.floorDiv(radius + regionSize - 1, regionSize));
        for (int ring = 0; ring <= maximumRing && result.size() < limit; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (ring > 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    addNearbySites(result, cell(centerX + dx, centerZ + dz).sites(), position, radius);
                }
            }
        }
        return result.stream().distinct()
                .sorted(Comparator.comparingLong(site -> distance(site.center(), position)))
                .limit(limit).toList();
    }

    public synchronized Optional<BlockPos> nearestRoadPoint(BlockPos position, int radius) {
        BlockPos nearest = null;
        long best = (long) radius * radius + 1;
        List<RoadCell> candidates = new ArrayList<>(cells.values());
        if (candidates.isEmpty()) candidates.add(cell(Math.floorDiv(position.getX(), regionSize),
                Math.floorDiv(position.getZ(), regionSize)));
        for (RoadCell cell : candidates) {
            for (RoadPath path : cell.paths()) {
                for (int index = 0; index < path.points().size(); index += 8) {
                    BlockPos point = path.points().get(index);
                    long distance = distance(point, position);
                    if (distance < best) {
                        best = distance;
                        nearest = point;
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    public synchronized List<BlockPos> roadWaypointsNear(BlockPos position, int radius) {
        List<BlockPos> result = new ArrayList<>();
        long maximum = (long) radius * radius;
        List<RoadCell> candidates = new ArrayList<>(cells.values());
        if (candidates.isEmpty()) candidates.add(cell(Math.floorDiv(position.getX(), regionSize),
                Math.floorDiv(position.getZ(), regionSize)));
        for (RoadCell cell : candidates) {
            for (RoadPath path : cell.paths()) {
                for (int index = 0; index < path.points().size(); index += 24) {
                    BlockPos point = path.points().get(index);
                    long distance = distance(point, position);
                    if (distance >= 8 * 8 && distance <= maximum) result.add(point);
                }
            }
        }
        return result.stream().distinct().sorted(Comparator.comparingLong(point -> distance(point, position)))
                .toList();
    }

    public synchronized Optional<RoadsideSite> site(long id) {
        return Optional.ofNullable(sites.get(id));
    }

    public synchronized List<BlockPos> routeBetweenSites(long fromId, long toId) {
        RoadsideSite from = sites.get(fromId);
        RoadsideSite to = sites.get(toId);
        if (from == null || to == null || from.partnerId() != to.id() || to.partnerId() != from.id()
                || from.cellX() != to.cellX() || from.cellZ() != to.cellZ()) return List.of();
        RoadCell cell = cell(from.cellX(), from.cellZ());
        RoadPath first = path(cell, from.pathId());
        RoadPath second = path(cell, to.pathId());
        if (first == null || second == null) return List.of();
        List<BlockPos> route = new ArrayList<>(from.connector());
        appendReverse(route, first.points().subList(0, from.pathIndex() + 1));
        append(route, second.points().subList(1, to.pathIndex() + 1));
        appendReverse(route, to.connector());
        return List.copyOf(route);
    }

    public synchronized List<BlockPos> routeToSite(BlockPos from, RoadsideSite destination) {
        long seed = mix(destination.id(), from.getX(), from.getZ(), 1401);
        List<BlockPos> raw = trace(from, destination.center(), seed);
        return gradeRoute(raw, false, null, null);
    }

    public synchronized List<BlockPos> fallbackRouteToSite(BlockPos from, RoadsideSite destination) {
        long seed = mix(destination.id(), from.getX(), from.getZ(), 1401);
        List<BlockPos> raw = trace(from, destination.center(), seed);
        List<BlockPos> result = new ArrayList<>();
        for (int index = 0; index < raw.size(); index += 2) {
            BlockPos point = raw.get(index);
            result.add(new BlockPos(point.getX(), sample(point.getX(), point.getZ()).height(), point.getZ()));
        }
        BlockPos last = raw.get(raw.size() - 1);
        if (result.isEmpty() || result.get(result.size() - 1).getX() != last.getX()
                || result.get(result.size() - 1).getZ() != last.getZ()) {
            result.add(new BlockPos(last.getX(), sample(last.getX(), last.getZ()).height(), last.getZ()));
        }
        return List.copyOf(result);
    }

    synchronized RoadCell cell(int cellX, int cellZ) {
        return cells.computeIfAbsent(ChunkPos.asLong(cellX, cellZ), ignored -> buildCell(cellX, cellZ));
    }

    private RoadCell buildCell(int cellX, int cellZ) {
        BlockPos hub = hub(cellX, cellZ);
        if (hub == null) return new RoadCell(cellX, cellZ, BlockPos.ZERO, List.of(), List.of());
        List<RoadPath> paths = new ArrayList<>();
        RandomSource random = RandomSource.create(mix(state.getLevelSeed(), cellX, cellZ, 91));
        int firstDirection = random.nextInt(DIRECTIONS.length);
        for (int spoke = 0; spoke < 4; spoke++) {
            int[] direction = DIRECTIONS[(firstDirection + spoke * 2 + random.nextInt(2)) % DIRECTIONS.length];
            int length = Math.max(112, spurLength * 2 / 3 + random.nextInt(Math.max(1, spurLength / 2)));
            int lateral = random.nextInt(65) - 32;
            int endX = hub.getX() + direction[0] * length + direction[1] * lateral;
            int endZ = hub.getZ() + direction[1] * length - direction[0] * lateral;
            addPath(paths, cellX, cellZ, spoke, hub, new BlockPos(endX, 0, endZ));
        }

        List<RoadsideSite> cellSites = buildSites(cellX, cellZ, paths);
        cellSites.forEach(site -> sites.put(site.id(), site));
        return new RoadCell(cellX, cellZ, hub, List.copyOf(paths), cellSites);
    }

    private void addPath(List<RoadPath> paths, int cellX, int cellZ, int kind, BlockPos from, BlockPos to) {
        long id = mix(state.getLevelSeed(), cellX, cellZ, 1000 + kind);
        List<BlockPos> points = gradeRoute(trace(from, to, id), true, from.getY(), null);
        if (points.size() >= MIN_ROAD_LENGTH) paths.add(new RoadPath(id, kind, points));
    }

    private List<RoadsideSite> buildSites(int cellX, int cellZ, List<RoadPath> paths) {
        if (!FrontierConfig.ROADSIDE_SITES.get() || paths.size() < 2) return List.of();
        List<RoadPath> longest = paths.stream()
                .sorted(Comparator.comparingInt((RoadPath path) -> path.points().size()).reversed())
                .limit(2).toList();
        RoadsideSite camp = siteAlong(cellX, cellZ, longest.get(0), SiteType.CAMP, 0);
        RoadsideSite hamlet = siteAlong(cellX, cellZ, longest.get(1), SiteType.HAMLET, 1);
        if (camp == null || hamlet == null) return List.of();
        camp = camp.withPartner(hamlet.id());
        hamlet = hamlet.withPartner(camp.id());
        return List.of(camp, hamlet);
    }

    private RoadsideSite siteAlong(int cellX, int cellZ, RoadPath path, SiteType type, int salt) {
        int index = Math.max(32, Math.min(path.points().size() - 24,
                path.points().size() / 2 + (int) Math.floorMod(path.id(), 33) - 16));
        if (index <= 0 || index >= path.points().size()) return null;
        BlockPos access = path.points().get(index);
        BlockPos before = path.points().get(Math.max(0, index - 4));
        BlockPos after = path.points().get(Math.min(path.points().size() - 1, index + 4));
        int tangentX = Integer.signum(after.getX() - before.getX());
        int tangentZ = Integer.signum(after.getZ() - before.getZ());
        int normalX = -tangentZ;
        int normalZ = tangentX;
        if (normalX == 0 && normalZ == 0) normalX = 1;
        int radius = type == SiteType.HAMLET ? 8 : 5;
        for (int offset : new int[]{11, -11, 15, -15, 19, -19}) {
            int x = access.getX() + normalX * offset;
            int z = access.getZ() + normalZ * offset;
            BlockPos center = flatSite(x, z, radius);
            if (center == null) continue;
            List<BlockPos> connector = gradeRoute(
                    trace(center, access, mix(path.id(), cellX, cellZ, salt + 500)),
                    false, center.getY(), access.getY());
            if (connector.size() < 4) continue;
            long id = mix(path.id(), center.getX(), center.getZ(), type.ordinal() + 700);
            return new RoadsideSite(id, cellX, cellZ, path.id(), index, center,
                    access, type, 0, connector);
        }
        return null;
    }

    private BlockPos flatSite(int x, int z, int radius) {
        TerrainSample center = sample(x, z);
        if (center.ocean() || center.waterDepth() > 0) return null;
        for (int[] offset : new int[][]{{-radius, -radius}, {-radius, radius},
                {radius, -radius}, {radius, radius}, {0, -radius}, {0, radius}, {-radius, 0}, {radius, 0}}) {
            TerrainSample sample = sample(x + offset[0], z + offset[1]);
            if (sample.ocean() || sample.waterDepth() > 0
                    || Math.abs(sample.height() - center.height()) > 3) return null;
        }
        return new BlockPos(x, center.height(), z);
    }

    private BlockPos hub(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(mix(state.getLevelSeed(), cellX, cellZ, 17));
        int baseX = cellX * regionSize + regionSize / 2;
        int baseZ = cellZ * regionSize + regionSize / 2;
        int jitter = Math.max(24, regionSize / 4);
        int x = baseX + random.nextInt(jitter * 2 + 1) - jitter;
        int z = baseZ + random.nextInt(jitter * 2 + 1) - jitter;
        return new BlockPos(x, sample(x, z).height(), z);
    }

    private List<BlockPos> trace(BlockPos from, BlockPos to, long seed) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int normalX = -Integer.signum(dz);
        int normalZ = Integer.signum(dx);
        RandomSource random = RandomSource.create(seed);
        int offset = random.nextInt(97) - 48;
        int middleX = (from.getX() + to.getX()) / 2 + normalX * offset;
        int middleZ = (from.getZ() + to.getZ()) / 2 + normalZ * offset;
        List<long[]> columns = new ArrayList<>();
        columns.add(new long[]{from.getX(), from.getZ()});
        appendLine(columns, from.getX(), from.getZ(), middleX, middleZ);
        appendLine(columns, middleX, middleZ, to.getX(), to.getZ());
        List<BlockPos> result = new ArrayList<>();
        for (long[] column : columns) result.add(new BlockPos((int) column[0], 0, (int) column[1]));
        return List.copyOf(result);
    }

    private static void appendLine(List<long[]> result, int fromX, int fromZ, int toX, int toZ) {
        int x = fromX;
        int z = fromZ;
        int dx = Math.abs(toX - fromX);
        int dz = Math.abs(toZ - fromZ);
        int movedX = 0;
        int movedZ = 0;
        int stepX = Integer.signum(toX - fromX);
        int stepZ = Integer.signum(toZ - fromZ);
        while (x != toX || z != toZ) {
            boolean moveX = movedX < dx && (movedZ >= dz
                    || (movedX + 1.0) / Math.max(1, dx) <= (movedZ + 1.0) / Math.max(1, dz));
            if (moveX) {
                x += stepX;
                movedX++;
            } else {
                z += stepZ;
                movedZ++;
            }
            long[] last = result.get(result.size() - 1);
            if (last[0] != x || last[1] != z) result.add(new long[]{x, z});
        }
    }

    private TerrainSample sample(int x, int z) {
        return terrain.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> {
            int height = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                    heights, state.randomState()) - 1;
            int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                    heights, state.randomState()) - 1;
            var biome = generator.getBiomeSource().getNoiseBiome(
                    x >> 2, height >> 2, z >> 2, state.randomState().sampler());
            return new TerrainSample(height, Math.max(0, height - floor), biome.is(BiomeTags.IS_OCEAN));
        });
    }

    private List<BlockPos> gradeRoute(List<BlockPos> raw, boolean truncate,
            Integer fixedStart, Integer fixedEnd) {
        if (raw.isEmpty()) return List.of();
        int usable = raw.size();
        int[] terrainHeights = new int[raw.size()];
        int previousAnchor = 0;
        TerrainSample previousSample = sample(raw.get(0).getX(), raw.get(0).getZ());
        if (previousSample.ocean() || previousSample.waterDepth() > MAX_WATER_DEPTH) return List.of();
        terrainHeights[0] = previousSample.height();
        int waterRun = previousSample.waterDepth() > 0 ? 1 : 0;
        for (int anchor = Math.min(TERRAIN_STEP, raw.size() - 1);
                previousAnchor < raw.size() - 1;
                anchor = Math.min(raw.size() - 1, anchor + TERRAIN_STEP)) {
            BlockPos point = raw.get(anchor);
            TerrainSample current = sample(point.getX(), point.getZ());
            int span = anchor - previousAnchor;
            waterRun = current.waterDepth() > 0 ? waterRun + span : 0;
            if (current.ocean() || current.waterDepth() > MAX_WATER_DEPTH || waterRun > MAX_WATER_RUN) {
                usable = truncate ? previousAnchor + 1 : 0;
                break;
            }
            for (int index = previousAnchor + 1; index <= anchor; index++) {
                double progress = (double) (index - previousAnchor) / span;
                terrainHeights[index] = (int) Math.round(previousSample.height()
                        + (current.height() - previousSample.height()) * progress);
            }
            previousAnchor = anchor;
            previousSample = current;
            if (anchor == raw.size() - 1) break;
        }
        if (usable < (truncate ? MIN_ROAD_LENGTH : raw.size())) return List.of();
        if (usable < raw.size()) {
            return gradeRoute(raw.subList(0, usable), false, fixedStart, null);
        }
        int[] heights = terrainHeights.clone();
        if (fixedStart != null) heights[0] = fixedStart;
        if (fixedEnd != null) heights[heights.length - 1] = fixedEnd;
        for (int index = 1; index < heights.length; index++) {
            heights[index] = Math.max(heights[index], heights[index - 1] - 1);
        }
        for (int index = heights.length - 2; index >= 0; index--) {
            heights[index] = Math.max(heights[index], heights[index + 1] - 1);
        }
        if (fixedStart != null && heights[0] != fixedStart
                || fixedEnd != null && heights[heights.length - 1] != fixedEnd) return List.of();
        for (int index = 0; index < heights.length; index++) {
            if (Math.abs(heights[index] - terrainHeights[index]) > MAX_EARTHWORK) {
                if (truncate && index >= MIN_ROAD_LENGTH) {
                    return gradeRoute(raw.subList(0, index), false, fixedStart, null);
                }
                return List.of();
            }
        }
        List<BlockPos> result = new ArrayList<>();
        for (int index = 0; index < raw.size(); index++) {
            BlockPos point = raw.get(index);
            result.add(new BlockPos(point.getX(), heights[index], point.getZ()));
        }
        return List.copyOf(result);
    }

    private List<RoadCell> cellsAround(BlockPos position, int radius) {
        int cellX = Math.floorDiv(position.getX(), regionSize);
        int cellZ = Math.floorDiv(position.getZ(), regionSize);
        List<RoadCell> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                result.add(cell(cellX + dx, cellZ + dz));
            }
        }
        return result;
    }

    private static void addNearbySites(List<RoadsideSite> destination, Iterable<RoadsideSite> candidates,
            BlockPos position, int radius) {
        for (RoadsideSite site : candidates) {
            if (site.center().closerThan(position, radius)) destination.add(site);
        }
    }

    private static boolean intersects(List<BlockPos> route, ChunkPos chunk, int margin) {
        return route.stream().anyMatch(pos -> pos.getX() >= chunk.getMinBlockX() - margin
                && pos.getX() <= chunk.getMaxBlockX() + margin
                && pos.getZ() >= chunk.getMinBlockZ() - margin
                && pos.getZ() <= chunk.getMaxBlockZ() + margin);
    }

    private static RoadPath path(RoadCell cell, long id) {
        return cell.paths().stream().filter(path -> path.id() == id).findFirst().orElse(null);
    }

    private static void append(List<BlockPos> destination, List<BlockPos> source) {
        for (BlockPos point : source) {
            if (destination.isEmpty() || !destination.get(destination.size() - 1).equals(point)) destination.add(point);
        }
    }

    private static void appendReverse(List<BlockPos> destination, List<BlockPos> source) {
        List<BlockPos> reverse = new ArrayList<>(source);
        Collections.reverse(reverse);
        append(destination, reverse);
    }

    private static long distance(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static long mix(long seed, int x, int z, int salt) {
        long value = seed ^ (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xC2B2AE3D27D4EB4FL
                ^ (long) salt * 0x165667B19E3779F9L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static <K, V> Map<K, V> cache(int capacity) {
        return new LinkedHashMap<>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
                return size() > capacity;
            }
        };
    }

    public int regionSize() {
        return regionSize;
    }

    public int spurLength() {
        return spurLength;
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        synchronized (NETWORKS) {
            NETWORKS.clear();
        }
    }

    public enum SiteType {
        CAMP, HAMLET
    }

    public record RoadCell(int cellX, int cellZ, BlockPos hub, List<RoadPath> paths,
            List<RoadsideSite> sites) {
    }

    public record RoadPath(long id, int kind, List<BlockPos> points) {
    }

    public record RoadsideSite(long id, int cellX, int cellZ, long pathId, int pathIndex,
            BlockPos center, BlockPos access, SiteType type, long partnerId, List<BlockPos> connector) {
        private RoadsideSite withPartner(long partnerId) {
            return new RoadsideSite(id, cellX, cellZ, pathId, pathIndex, center, access,
                    type, partnerId, connector);
        }
    }

    private record TerrainSample(int height, int waterDepth, boolean ocean) {
    }
}
