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
        SPEC = builder.build();
    }

    private FrontierConfig() {
    }
}
