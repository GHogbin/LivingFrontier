package dev.livingfrontier.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class SkyWraithEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DIVING =
            SynchedEntityData.defineId(SkyWraithEntity.class, EntityDataSerializers.BOOLEAN);

    public SkyWraithEntity(EntityType<? extends SkyWraithEntity> type, Level level) {
        super(type, level);
        moveControl = new WraithMoveControl();
        setNoGravity(true);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 18).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.4).add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DIVING, false);
    }

    public boolean isDiving() {
        return entityData.get(DIVING);
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
        goalSelector.addGoal(1, new SwoopGoal());
        goalSelector.addGoal(2, new AirWanderGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    public static boolean canSpawn(EntityType<SkyWraithEntity> type, ServerLevelAccessor level, MobSpawnType reason,
            BlockPos pos, RandomSource random) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getLevel().isNight()
                && level.canSeeSky(pos) && pos.getY() >= surface && pos.getY() <= surface + 24
                && level.getBrightness(LightLayer.BLOCK, pos) <= 7 && level.getFluidState(pos).isEmpty();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    private final class WraithMoveControl extends MoveControl {
        private WraithMoveControl() {
            super(SkyWraithEntity.this);
        }

        @Override
        public void tick() {
            setZza(0);
            setYya(0);
            setXxa(0);
            if (operation == Operation.MOVE_TO) {
                Vec3 offset = new Vec3(wantedX - getX(), wantedY - getY(), wantedZ - getZ());
                if (offset.lengthSqr() < 0.25) {
                    operation = Operation.WAIT;
                } else {
                    // Ground travel's air-input speed cannot keep up with a circling flight target.
                    Vec3 desired = offset.normalize().scale(getAttributeValue(Attributes.FLYING_SPEED) * speedModifier);
                    setDeltaMovement(getDeltaMovement().lerp(desired, 0.2));
                    setYRot((float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90);
                    return;
                }
            }
            setDeltaMovement(getDeltaMovement().scale(0.9));
        }
    }

    private final class SwoopGoal extends Goal {
        private int ticks;
        private int phase;
        private double angle;

        private SwoopGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) < 64 * 64
                    && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            phase = 0;
            ticks = 60;
            angle = random.nextDouble() * Math.PI * 2;
            entityData.set(DIVING, false);
            navigation.stop();
        }

        @Override
        public void stop() {
            entityData.set(DIVING, false);
            moveControl.setWantedPosition(getX(), getY(), getZ(), 0);
            setDeltaMovement(getDeltaMovement().scale(0.5));
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            if (phase == 2) {
                moveControl.setWantedPosition(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 1.6);
                if (distanceToSqr(target) < 2.6 && hasLineOfSight(target)) {
                    doHurtTarget(target);
                    phase = 3;
                    ticks = 80;
                    entityData.set(DIVING, false);
                }
            } else {
                angle += 0.035;
                double radius = phase == 1 ? 4 : 7;
                moveControl.setWantedPosition(target.getX() + Math.cos(angle) * radius,
                        target.getY() + 5, target.getZ() + Math.sin(angle) * radius, 1.0);
                if (phase == 1 && level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 2, 0.2, 0.2, 0.2, 0);
                }
            }
            if (--ticks <= 0) {
                if (phase == 0 && hasLineOfSight(target)) {
                    phase = 1;
                    ticks = 20;
                    playSound(SoundEvents.PHANTOM_FLAP, 1, 0.6F);
                } else if (phase == 1) {
                    phase = 2;
                    ticks = 40;
                    entityData.set(DIVING, true);
                    playSound(SoundEvents.PHANTOM_SWOOP, 0.8F, 1.2F);
                } else {
                    phase = 0;
                    ticks = 60;
                    entityData.set(DIVING, false);
                }
            }
        }
    }

    private final class AirWanderGoal extends Goal {
        private BlockPos destination = BlockPos.ZERO;

        private AirWanderGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || random.nextInt(30) != 0) {
                return false;
            }
            int x = blockPosition().getX() + random.nextInt(17) - 8;
            int z = blockPosition().getZ() + random.nextInt(17) - 8;
            if (!level().hasChunkAt(new BlockPos(x, blockPosition().getY(), z))) {
                return false;
            }
            int height = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            destination = new BlockPos(x, height + 5 + random.nextInt(4), z);
            return level().isEmptyBlock(destination) && level().getFluidState(destination).isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && !navigation.isDone();
        }

        @Override
        public void start() {
            navigation.moveTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 0.9);
        }
    }
}
