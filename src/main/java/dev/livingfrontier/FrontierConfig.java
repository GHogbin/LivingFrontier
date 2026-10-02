package dev.livingfrontier;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FrontierConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DEER_WEIGHT;
    public static final ForgeConfigSpec.IntValue SONGBIRD_WEIGHT;
    public static final ForgeConfigSpec.IntValue FIREFLY_WEIGHT;
    public static final ForgeConfigSpec.IntValue RAIDER_WEIGHT;
    public static final ForgeConfigSpec.IntValue BOAR_WEIGHT;
    public static final ForgeConfigSpec.IntValue PROWLER_WEIGHT;
    public static final ForgeConfigSpec.IntValue SKY_WRAITH_WEIGHT;
    public static final ForgeConfigSpec.BooleanValue ENCOUNTERS_ENABLED;
    public static final ForgeConfigSpec.IntValue ENCOUNTER_MIN_MINUTES;
    public static final ForgeConfigSpec.IntValue ENCOUNTER_MAX_MINUTES;
    public static final ForgeConfigSpec.IntValue ENCOUNTER_LOCAL_CAP;
    public static final ForgeConfigSpec.IntValue ENCOUNTER_GLOBAL_CAP;
    public static final ForgeConfigSpec.IntValue TRAVELLER_WEIGHT;
    public static final ForgeConfigSpec.BooleanValue VILLAGE_LIFE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue VILLAGE_MARKETS;
    public static final ForgeConfigSpec.BooleanValue VILLAGER_CONVERSATIONS;
    public static final ForgeConfigSpec.IntValue VILLAGE_GUARDS;
    public static final ForgeConfigSpec.BooleanValue WILDERNESS_ROADS;
    public static final ForgeConfigSpec.IntValue ROAD_REGION_SIZE;
    public static final ForgeConfigSpec.IntValue ROAD_SPUR_LENGTH;
    public static final ForgeConfigSpec.BooleanValue ROADSIDE_SITES;
    public static final ForgeConfigSpec.BooleanValue TRADE_NETWORK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Relative natural spawn weights. Zero disables a mob. Restart after editing.").push("spawning");
        DEER_WEIGHT = builder.defineInRange("deerWeight", 8, 0, 100);
        SONGBIRD_WEIGHT = builder.defineInRange("songbirdWeight", 10, 0, 100);
        FIREFLY_WEIGHT = builder.defineInRange("fireflyWeight", 12, 0, 100);
        RAIDER_WEIGHT = builder.defineInRange("raiderWeight", 4, 0, 100);
        BOAR_WEIGHT = builder.defineInRange("boarWeight", 6, 0, 100);
        PROWLER_WEIGHT = builder.defineInRange("prowlerWeight", 5, 0, 100);
        SKY_WRAITH_WEIGHT = builder.defineInRange("skyWraithWeight", 3, 0, 100);
        TRAVELLER_WEIGHT = builder.defineInRange("travellerWeight", 3, 0, 100);
        builder.pop();
        builder.comment("Occasional hostile exploration encounters; excluded near villages, lit ground and guarded bases.")
                .push("encounters");
        ENCOUNTERS_ENABLED = builder.define("enabled", true);
        ENCOUNTER_MIN_MINUTES = builder.comment("Minimum interval, including the first encounter.")
                .defineInRange("minMinutes", 6, 1, 120);
        ENCOUNTER_MAX_MINUTES = builder.comment("Maximum interval; values below minMinutes are clamped to it.")
                .defineInRange("maxMinutes", 12, 1, 120);
        ENCOUNTER_LOCAL_CAP = builder.comment("Maximum roaming Frontier hostiles within 96 blocks, including a new group.")
                .defineInRange("localCap", 6, 1, 32);
        ENCOUNTER_GLOBAL_CAP = builder.comment("Maximum living encounter-tagged mobs loaded in the Overworld.")
                .defineInRange("globalCap", 24, 1, 128);
        builder.pop();
        builder.push("villageLife");
        VILLAGE_LIFE_ENABLED = builder.comment("One-time extra residents near inhabited village bells; no vanilla villagers replaced.")
                .define("enabled", true);
        VILLAGE_MARKETS = builder.comment("Build one small stall only on an empty, flat natural-ground patch. No buildings overwritten.")
                .define("markets", true);
        VILLAGER_CONVERSATIONS = builder.comment("Sneak-right-click adult villagers for conversation and one supply errand per player per game-day.")
                .define("conversations", true);
        VILLAGE_GUARDS = builder.comment("Initial guards per discovered inhabited village, not respawned after defeat.")
                .defineInRange("guardsPerVillage", 2, 0, 4);
        builder.pop();
        builder.comment("Seeded wilderness roads and roadside settlements in NEW Overworld chunks only.")
                .push("wildernessRoads");
        WILDERNESS_ROADS = builder.define("enabled", true);
        ROAD_REGION_SIZE = builder.comment("Approximate spacing between deterministic road crossroads.")
                .defineInRange("regionSize", 384, 256, 768);
        ROAD_SPUR_LENGTH = builder.comment("Maximum length of an extra dead-end road from each crossroad.")
                .defineInRange("spurLength", 224, 96, 512);
        ROADSIDE_SITES = builder.comment("Generate small camps and hamlets beside viable roads.")
                .define("roadsideSites", true);
        TRADE_NETWORK = builder.comment("Populate roadside sites and villages with persistent cargo traders.")
                .define("tradeNetwork", true);
        builder.pop();
        SPEC = builder.build();
    }

    private FrontierConfig() {
    }
}
