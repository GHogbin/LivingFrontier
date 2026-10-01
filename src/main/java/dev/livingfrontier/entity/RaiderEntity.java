package dev.livingfrontier.entity;

import dev.livingfrontier.FrontierSpawnRules;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import javax.annotation.Nullable;

public class RaiderEntity extends Monster {
    private @Nullable BlockPos home;

    public RaiderEntity(EntityType<? extends RaiderEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 26).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ARMOR, 2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ReturnHomeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(3, new FollowPackLeaderGoal<>(this, RaiderEntity.class,
                raider -> !raider.isGuard() && !(raider instanceof WarlordEntity)));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData groupData, @Nullable CompoundTag spawnTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData, spawnTag);
        if (reason == MobSpawnType.STRUCTURE) {
            home = blockPosition();
            setPersistenceRequired();
        }
        return result;
    }

    public boolean isGuard() {
        return home != null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        if (home != null) {
            output.putLong("FrontierHome", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        home = input.contains("FrontierHome") ? BlockPos.of(input.getLong("FrontierHome")) : null;
    }

    public static boolean canSpawn(EntityType<RaiderEntity> type, ServerLevelAccessor level, MobSpawnType reason,
            BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && FrontierSpawnRules.isOutdoors(level, pos)
                && level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
                && checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    public float getWalkTargetValue(BlockPos position, LevelReader level) {
        // Patrols roam in daylight rather than inheriting monsters' darkness preference.
        return 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    private final class ReturnHomeGoal extends Goal {
        private ReturnHomeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return home != null && home.distSqr(blockPosition()) > 22 * 22;
        }

        @Override
        public boolean canContinueToUse() {
            return home != null && home.distSqr(blockPosition()) > 5 * 5;
        }

        @Override
        public void start() {
            setTarget(null);
        }

        @Override
        public void tick() {
            if (home != null && tickCount % 20 == 0) {
                navigation.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
            }
        }
    }

}
