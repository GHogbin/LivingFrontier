package dev.livingfrontier.test;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.minecraft.core.BlockPos;

public final class SiteLayouts {
    public static final Layouts LAYOUTS = read();
    public static final Site CAMP = LAYOUTS.camp();
    public static final Site KEEP = LAYOUTS.keep();

    private SiteLayouts() {
    }

    private static Layouts read() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(SiteLayouts.class.getResourceAsStream(
                "/data/livingfrontier/layouts/sites.json")), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Layouts.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read generated site layout metadata", exception);
        }
    }

    public static BlockPos pos(int[] coordinates) {
        if (coordinates.length != 3) {
            throw new IllegalArgumentException("Expected a three-dimensional site coordinate");
        }
        return new BlockPos(coordinates[0], coordinates[1], coordinates[2]);
    }

    public static BlockPos at(BlockPos origin, int[] coordinates) {
        return origin.offset(pos(coordinates));
    }

    public record Layouts(Site camp, Site keep) {
    }

    public record Site(int[] size, int guards, int[][] chests, int[] entrance, int[] gateController,
            int[] gateOrigin, int gateWidth, int gateHeight, int[] bossPosition, Room[] rooms, int solutionLength) {
    }

    public record Room(String name, int room, Rune[] runes, int[] hintPosition) {
    }

    public record Rune(String symbol, int[] pos) {
    }
}
