package dev.livingfrontier.village;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum TradeGood {
    WHEAT(Items.WHEAT, 48, 8, 1),
    BREAD(Items.BREAD, 24, 4, 1),
    CARROTS(Items.CARROT, 48, 8, 1),
    POTATOES(Items.POTATO, 48, 8, 1),
    PAPER(Items.PAPER, 32, 8, 1),
    LEATHER(Items.LEATHER, 24, 4, 2),
    COAL(Items.COAL, 32, 8, 2),
    IRON(Items.IRON_INGOT, 16, 3, 3),
    LOGS(Items.OAK_LOG, 32, 8, 2),
    WOOL(Items.WHITE_WOOL, 20, 4, 2);

    private final Item item;
    private final int target;
    private final int bundle;
    private final int baseEmeralds;

    TradeGood(Item item, int target, int bundle, int baseEmeralds) {
        this.item = item;
        this.target = target;
        this.bundle = bundle;
        this.baseEmeralds = baseEmeralds;
    }

    public Item item() {
        return item;
    }

    public int target() {
        return target;
    }

    public int bundle() {
        return bundle;
    }

    public int baseEmeralds() {
        return baseEmeralds;
    }

    public static Optional<TradeGood> forItem(Item item) {
        return Arrays.stream(values()).filter(good -> good.item == item).findFirst();
    }
}
