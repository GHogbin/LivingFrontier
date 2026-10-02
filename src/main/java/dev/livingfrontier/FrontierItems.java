package dev.livingfrontier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FrontierItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LivingFrontier.MOD_ID);
    public static final RegistryObject<Item> DEER_EGG = egg("deer", FrontierEntities.DEER, 0x815632, 0xdacaa0);
    public static final RegistryObject<Item> SONGBIRD_EGG = egg("songbird", FrontierEntities.SONGBIRD, 0x70594a, 0xd36b3b);
    public static final RegistryObject<Item> FIREFLY_EGG = egg("firefly", FrontierEntities.FIREFLY, 0x26382b, 0xd6f36c);
    public static final RegistryObject<Item> RAIDER_EGG = egg("raider", FrontierEntities.RAIDER, 0x343e3b, 0x925942);
    public static final RegistryObject<Item> WARLORD_EGG = egg("warlord", FrontierEntities.WARLORD, 0x292a35, 0xd6ad56);
    public static final RegistryObject<Item> BOAR_EGG = egg("boar", FrontierEntities.BOAR, 0x624435, 0xdbcba4);
    public static final RegistryObject<Item> PROWLER_EGG = egg("prowler", FrontierEntities.PROWLER, 0x282c32, 0xe7af43);
    public static final RegistryObject<Item> SKY_WRAITH_EGG = egg("sky_wraith", FrontierEntities.SKY_WRAITH, 0x34335f, 0x9bdbe6);
    public static final RegistryObject<Item> TRAVELLER_EGG = egg("traveller", FrontierEntities.TRAVELLER, 0x805b44, 0x76ac8d);
    public static final RegistryObject<Item> TRADER_EGG = egg("trader", FrontierEntities.TRADER, 0x6b442e, 0xd4b45f);
    public static final RegistryObject<Item> VILLAGE_GUARD_EGG = egg("village_guard", FrontierEntities.VILLAGE_GUARD, 0x9babb3, 0x456686);

    private FrontierItems() {
    }

    private static RegistryObject<Item> egg(String name, java.util.function.Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,
            int base, int spots) {
        return ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(type, base, spots, new Item.Properties()));
    }

    public static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            ITEMS.getEntries().stream().filter(item -> item.get() instanceof ForgeSpawnEggItem).forEach(event::accept);
        }
    }
}
