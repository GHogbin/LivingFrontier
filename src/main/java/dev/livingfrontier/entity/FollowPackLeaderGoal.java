package dev.livingfrontier.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import javax.annotation.Nullable;

public final class FollowPackLeaderGoal<T extends PathfinderMob> extends Goal {
    private final T mob;
    private final Class<T> type;
    private final Predicate<T> eligible;
    private @Nullable T leader;

    public FollowPackLeaderGoal(T mob, Class<T> type, Predicate<T> eligible) {
        this.mob = mob;
        this.type = type;
        this.eligible = eligible;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!eligible.test(mob) || mob.getTarget() != null || mob.getRandom().nextInt(20) != 0) {
            return false;
        }
        leader = mob.level().getEntitiesOfClass(type, mob.getBoundingBox().inflate(16),
                other -> other.isAlive() && eligible.test(other) && other.getUUID().compareTo(mob.getUUID()) < 0)
                .stream().min(Comparator.comparing(PathfinderMob::getUUID)).orElse(null);
        return leader != null && mob.distanceToSqr(leader) > 16;
    }

    @Override
    public boolean canContinueToUse() {
        return leader != null && leader.isAlive() && eligible.test(mob) && eligible.test(leader)
                && mob.getTarget() == null && mob.distanceToSqr(leader) > 9 && mob.distanceToSqr(leader) < 32 * 32;
    }

    @Override
    public void tick() {
        if (leader != null && mob.tickCount % 10 == 0) {
            mob.getNavigation().moveTo(leader, 1.0);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        leader = null;
    }
}
