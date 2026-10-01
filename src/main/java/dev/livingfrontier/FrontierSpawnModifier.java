package dev.livingfrontier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public record FrontierSpawnModifier(HolderSet<Biome> biomes, Kind kind) implements BiomeModifier {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, LivingFrontier.MOD_ID);
    public static final RegistryObject<Codec<FrontierSpawnModifier>> CODEC = SERIALIZERS.register("wildlife_spawns",
            () -> RecordCodecBuilder.create(instance -> instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(FrontierSpawnModifier::biomes),
                    StringRepresentable.fromEnum(Kind::values).fieldOf("mob").forGetter(FrontierSpawnModifier::kind)
            ).apply(instance, FrontierSpawnModifier::new)));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && biomes.contains(biome)) {
            int weight = kind.weight.get();
            if (weight > 0) {
                EntityType<? extends Mob> type = kind.type.get();
                builder.getMobSpawnSettings().addSpawn(type.getCategory(),
                        new MobSpawnSettings.SpawnerData(type, weight, kind.min, kind.max));
            }
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }

    public enum Kind implements StringRepresentable {
        DEER("deer", FrontierEntities.DEER, FrontierConfig.DEER_WEIGHT, 2, 4),
        SONGBIRD("songbird", FrontierEntities.SONGBIRD, FrontierConfig.SONGBIRD_WEIGHT, 2, 4),
        FIREFLY("firefly", FrontierEntities.FIREFLY, FrontierConfig.FIREFLY_WEIGHT, 3, 6),
        RAIDER("raider", FrontierEntities.RAIDER, FrontierConfig.RAIDER_WEIGHT, 3, 5);

        private final String name;
        private final java.util.function.Supplier<? extends EntityType<? extends Mob>> type;
        private final ForgeConfigSpec.IntValue weight;
        private final int min;
        private final int max;

        Kind(String name, java.util.function.Supplier<? extends EntityType<? extends Mob>> type,
                ForgeConfigSpec.IntValue weight, int min, int max) {
            this.name = name;
            this.type = type;
            this.weight = weight;
            this.min = min;
            this.max = max;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
