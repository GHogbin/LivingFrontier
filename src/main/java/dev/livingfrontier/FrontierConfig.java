package dev.livingfrontier;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FrontierConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DEER_WEIGHT;
    public static final ForgeConfigSpec.IntValue SONGBIRD_WEIGHT;
    public static final ForgeConfigSpec.IntValue FIREFLY_WEIGHT;
    public static final ForgeConfigSpec.IntValue RAIDER_WEIGHT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Relative natural spawn weights. Zero disables a mob. Restart after editing.").push("spawning");
        DEER_WEIGHT = builder.defineInRange("deerWeight", 8, 0, 100);
        SONGBIRD_WEIGHT = builder.defineInRange("songbirdWeight", 10, 0, 100);
        FIREFLY_WEIGHT = builder.defineInRange("fireflyWeight", 12, 0, 100);
        RAIDER_WEIGHT = builder.defineInRange("raiderWeight", 4, 0, 100);
        builder.pop();
        SPEC = builder.build();
    }

    private FrontierConfig() {
    }
}
