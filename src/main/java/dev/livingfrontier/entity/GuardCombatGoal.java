package dev.livingfrontier.entity;

import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.LivingFrontier;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public final class GuardCombatGoal<T extends PathfinderMob & ArmedGuard> extends Goal {
    private final T mob;
    private final double meleeSpeed;
    private int meleeCooldown;
    private int rangedCooldown;

    public GuardCombatGoal(T mob, double meleeSpeed) {
        this.mob = mob;
        this.meleeSpeed = meleeSpeed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return validTarget(mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return validTarget(target) && mob.distanceToSqr(target) < 48 * 48;
    }

    private boolean validTarget(LivingEntity target) {
        return target != null && target.isAlive()
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        rangedCooldown = 10;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.stopUsingItem();
        mob.getNavigation().stop();
        mob.equipForRange(mob.isArcher());
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (meleeCooldown > 0) meleeCooldown--;
        if (rangedCooldown > 0) rangedCooldown--;
        mob.getLookControl().setLookAt(target, 30, 30);
        double distance = mob.distanceToSqr(target);
        boolean visible = mob.getSensing().hasLineOfSight(target);
        if (!mob.isArcher() || distance < 4 * 4) {
            mob.stopUsingItem();
            mob.equipForRange(false);
            mob.getNavigation().moveTo(target, meleeSpeed);
            double reach = Math.pow(mob.getBbWidth() * 2, 2) + target.getBbWidth();
            if (distance <= reach && visible && meleeCooldown == 0) {
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(target);
                meleeCooldown = 20;
            }
            return;
        }
        mob.equipForRange(true);
        if (distance > 18 * 18 || !visible) {
            mob.stopUsingItem();
            mob.getNavigation().moveTo(target, 1.0);
            return;
        }
        if (distance < 7 * 7) {
            Vec3 retreat = mob.position().add(mob.position().subtract(target.position()).normalize().scale(4));
            BlockPos destination = BlockPos.containing(retreat);
            if (mob.level().hasChunkAt(destination) && mob.isWithinRestriction(destination)) {
                mob.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 0.8);
            }
        } else {
            mob.getNavigation().stop();
        }
        if (!mob.isUsingItem() && rangedCooldown == 0) {
            mob.startUsingItem(InteractionHand.MAIN_HAND);
        }
        if (mob.isUsingItem() && mob.getTicksUsingItem() >= 20 && clearFiringLine(target)) {
            mob.stopUsingItem();
            shoot(target);
            rangedCooldown = 45 + mob.getRandom().nextInt(16);
        }
    }

    private boolean clearFiringLine(LivingEntity target) {
        Vec3 start = mob.getEyePosition();
        Vec3 end = target.getEyePosition();
        for (var entity : mob.level().getEntities(mob, mob.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                entity -> entity != target && entity.isAlive() && entity instanceof LivingEntity)) {
            if (GuardArrowEntity.protectedAlly(mob instanceof VillageGuardEntity, target.getUUID(), entity)
                    && entity.getBoundingBox().inflate(0.2).clip(start, end).isPresent()) {
                return false;
            }
        }
        return true;
    }

    private void shoot(LivingEntity target) {
        GuardArrowEntity arrow = FrontierEntities.GUARD_ARROW.get().create(mob.level());
        if (arrow == null) {
            throw new IllegalStateException("Registered guard arrow could not be created");
        }
        arrow.configure(mob, target);
        double x = target.getX() - mob.getX();
        double z = target.getZ() - mob.getZ();
        double y = target.getY() + target.getBbHeight() / 3 - arrow.getY();
        arrow.shoot(x, y + Math.sqrt(x * x + z * z) * 0.12, z, 1.6F, 3);
        if (!mob.level().addFreshEntity(arrow)) {
            LivingFrontier.LOGGER.warn("Could not add arrow fired by {}", mob.getUUID());
            return;
        }
        mob.playSound(SoundEvents.SKELETON_SHOOT, 0.9F, 1.0F);
    }
}
