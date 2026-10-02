package dev.livingfrontier.test;

import dev.livingfrontier.village.TradeGood;
import dev.livingfrontier.village.TradeNetworkState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class TradeNetworkGameTests {
    private TradeNetworkGameTests() {
    }

    @GameTest(template = "test/empty")
    public static void settlementStocksSurviveSave(GameTestHelper helper) {
        TradeNetworkState state = new TradeNetworkState();
        long village = state.registerVillage(new BlockPos(80, 64, 80));
        state.setStock(village, TradeGood.IRON, 37);
        TradeNetworkState restored = TradeNetworkState.load(state.save(new CompoundTag()));
        helper.assertTrue(restored.stock(village, TradeGood.IRON) == 37,
                "Settlement stock survives SavedData round trips");
        helper.succeed();
    }

    @GameTest(template = "test/empty")
    public static void cargoTransferChangesStocksAndPrices(GameTestHelper helper) {
        TradeNetworkState state = new TradeNetworkState();
        long village = state.registerVillage(new BlockPos(120, 64, 120));
        state.setStock(village, TradeGood.WHEAT, 0);
        int scarce = state.price(village, TradeGood.WHEAT);
        SimpleContainer cargo = new SimpleContainer(8);
        cargo.setItem(0, new ItemStack(Items.WHEAT, 32));
        state.arriveAndReload(village, cargo, 2);
        int abundant = state.price(village, TradeGood.WHEAT);
        var view = state.settlement(village).orElseThrow();
        helper.assertTrue(view.imports() >= 32 && abundant < scarce,
                "Delivered cargo becomes village stock and lowers scarcity pricing");
        helper.assertTrue(view.exports() > 0 && !cargo.isEmpty(),
                "The destination reloads the trader with local exports");
        helper.succeed();
    }

    @GameTest(template = "test/empty")
    public static void purchaseOrdersRequireCargoCapacity(GameTestHelper helper) {
        TradeNetworkState state = new TradeNetworkState();
        long village = state.registerVillage(new BlockPos(180, 64, 180));
        SimpleContainer cargo = new SimpleContainer(8);
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            cargo.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        }
        helper.assertTrue(state.offers(village, cargo).stream()
                        .noneMatch(offer -> offer.getResult().is(Items.EMERALD)),
                "Full cargo inventories publish no purchase orders that would discard player goods");
        helper.succeed();
    }
}
