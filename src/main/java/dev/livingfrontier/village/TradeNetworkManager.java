package dev.livingfrontier.village;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.LivingFrontier;
import dev.livingfrontier.entity.TraderEntity;
import dev.livingfrontier.entity.TravellerEntity;
import dev.livingfrontier.road.WildernessRoadNetwork;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingFrontier.MOD_ID)
public final class TradeNetworkManager {
    private TradeNetworkManager() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !FrontierConfig.TRADE_NETWORK.get()
                || !FrontierConfig.WILDERNESS_ROADS.get() || !FrontierConfig.ROADSIDE_SITES.get()) return;
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 100 != 0) return;
        WildernessRoadNetwork network = WildernessRoadNetwork.forLevel(level);
        TradeNetworkState state = TradeNetworkState.get(level);
        VillageLifeState villageLife = VillageLifeState.get(level);
        for (var player : level.players()) {
            if (player.isSpectator() || player instanceof net.minecraftforge.common.util.FakePlayer) continue;
            for (var site : network.sitesNear(player.blockPosition(), 160)) {
                state.registerSite(site);
                network.site(site.partnerId()).ifPresent(state::registerSite);
                if (!state.isPopulated(site.id()) && markerExists(level, site)) {
                    initializeSite(level, network, state, site);
                }
            }
            for (TraderEntity trader : level.getEntitiesOfClass(TraderEntity.class,
                    player.getBoundingBox().inflate(128), trader -> trader.isAlive() && !trader.hasJourney())) {
                assignUnroutedTrader(level, network, state, trader);
            }
            for (BlockPos village : villageLife.pendingTradersNear(player.blockPosition(), 96)) {
                long villageId = TradeNetworkState.settlementId(village, 0x56494C4C);
                if (spawnVillageTrader(level, village, villageId)) villageLife.completeTrader(village);
            }
        }
    }

    public static boolean configureVillageTrader(ServerLevel level, TraderEntity trader,
            BlockPos village, long villageId) {
        if (!FrontierConfig.TRADE_NETWORK.get() || !FrontierConfig.WILDERNESS_ROADS.get()
                || !FrontierConfig.ROADSIDE_SITES.get()) return false;
        WildernessRoadNetwork network = WildernessRoadNetwork.forLevel(level);
        TradeNetworkState state = TradeNetworkState.get(level);
        state.register(villageId, village, TradeNetworkState.SettlementKind.VILLAGE, 0);
        List<WildernessRoadNetwork.RoadsideSite> candidates = network.siteCandidates(village, 768, 8);
        for (var site : candidates) {
            List<BlockPos> route = network.routeToSite(village, site);
            if (route.isEmpty()) continue;
            configureJourney(level, state, trader, villageId, site, route);
            return true;
        }
        if (!candidates.isEmpty()) {
            var site = candidates.get(0);
            List<BlockPos> route = network.fallbackRouteToSite(village, site);
            if (!route.isEmpty()) {
                configureJourney(level, state, trader, villageId, site, route);
                return true;
            }
        }
        return false;
    }

    private static void configureJourney(ServerLevel level, TradeNetworkState state, TraderEntity trader,
            long villageId, WildernessRoadNetwork.RoadsideSite site, List<BlockPos> route) {
        state.registerSite(site);
        state.setPartner(villageId, site.id());
        state.arriveAndReload(villageId, trader.getInventory(), level.getGameTime() / 24000L);
        trader.setJourney(villageId, site.id(), route);
    }

    public static boolean spawnVillageTrader(ServerLevel level, BlockPos village, long villageId) {
        return spawnVillageTrader(level, village, villageId, null);
    }

    public static boolean spawnVillageTrader(ServerLevel level, BlockPos village, long villageId,
            BlockPos preferredPosition) {
        TraderEntity trader = FrontierEntities.TRADER.get().create(level);
        if (trader == null) throw new IllegalStateException("Cannot create registered road trader");
        BlockPos position = preferredPosition == null ? spawnPosition(level, village, 4) : preferredPosition;
        trader.moveTo(position, 0, 0);
        if (!validSpawn(level, trader, position)) return false;
        ForgeEventFactory.onFinalizeSpawn(trader, level, level.getCurrentDifficultyAt(position),
                MobSpawnType.STRUCTURE, null, null);
        if (trader.isSpawnCancelled() || !configureVillageTrader(level, trader, village, villageId)) return false;
        trader.setPersistenceRequired();
        return level.addFreshEntity(trader);
    }

    public static void initializeSite(ServerLevel level, WildernessRoadNetwork network,
            TradeNetworkState state, WildernessRoadNetwork.RoadsideSite site) {
        state.registerSite(site);
        if (state.isPopulated(site.id())) return;
        state.markPopulated(site.id());
        int travellers = site.type() == WildernessRoadNetwork.SiteType.HAMLET ? 2 : 1;
        for (int index = 0; index < travellers; index++) {
            spawnTraveller(level, site, index);
        }
        if (site.type() == WildernessRoadNetwork.SiteType.HAMLET) {
            network.site(site.partnerId()).ifPresent(partner -> {
                state.registerSite(partner);
                List<BlockPos> route = network.routeBetweenSites(site.id(), partner.id());
                if (!route.isEmpty()) spawnTrader(level, state, site, partner, route);
            });
        }
    }

    private static void assignUnroutedTrader(ServerLevel level, WildernessRoadNetwork network,
            TradeNetworkState state, TraderEntity trader) {
        var origin = network.nearestSite(trader.blockPosition(), 64);
        if (origin.isEmpty()) return;
        var site = origin.get();
        var partner = network.site(site.partnerId());
        if (partner.isEmpty()) return;
        state.registerSite(site);
        state.registerSite(partner.get());
        List<BlockPos> route = network.routeBetweenSites(site.id(), partner.get().id());
        if (route.isEmpty()) return;
        state.arriveAndReload(site.id(), trader.getInventory(), level.getGameTime() / 24000L);
        trader.setJourney(site.id(), partner.get().id(), route);
    }

    private static void spawnTraveller(ServerLevel level, WildernessRoadNetwork.RoadsideSite site, int index) {
        TravellerEntity traveller = FrontierEntities.TRAVELLER.get().create(level);
        if (traveller == null) return;
        BlockPos position = spawnPosition(level, site.center(), index * 2 + 2);
        traveller.moveTo(position, index * 90, 0);
        if (!validSpawn(level, traveller, position)) return;
        ForgeEventFactory.onFinalizeSpawn(traveller, level, level.getCurrentDifficultyAt(position),
                MobSpawnType.STRUCTURE, null, null);
        if (traveller.isSpawnCancelled()) return;
        traveller.setPersistenceRequired();
        level.addFreshEntity(traveller);
    }

    private static void spawnTrader(ServerLevel level, TradeNetworkState state,
            WildernessRoadNetwork.RoadsideSite origin, WildernessRoadNetwork.RoadsideSite destination,
            List<BlockPos> route) {
        TraderEntity trader = FrontierEntities.TRADER.get().create(level);
        if (trader == null) return;
        BlockPos position = spawnPosition(level, origin.center(), 4);
        trader.moveTo(position, 0, 0);
        if (!validSpawn(level, trader, position)) return;
        ForgeEventFactory.onFinalizeSpawn(trader, level, level.getCurrentDifficultyAt(position),
                MobSpawnType.STRUCTURE, null, null);
        if (trader.isSpawnCancelled()) return;
        state.arriveAndReload(origin.id(), trader.getInventory(), level.getGameTime() / 24000L);
        trader.setJourney(origin.id(), destination.id(), route);
        level.addFreshEntity(trader);
    }

    private static boolean validSpawn(ServerLevel level, Mob mob, BlockPos position) {
        if (!level.noCollision(mob) || !mob.checkSpawnObstruction(level)) {
            LivingFrontier.LOGGER.warn("Roadside resident placement obstructed at {}", position);
            return false;
        }
        return true;
    }

    private static BlockPos spawnPosition(ServerLevel level, BlockPos center, int offset) {
        for (int radius = Math.max(2, offset); radius <= 10; radius += 2) {
            for (int side = 0; side < 8; side++) {
                double angle = side * Math.PI / 4;
                int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
                BlockPos local = new BlockPos(x, center.getY() + 1, z);
                if (level.hasChunkAt(local) && level.getBlockState(local).isAir()
                        && level.getBlockState(local.above()).isAir()
                        && !level.getBlockState(local.below()).isAir()
                        && level.getFluidState(local).isEmpty()) return local;
                BlockPos position = new BlockPos(x,
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (level.hasChunkAt(position) && level.getBlockState(position).isAir()
                        && level.getBlockState(position.above()).isAir()
                        && level.getFluidState(position).isEmpty()) return position;
            }
        }
        return center.above();
    }

    private static boolean markerExists(ServerLevel level, WildernessRoadNetwork.RoadsideSite site) {
        if (!level.hasChunkAt(site.center())) return false;
        return site.type() == WildernessRoadNetwork.SiteType.HAMLET
                ? level.getBlockState(site.center().above()).is(Blocks.BELL)
                : level.getBlockState(site.center().above()).is(Blocks.CAMPFIRE);
    }
}
