package dev.livingfrontier.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

public final class FireflyEntity extends PathfinderMob {
    public FireflyEntity(EntityType<? extends FireflyEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.12).add(Attributes.FLYING_SPEED, 0.15);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HoverGoal());
    }

    public boolean isNight() {
        return level().isNight();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    public static boolean canSpawn(EntityType<FireflyEntity> type, ServerLevelAccessor level, MobSpawnType reason,
            BlockPos pos, RandomSource random) {
        return level.getLevel().isNight() && level.canSeeSky(pos)
                && level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON);
    }

    private final class HoverGoal extends Goal {
        private double x;
        private double y;
        private double z;

        private HoverGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (random.nextInt(isNight() ? 20 : 100) != 0) {
                return false;
            }
            int targetX = blockPosition().getX() + random.nextInt(9) - 4;
            int targetZ = blockPosition().getZ() + random.nextInt(9) - 4;
            if (!level().hasChunkAt(new BlockPos(targetX, blockPosition().getY(), targetZ))) {
                return false;
            }
            int surface = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
            BlockPos target = new BlockPos(targetX, surface + (isNight() ? random.nextInt(3) : 0), targetZ);
            if (Math.abs(target.getY() - getY()) > 5) {
                return false;
            }
            if (!level().isEmptyBlock(target) || !level().getFluidState(target).isEmpty()) {
                return false;
            }
            x = target.getX() + 0.5;
            y = target.getY() + 0.5;
            z = target.getZ() + 0.5;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !navigation.isDone();
        }

        @Override
        public void start() {
            navigation.moveTo(x, y, z, isNight() ? 1.0 : 0.4);
        }
    }
}
