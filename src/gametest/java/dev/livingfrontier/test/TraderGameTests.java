package dev.livingfrontier.test;

import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.entity.TraderEntity;
import dev.livingfrontier.entity.RoadTravelGoal;
import dev.livingfrontier.village.TradeGood;
import dev.livingfrontier.village.TradeNetworkManager;
import dev.livingfrontier.village.TradeNetworkState;
import dev.livingfrontier.entity.TravellerEntity;
import dev.livingfrontier.road.WildernessRoadNetwork;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class TraderGameTests {
    private TraderGameTests() {
    }

    @GameTest(template = "test/empty")
    public static void traderCargoAndRouteSurviveSave(GameTestHelper helper) {
        TraderEntity trader = FrontierEntities.TRADER.get().create(helper.getLevel());
        helper.assertTrue(trader != null, "Road trader entity is registered");
        TradeNetworkState state = TradeNetworkState.get(helper.getLevel());
        long origin = state.registerVillage(new BlockPos(20, 64, 20));
        long destination = state.registerVillage(new BlockPos(60, 64, 20));
        trader.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 6));
        trader.setJourney(origin, destination,
                List.of(new BlockPos(20, 64, 20), new BlockPos(40, 64, 20), new BlockPos(60, 64, 20)));
        CompoundTag saved = new CompoundTag();
        trader.save(saved);
        var restored = EntityType.create(saved, helper.getLevel()).orElseThrow();
        helper.assertTrue(restored instanceof TraderEntity loaded && loaded.hasJourney()
                        && loaded.destinationId() == destination
                        && loaded.getInventory().countItem(Items.IRON_INGOT) == 6,
                "Trader cargo and road journey survive entity save/load");
        helper.succeed();
    }

    @GameTest(template = "test/empty")
    public static void traderOffersComeFromPhysicalCargo(GameTestHelper helper) {
        TraderEntity trader = FrontierEntities.TRADER.get().create(helper.getLevel());
        helper.assertTrue(trader != null, "Road trader entity is registered");
        TradeNetworkState state = TradeNetworkState.get(helper.getLevel());
        long origin = state.registerVillage(new BlockPos(100, 64, 100));
        long destination = state.registerVillage(new BlockPos(140, 64, 100));
        state.setStock(destination, TradeGood.BREAD, 0);
        trader.getInventory().setItem(0, new ItemStack(Items.BREAD, 12));
        trader.setJourney(origin, destination,
                List.of(new BlockPos(100, 64, 100), new BlockPos(140, 64, 100)));
        var offers = trader.getOffers();
        helper.assertTrue(offers.stream().anyMatch(offer -> offer.getResult().is(Items.BREAD))
                        && offers.stream().anyMatch(offer -> offer.getResult().is(Items.EMERALD)),
                "Trader exposes cargo sales and destination purchase orders through vanilla offers");
        helper.succeed();
    }

    @GameTest(template = "test/empty", timeoutTicks = 140)
    public static void traderWalksItsPersistedRoadWaypoints(GameTestHelper helper) {
        for (int x = 1; x <= 13; x++) {
            for (int z = 1; z <= 4; z++) helper.setBlock(x, 1, z, Blocks.GRASS_BLOCK);
        }
        TraderEntity trader = helper.spawn(FrontierEntities.TRADER.get(), 2, 2, 2);
        TradeNetworkState state = TradeNetworkState.get(helper.getLevel());
        long origin = state.registerVillage(helper.absolutePos(new BlockPos(2, 1, 2)));
        long destination = state.registerVillage(helper.absolutePos(new BlockPos(11, 1, 2)));
        trader.setJourney(origin, destination, List.of(
                helper.absolutePos(new BlockPos(2, 1, 2)),
                helper.absolutePos(new BlockPos(6, 1, 2)),
                helper.absolutePos(new BlockPos(11, 1, 2))));
        BlockPos destinationPosition = helper.absolutePos(new BlockPos(11, 1, 2));
        helper.assertTrue(new RoadTravelGoal(trader, 0.85).canUse(),
                "Road trader journey is eligible to run");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(trader.distanceToSqr(destinationPosition.getX() + 0.5,
                            destinationPosition.getY() + 1, destinationPosition.getZ() + 0.5) < 25,
                    "Road trader reaches " + destinationPosition + "; current position " + trader.blockPosition());
            helper.succeed();
        });
    }

    @GameTest(template = "test/empty")
    public static void roadsidePopulationInitializesOnlyOnce(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(6, 1, 6));
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                helper.getLevel().setBlock(center.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                helper.getLevel().setBlock(center.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 2);
                helper.getLevel().setBlock(center.offset(x, 2, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        helper.getLevel().setBlock(center.above(), Blocks.CAMPFIRE.defaultBlockState(), 2);
        long id = TradeNetworkState.settlementId(center, 912);
        var site = new WildernessRoadNetwork.RoadsideSite(id, 0, 0, 17, 0, center,
                center.east(4), WildernessRoadNetwork.SiteType.CAMP, 0,
                List.of(center, center.east(), center.east(2), center.east(3), center.east(4)));
        TradeNetworkState state = TradeNetworkState.get(helper.getLevel());
        WildernessRoadNetwork network = WildernessRoadNetwork.forLevel(helper.getLevel());
        TradeNetworkManager.initializeSite(helper.getLevel(), network, state, site);
        TradeNetworkManager.initializeSite(helper.getLevel(), network, state, site);
        var residents = helper.getLevel().getEntitiesOfClass(TravellerEntity.class,
                new AABB(center).inflate(16));
        helper.assertTrue(state.isPopulated(id) && residents.size() == 1,
                "Roadside camps initialize once without duplicating residents");
        helper.succeed();
    }

    @GameTest(template = "test/empty")
    public static void disabledRoadSitesDoNotCreateBrokenVillageJourneys(GameTestHelper helper) {
        TraderEntity trader = FrontierEntities.TRADER.get().create(helper.getLevel());
        helper.assertTrue(trader != null, "Road trader entity is registered");
        boolean sites = FrontierConfig.ROADSIDE_SITES.get();
        FrontierConfig.ROADSIDE_SITES.set(false);
        try {
            helper.assertTrue(!TradeNetworkManager.configureVillageTrader(helper.getLevel(), trader,
                            helper.absolutePos(new BlockPos(3, 1, 3)), 771),
                    "Disabled roadside sites reject unrouteable village traders");
        } finally {
            FrontierConfig.ROADSIDE_SITES.set(sites);
        }
        helper.succeed();
    }
}
