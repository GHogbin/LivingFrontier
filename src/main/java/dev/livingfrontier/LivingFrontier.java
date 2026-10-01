package dev.livingfrontier;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LivingFrontier.MOD_ID)
public final class LivingFrontier {
    public static final String MOD_ID = "livingfrontier";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingFrontier(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        FrontierEntities.ENTITY_TYPES.register(bus);
        FrontierItems.ITEMS.register(bus);
        FrontierSpawnModifier.SERIALIZERS.register(bus);
        bus.addListener(FrontierEntities::createAttributes);
        bus.addListener(FrontierEntities::registerSpawnPlacements);
        bus.addListener(FrontierItems::creativeTabs);
        context.registerConfig(ModConfig.Type.COMMON, FrontierConfig.SPEC);
        LOGGER.info("Living Frontier Forge beta: wildlife, roaming raiders and guarded wilderness bases");
    }
}
