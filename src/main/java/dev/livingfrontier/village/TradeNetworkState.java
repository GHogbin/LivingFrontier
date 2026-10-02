package dev.livingfrontier.village;

import dev.livingfrontier.road.WildernessRoadNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.SavedData;

public final class TradeNetworkState extends SavedData {
    private static final String DATA_NAME = "livingfrontier_trade_network";
    private final Map<Long, Settlement> settlements = new HashMap<>();

    public static TradeNetworkState get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                TradeNetworkState::load, TradeNetworkState::new, DATA_NAME);
    }

    public static TradeNetworkState load(CompoundTag tag) {
        TradeNetworkState state = new TradeNetworkState();
        ListTag values = tag.getList("Settlements", Tag.TAG_COMPOUND);
        for (int index = 0; index < values.size(); index++) {
            CompoundTag value = values.getCompound(index);
            long id = value.getLong("Id");
            Settlement settlement = new Settlement(id, BlockPos.of(value.getLong("Center")),
                    SettlementKind.valueOf(value.getString("Kind")), value.getLong("Partner"),
                    value.getBoolean("Populated"), value.getLong("LastRefresh"),
                    value.getInt("Imports"), value.getInt("Exports"));
            CompoundTag stock = value.getCompound("Stock");
            for (TradeGood good : TradeGood.values()) {
                settlement.stock.put(good, stock.getInt(good.name()));
            }
            state.settlements.put(id, settlement);
        }
        return state;
    }

    public long registerVillage(BlockPos center) {
        long id = settlementId(center, 0x56494C4C);
        register(id, center, SettlementKind.VILLAGE, 0);
        return id;
    }

    public long registerSite(WildernessRoadNetwork.RoadsideSite site) {
        SettlementKind kind = site.type() == WildernessRoadNetwork.SiteType.HAMLET
                ? SettlementKind.HAMLET : SettlementKind.CAMP;
        register(site.id(), site.center(), kind, site.partnerId());
        return site.id();
    }

    public void register(long id, BlockPos center, SettlementKind kind, long partnerId) {
        Settlement existing = settlements.get(id);
        if (existing != null) {
            if (partnerId != 0 && existing.partnerId != partnerId) {
                existing.partnerId = partnerId;
                setDirty();
            }
            return;
        }
        Settlement settlement = new Settlement(id, center.immutable(), kind, partnerId,
                false, -1, 0, 0);
        seedStock(settlement);
        settlements.put(id, settlement);
        setDirty();
    }

    public Optional<SettlementView> settlement(long id) {
        Settlement settlement = settlements.get(id);
        return settlement == null ? Optional.empty() : Optional.of(settlement.view());
    }

    public OptionalLong nearestSettlement(BlockPos position, int radius, long excludedId) {
        long bestId = 0;
        long bestDistance = (long) radius * radius + 1;
        for (Settlement settlement : settlements.values()) {
            if (settlement.id == excludedId) continue;
            long distance = distance(position, settlement.center);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestId = settlement.id;
            }
        }
        return bestId == 0 ? OptionalLong.empty() : OptionalLong.of(bestId);
    }

    public void setPartner(long id, long partnerId) {
        Settlement settlement = settlements.get(id);
        if (settlement != null && settlement.partnerId != partnerId) {
            settlement.partnerId = partnerId;
            setDirty();
        }
    }

    public boolean isPopulated(long id) {
        Settlement settlement = settlements.get(id);
        return settlement != null && settlement.populated;
    }

    public void markPopulated(long id) {
        Settlement settlement = settlements.get(id);
        if (settlement != null && !settlement.populated) {
            settlement.populated = true;
            setDirty();
        }
    }

    public int stock(long id, TradeGood good) {
        Settlement settlement = settlements.get(id);
        return settlement == null ? 0 : settlement.stock.getOrDefault(good, 0);
    }

    public void setStock(long id, TradeGood good, int count) {
        Settlement settlement = settlements.get(id);
        if (settlement != null) {
            settlement.stock.put(good, Math.max(0, count));
            setDirty();
        }
    }

    public int price(long id, TradeGood good) {
        int stock = stock(id, good);
        if (stock < good.target() / 2) return Math.min(32, good.baseEmeralds() * 2);
        if (stock > good.target() * 3 / 2) return Math.max(1, good.baseEmeralds() - 1);
        return good.baseEmeralds();
    }

    public void refresh(long id, long day) {
        Settlement settlement = settlements.get(id);
        if (settlement == null || settlement.lastRefresh >= day) return;
        long elapsed = settlement.lastRefresh < 0 ? 1 : Math.min(7, day - settlement.lastRefresh);
        TradeGood specialty = TradeGood.values()[Math.floorMod(Long.hashCode(id), TradeGood.values().length)];
        for (int pass = 0; pass < elapsed; pass++) {
            for (TradeGood good : TradeGood.values()) {
                int stock = settlement.stock.getOrDefault(good, 0);
                if (good == specialty) stock += Math.max(1, good.target() / 3);
                else stock -= Math.max(1, good.target() / 16);
                settlement.stock.put(good, Math.max(0, Math.min(good.target() * 3, stock)));
            }
        }
        settlement.lastRefresh = day;
        setDirty();
    }

    public void arriveAndReload(long id, SimpleContainer cargo, long day) {
        Settlement settlement = settlements.get(id);
        if (settlement == null) return;
        refresh(id, day);
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            ItemStack stack = cargo.removeItemNoUpdate(slot);
            if (stack.isEmpty()) continue;
            TradeGood.forItem(stack.getItem()).ifPresent(good -> {
                settlement.stock.merge(good, stack.getCount(), Integer::sum);
                settlement.imports += stack.getCount();
            });
        }
        List<TradeGood> exports = new ArrayList<>(List.of(TradeGood.values()));
        exports.sort(Comparator.comparingInt((TradeGood good) ->
                settlement.stock.getOrDefault(good, 0) - good.target()).reversed());
        for (TradeGood good : exports) {
            if (!hasRoom(cargo)) break;
            int available = Math.max(0, settlement.stock.getOrDefault(good, 0) - good.target() / 2);
            int count = Math.min(good.bundle() * 2, available);
            if (count < good.bundle()) continue;
            ItemStack remainder = cargo.addItem(new ItemStack(good.item(), count));
            int loaded = count - remainder.getCount();
            settlement.stock.merge(good, -loaded, Integer::sum);
            settlement.exports += loaded;
        }
        setDirty();
    }

    public MerchantOffers offers(long destinationId, Container cargo) {
        MerchantOffers offers = new MerchantOffers();
        Map<TradeGood, Integer> cargoCounts = new EnumMap<>(TradeGood.class);
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            ItemStack stack = cargo.getItem(slot);
            TradeGood.forItem(stack.getItem()).ifPresent(good -> cargoCounts.merge(good, stack.getCount(), Integer::sum));
        }
        cargoCounts.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(5).forEach(entry -> {
            TradeGood good = entry.getKey();
            int bundle = Math.min(good.bundle(), entry.getValue());
            int uses = Math.max(1, entry.getValue() / bundle);
            offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, price(destinationId, good)),
                    new ItemStack(good.item(), bundle), uses, 1, 0));
        });
        Settlement destination = settlements.get(destinationId);
        if (destination != null) {
            List<TradeGood> demands = new ArrayList<>(List.of(TradeGood.values()));
            demands.sort(Comparator.comparingDouble(good ->
                    (double) destination.stock.getOrDefault(good, 0) / good.target()));
            int emptySlots = emptySlots(cargo);
            int purchaseOffers = 0;
            for (TradeGood good : demands) {
                if (purchaseOffers >= 3) break;
                int capacity = partialCapacity(cargo, good);
                if (capacity < good.bundle() && emptySlots > 0) {
                    capacity += good.item().getDefaultInstance().getMaxStackSize();
                    emptySlots--;
                }
                int uses = Math.min(6, capacity / good.bundle());
                if (uses <= 0) continue;
                offers.add(new MerchantOffer(new ItemStack(good.item(), good.bundle()),
                        new ItemStack(Items.EMERALD, Math.max(1, price(destinationId, good) / 2)),
                        uses, 1, 0));
                purchaseOffers++;
            }
        }
        return offers;
    }

    public void traderCompletedOffer(long destinationId, Container cargo, MerchantOffer offer) {
        ItemStack result = offer.getResult();
        if (result.is(Items.EMERALD)) {
            ItemStack sold = offer.getBaseCostA();
            int stored = addCargo(cargo, sold);
            int overflow = sold.getCount() - stored;
            Settlement destination = settlements.get(destinationId);
            if (overflow > 0 && destination != null) {
                TradeGood.forItem(sold.getItem()).ifPresent(good -> {
                    destination.stock.merge(good, overflow, Integer::sum);
                    destination.imports += overflow;
                });
            }
        } else {
            removeCargo(cargo, result);
        }
        Settlement destination = settlements.get(destinationId);
        if (destination != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag values = new ListTag();
        settlements.values().stream().sorted(Comparator.comparingLong(settlement -> settlement.id)).forEach(settlement -> {
            CompoundTag value = new CompoundTag();
            value.putLong("Id", settlement.id);
            value.putLong("Center", settlement.center.asLong());
            value.putString("Kind", settlement.kind.name());
            value.putLong("Partner", settlement.partnerId);
            value.putBoolean("Populated", settlement.populated);
            value.putLong("LastRefresh", settlement.lastRefresh);
            value.putInt("Imports", settlement.imports);
            value.putInt("Exports", settlement.exports);
            CompoundTag stock = new CompoundTag();
            settlement.stock.forEach((good, count) -> stock.putInt(good.name(), count));
            value.put("Stock", stock);
            values.add(value);
        });
        tag.put("Settlements", values);
        return tag;
    }

    public static long settlementId(BlockPos position, int salt) {
        long value = position.asLong() ^ (long) salt * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static void seedStock(Settlement settlement) {
        long seed = settlement.id;
        for (TradeGood good : TradeGood.values()) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int count = good.target() / 2 + Math.floorMod((int) (seed >>> 32), good.target() + 1);
            settlement.stock.put(good, count);
        }
        TradeGood specialty = TradeGood.values()[Math.floorMod(Long.hashCode(settlement.id), TradeGood.values().length)];
        settlement.stock.put(specialty, specialty.target() * 2);
    }

    private static boolean hasRoom(SimpleContainer cargo) {
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            if (cargo.getItem(slot).isEmpty()) return true;
        }
        return false;
    }

    private static int addCargo(Container cargo, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < cargo.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = cargo.getItem(slot);
            if (existing.isEmpty()) {
                cargo.setItem(slot, remaining);
                return stack.getCount();
            }
            if (ItemStack.isSameItemSameTags(existing, remaining)) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }
        }
        return stack.getCount() - remaining.getCount();
    }

    private static int emptySlots(Container cargo) {
        int slots = 0;
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            if (cargo.getItem(slot).isEmpty()) slots++;
        }
        return slots;
    }

    private static int partialCapacity(Container cargo, TradeGood good) {
        int capacity = 0;
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            ItemStack stack = cargo.getItem(slot);
            if (stack.is(good.item())) capacity += stack.getMaxStackSize() - stack.getCount();
        }
        return capacity;
    }

    private static void removeCargo(Container cargo, ItemStack stack) {
        int remaining = stack.getCount();
        for (int slot = 0; slot < cargo.getContainerSize() && remaining > 0; slot++) {
            ItemStack existing = cargo.getItem(slot);
            if (!ItemStack.isSameItemSameTags(existing, stack)) continue;
            int removed = Math.min(remaining, existing.getCount());
            existing.shrink(removed);
            remaining -= removed;
            if (existing.isEmpty()) cargo.setItem(slot, ItemStack.EMPTY);
        }
    }

    private static long distance(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    public enum SettlementKind {
        VILLAGE, CAMP, HAMLET
    }

    public record SettlementView(long id, BlockPos center, SettlementKind kind, long partnerId,
            boolean populated, int imports, int exports, Map<TradeGood, Integer> stock) {
    }

    private static final class Settlement {
        private final long id;
        private final BlockPos center;
        private final SettlementKind kind;
        private long partnerId;
        private boolean populated;
        private long lastRefresh;
        private int imports;
        private int exports;
        private final EnumMap<TradeGood, Integer> stock = new EnumMap<>(TradeGood.class);

        private Settlement(long id, BlockPos center, SettlementKind kind, long partnerId,
                boolean populated, long lastRefresh, int imports, int exports) {
            this.id = id;
            this.center = center;
            this.kind = kind;
            this.partnerId = partnerId;
            this.populated = populated;
            this.lastRefresh = lastRefresh;
            this.imports = imports;
            this.exports = exports;
        }

        private SettlementView view() {
            return new SettlementView(id, center, kind, partnerId, populated, imports, exports,
                    Map.copyOf(stock));
        }
    }
}
