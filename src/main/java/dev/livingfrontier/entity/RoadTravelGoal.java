package dev.livingfrontier.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

public final class RoadTravelGoal extends Goal {
    private final TraderEntity trader;
    private final double speed;
    private int repathDelay;
    private int failedPaths;
    private int stalledTicks;
    private double lastDistance = Double.MAX_VALUE;

    public RoadTravelGoal(TraderEntity trader, double speed) {
        this.trader = trader;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !trader.isTrading() && trader.canTravel() && trader.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repathDelay = 0;
        failedPaths = 0;
        stalledTicks = 0;
        lastDistance = Double.MAX_VALUE;
    }

    @Override
    public void tick() {
        BlockPos target = trader.currentWaypoint();
        if (target == null) {
            trader.completeJourney();
            return;
        }
        if (!(trader.level() instanceof ServerLevel level) || !level.hasChunkAt(target)) {
            trader.getNavigation().stop();
            return;
        }
        double targetX = target.getX() + 0.5;
        double targetY = target.getY() + 1;
        double targetZ = target.getZ() + 0.5;
        double distance = trader.distanceToSqr(targetX, targetY, targetZ);
        if (distance < 4) {
            trader.advanceWaypoint();
            repathDelay = 0;
            failedPaths = 0;
            stalledTicks = 0;
            lastDistance = Double.MAX_VALUE;
            return;
        }
        if (distance + 0.25 < lastDistance) {
            lastDistance = distance;
            stalledTicks = 0;
        } else if (++stalledTicks >= 100) {
            trader.recoverFromUnreachableWaypoint();
            stalledTicks = 0;
            lastDistance = Double.MAX_VALUE;
            return;
        }
        if (--repathDelay <= 0) {
            repathDelay = 20;
            if (trader.getNavigation().moveTo(targetX, targetY, targetZ, speed)) {
                failedPaths = Math.max(0, failedPaths - 1);
            } else if (++failedPaths >= 5) {
                trader.recoverFromUnreachableWaypoint();
                failedPaths = 0;
            }
        }
    }

    @Override
    public void stop() {
        trader.getNavigation().stop();
    }
}
