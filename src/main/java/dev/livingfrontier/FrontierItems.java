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

    private FrontierItems() {
    }

    private static RegistryObject<Item> egg(String name, java.util.function.Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,
            int base, int spots) {
        return ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(type, base, spots, new Item.Properties()));
    }

    public static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            ITEMS.getEntries().forEach(event::accept);
        }
    }
}
