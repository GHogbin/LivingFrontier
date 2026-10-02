package dev.livingfrontier.road;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class WildernessRoadGameTests {
    private WildernessRoadGameTests() {
    }

    @GameTest(template = "test/empty", timeoutTicks = 200)
    public static void seededRoadsCreatePairedCampsAndHamlets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WildernessRoadNetwork network = WildernessRoadNetwork.forLevel(level);
        WildernessRoadNetwork.RoadCell selected = null;
        for (int x = -2; x <= 2 && selected == null; x++) {
            for (int z = -2; z <= 2; z++) {
                var cell = network.cell(x, z);
                if (cell.paths().size() >= 2 && cell.sites().size() == 2) {
                    selected = cell;
                    break;
                }
            }
        }
        helper.assertTrue(selected != null, "Seeded wilderness roads produce a viable roadside settlement pair");
        var camp = selected.sites().stream()
                .filter(site -> site.type() == WildernessRoadNetwork.SiteType.CAMP).findFirst().orElse(null);
        var hamlet = selected.sites().stream()
                .filter(site -> site.type() == WildernessRoadNetwork.SiteType.HAMLET).findFirst().orElse(null);
        helper.assertTrue(camp != null && hamlet != null && camp.partnerId() == hamlet.id()
                        && hamlet.partnerId() == camp.id(),
                "Each viable road cell pairs one camp with one hamlet");
        List<BlockPos> route = network.routeBetweenSites(camp.id(), hamlet.id());
        helper.assertTrue(route.size() > 32 && route.get(0).closerThan(camp.center(), 2)
                        && route.get(route.size() - 1).closerThan(hamlet.center(), 2),
                "The settlement pair has a continuous trade route");
        for (int index = 1; index < route.size(); index++) {
            BlockPos previous = route.get(index - 1);
            BlockPos current = route.get(index);
            helper.assertTrue(Math.abs(current.getX() - previous.getX())
                            + Math.abs(current.getZ() - previous.getZ()) <= 1
                            && Math.abs(current.getY() - previous.getY()) <= 1,
                    "Wilderness routes remain walkable");
        }
        helper.succeed();
    }
}
