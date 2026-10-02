package dev.livingfrontier.village;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.LivingFrontier;
import dev.livingfrontier.entity.VillageResidentEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingFrontier.MOD_ID)
public final class VillageLife {
    private VillageLife() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !FrontierConfig.VILLAGE_LIFE_ENABLED.get()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 200 != 0) {
            return;
        }
        VillageLifeState state = VillageLifeState.get(level);
        for (var player : level.players()) {
            if (player.isSpectator() || player instanceof net.minecraftforge.common.util.FakePlayer) {
                continue;
            }
            for (Villager villager : level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64),
                    Villager::isAlive)) {
                villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT)
                        .filter(meeting -> meeting.dimension().equals(Level.OVERWORLD))
                        .map(meeting -> meeting.pos())
                        .filter(meeting -> !state.hasVillage(meeting))
                        .ifPresent(meeting -> improve(level, meeting));
            }
        }
    }

    public static boolean improve(ServerLevel level, BlockPos bell) {
        if (!FrontierConfig.VILLAGE_LIFE_ENABLED.get() || !level.dimension().equals(Level.OVERWORLD)
                || !level.hasChunkAt(bell) || !level.getBlockState(bell).is(Blocks.BELL)) {
            return false;
        }
        VillageLifeState state = VillageLifeState.get(level);
        if (state.hasVillage(bell) || level.getEntitiesOfClass(Villager.class, new AABB(bell).inflate(48),
                villager -> villager.isAlive() && !villager.isBaby()).size() < 2) {
            return false;
        }
        Optional<BlockPos> market = FrontierConfig.VILLAGE_MARKETS.get() ? findMarket(level, bell) : Optional.empty();
        List<BlockPos> spawnPositions = residentPositions(level, bell);
        if (spawnPositions.isEmpty()) {
            return false;
        }
        // Save the one-time decision, not a missing-mob check that would resurrect killed residents.
        state.recordVillage(bell);
        long tradeVillage = TradeNetworkState.get(level).registerVillage(bell);
        int targetGuards = FrontierConfig.VILLAGE_GUARDS.get();
        int guards = 0;
        int position = 0;
        while (guards < targetGuards && position < spawnPositions.size()) {
            if (addResident(level, FrontierEntities.VILLAGE_GUARD.get(), spawnPositions.get(position++), bell,
                    guards % 2 == 1)) {
                guards++;
            }
        }
        boolean traveller = false;
        while (!traveller && position < spawnPositions.size()) {
            traveller = addResident(level, FrontierEntities.TRAVELLER.get(), spawnPositions.get(position++), bell, false);
        }
        boolean traderRequired = FrontierConfig.TRADE_NETWORK.get() && FrontierConfig.WILDERNESS_ROADS.get()
                && FrontierConfig.ROADSIDE_SITES.get();
        boolean trader = !traderRequired;
        while (!trader && position < spawnPositions.size()) {
            trader = TradeNetworkManager.spawnVillageTrader(level, bell, tradeVillage,
                    spawnPositions.get(position++));
        }
        if (traderRequired && !trader) state.queueTrader(bell);
        if (guards < targetGuards || !traveller || !trader) {
            LivingFrontier.LOGGER.warn("Village at {} had room for {}/{} guards, {} traveller and {} trader",
                    bell, guards, targetGuards, traveller ? 1 : 0, trader ? 1 : 0);
        }

        market.ifPresent(marketPosition -> buildMarket(level, marketPosition));
        return true;
    }

    private static <T extends VillageResidentEntity> boolean addResident(ServerLevel level, EntityType<T> type,
            BlockPos position, BlockPos home, boolean archer) {
        T resident = type.create(level);
        if (resident == null) {
            throw new IllegalStateException("Cannot create registered village resident " + type);
        }
        resident.moveTo(position, 0, 0);
        if (!level.noCollision(resident) || !resident.checkSpawnObstruction(level)) {
            LivingFrontier.LOGGER.warn("Village resident placement obstructed at {}", position);
            return false;
        }
        if (resident instanceof dev.livingfrontier.entity.ArmedGuard guard) {
            guard.setArcher(archer);
        }
        ForgeEventFactory.onFinalizeSpawn(resident, level, level.getCurrentDifficultyAt(position),
                MobSpawnType.STRUCTURE, null, null);
        if (resident.isSpawnCancelled()) {
            return false;
        }
        resident.setVillageHome(home);
        if (!level.addFreshEntity(resident)) {
            LivingFrontier.LOGGER.warn("Could not add village resident at {}", position);
            return false;
        }
        return true;
    }

    private static List<BlockPos> residentPositions(ServerLevel level, BlockPos bell) {
        List<BlockPos> positions = new ArrayList<>();
        for (int radius = 4; radius <= 16; radius += 3) {
            for (int side = 0; side < 8; side++) {
                double angle = side * Math.PI / 4;
                int x = bell.getX() + (int) (Math.cos(angle) * radius);
                int z = bell.getZ() + (int) (Math.sin(angle) * radius);
                if (!level.hasChunkAt(new BlockPos(x, bell.getY(), z))) {
                    continue;
                }
                BlockPos position = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (level.getWorldBorder().isWithinBounds(position) && Math.abs(position.getY() - bell.getY()) <= 5
                        && level.getBlockState(position).isAir()
                        && level.getBlockState(position.above()).isAir()
                        && residentGround(level.getBlockState(position.below()))
                        && level.getFluidState(position).isEmpty()) {
                    positions.add(position);
                }
            }
        }
        return positions;
    }

    private static Optional<BlockPos> findMarket(ServerLevel level, BlockPos bell) {
        for (int radius = 8; radius <= 24; radius += 4) {
            for (int side = 0; side < 8; side++) {
                double angle = side * Math.PI / 4;
                int x = bell.getX() + (int) (Math.cos(angle) * radius);
                int z = bell.getZ() + (int) (Math.sin(angle) * radius);
                if (!level.hasChunkAt(new BlockPos(x, bell.getY(), z))) {
                    continue;
                }
                BlockPos position = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (Math.abs(position.getY() - bell.getY()) <= 5 && canBuildMarket(level, position)) {
                    return Optional.of(position);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean canBuildMarket(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos foot = origin.offset(x, 0, z);
                if (!level.hasChunkAt(foot) || !level.getWorldBorder().isWithinBounds(foot)) {
                    return false;
                }
                BlockState ground = level.getBlockState(foot.below());
                if (!naturalGround(ground)) {
                    return false;
                }
                for (int y = 0; y <= 3; y++) {
                    if (!level.getBlockState(foot.above(y)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                new AABB(origin.offset(-2, 0, -2)).expandTowards(5, 4, 5)).isEmpty();
    }

    public static boolean buildMarket(ServerLevel level, BlockPos origin) {
        if (!canBuildMarket(level, origin)) {
            return false;
        }
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-2, 2}) {
                for (int y = 0; y <= 2; y++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, 3, z), (x % 2 == 0 ? Blocks.WHITE_WOOL : Blocks.GREEN_WOOL).defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.offset(-1, 0, -1), Blocks.BARREL.defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 0, -1), Blocks.COMPOSTER.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, -1), Blocks.FLETCHING_TABLE.defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 2, -1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 3);
        return true;
    }

    private static boolean naturalGround(BlockState ground) {
        return ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(Blocks.DIRT_PATH)
                || ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.PODZOL) || ground.is(Blocks.SAND);
    }

    private static boolean residentGround(BlockState ground) {
        return naturalGround(ground) || ground.is(Blocks.COBBLESTONE) || ground.is(Blocks.STONE_BRICKS)
                || ground.is(Blocks.SMOOTH_SANDSTONE) || ground.is(Blocks.GRAVEL) || ground.is(Blocks.SNOW_BLOCK);
    }
}
