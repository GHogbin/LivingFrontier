package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class ClientModelCheck {
    public static void main(String[] args) {
        Map<String, LayerDefinition> layers = Map.of("deer", DeerModel.createBodyLayer(),
                "songbird", SongbirdModel.createBodyLayer(), "firefly", FireflyModel.createBodyLayer(),
                "raider", RaiderModel.createBodyLayer(false), "warlord", RaiderModel.createBodyLayer(true),
                "boar", BoarModel.createBodyLayer(), "prowler", ProwlerModel.createBodyLayer(),
                "sky_wraith", SkyWraithModel.createBodyLayer());
        for (var entry : layers.entrySet()) {
            var root = entry.getValue().bakeRoot();
            CountingVertices vertices = new CountingVertices(entry.getKey());
            root.render(new PoseStack(), vertices, 15728880, 0);
            if (vertices.count == 0) {
                throw new IllegalStateException(entry.getKey() + " model has no geometry");
            }
            System.out.println(entry.getKey() + ": baked " + vertices.count + " valid vertices");
        }
    }

    private static final class CountingVertices implements VertexConsumer {
        private final String model;
        private int count;

        private CountingVertices(String model) {
            this.model = model;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalStateException(model + " has a non-finite vertex");
            }
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            if (!Float.isFinite(u) || !Float.isFinite(v) || u < 0 || u > 1 || v < 0 || v > 1) {
                throw new IllegalStateException(model + " has out-of-atlas UV " + u + "," + v);
            }
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
            count++;
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
