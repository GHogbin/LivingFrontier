package dev.livingfrontier.entity;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.road.WildernessRoadNetwork;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

public final class RoadWanderGoal extends Goal {
    private final TravellerEntity traveller;
    private final double speed;
    private BlockPos target;

    public RoadWanderGoal(TravellerEntity traveller, double speed) {
        this.traveller = traveller;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!FrontierConfig.WILDERNESS_ROADS.get() || traveller.isVillageResident()
                || traveller.getRandom().nextInt(80) != 0
                || !(traveller.level() instanceof ServerLevel level)) return false;
        List<BlockPos> waypoints = WildernessRoadNetwork.forLevel(level)
                .roadWaypointsNear(traveller.blockPosition(), 96);
        if (waypoints.isEmpty()) return false;
        int start = Math.max(0, waypoints.size() / 2);
        target = waypoints.get(start + traveller.getRandom().nextInt(waypoints.size() - start));
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && !traveller.getNavigation().isDone()
                && traveller.distanceToSqr(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5) > 4;
    }

    @Override
    public void start() {
        traveller.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, speed);
    }

    @Override
    public void stop() {
        target = null;
    }
}
