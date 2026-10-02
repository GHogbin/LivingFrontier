package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class ClientModelCheck {
    public static void main(String[] args) {
        Map<String, LayerDefinition> layers = Map.ofEntries(
                Map.entry("deer", DeerModel.createBodyLayer()),
                Map.entry("songbird", SongbirdModel.createBodyLayer()),
                Map.entry("firefly", FireflyModel.createBodyLayer()),
                Map.entry("raider", RaiderModel.createBodyLayer(false)),
                Map.entry("warlord", RaiderModel.createBodyLayer(true)),
                Map.entry("boar", BoarModel.createBodyLayer()),
                Map.entry("prowler", ProwlerModel.createBodyLayer()),
                Map.entry("sky_wraith", SkyWraithModel.createBodyLayer()),
                Map.entry("traveller", FrontierPeopleModel.createBodyLayer(false)),
                Map.entry("trader", FrontierPeopleModel.createBodyLayer(false)),
                Map.entry("village_guard", FrontierPeopleModel.createBodyLayer(true)));
        for (var entry : layers.entrySet()) {
            var root = entry.getValue().bakeRoot();
            CountingVertices vertices = new CountingVertices(entry.getKey());
            root.render(new PoseStack(), vertices, 15728880, 0);
            if (vertices.count == 0) {
                throw new IllegalStateException(entry.getKey() + " model has no geometry");
            }
            System.out.println(entry.getKey() + ": baked " + vertices.count + " valid vertices");
            if (entry.getKey().equals("raider") || entry.getKey().equals("village_guard")) {
                checkBow(root, entry.getKey());
            } else if (entry.getKey().equals("warlord")) {
                require(!root.getChild("torso").getChild("right_arm").hasChild("bow"),
                        "warlord must retain its melee-only hammer");
            } else if (entry.getKey().equals("traveller") || entry.getKey().equals("trader")) {
                ModelPart torso = root.getChild("torso");
                require(!torso.getChild("right_arm").hasChild("bow")
                        && !torso.getChild("right_arm").hasChild("sword")
                        && !torso.getChild("left_arm").hasChild("shield"),
                        entry.getKey() + " must remain unarmed");
            }
        }
    }

    private static void checkBow(ModelPart root, String model) {
        ModelPart torso = root.getChild("torso");
        ModelPart rightArm = torso.getChild("right_arm");
        ModelPart leftArm = torso.getChild("left_arm");
        ModelPart bow = rightArm.getChild("bow");
        ModelPart weapon = rightArm.getChild(model.equals("raider") ? "weapon" : "sword");
        ModelPart[] melee = model.equals("raider") ? new ModelPart[]{weapon}
                : new ModelPart[]{weapon, leftArm.getChild("shield")};
        BowGeometry.setHoldingBow(bow, true, melee);
        require(bow.visible, model + " must show its equipped bow");
        for (ModelPart part : melee) {
            require(!part.visible, model + " must hide melee equipment while holding a bow");
        }
        require(BowGeometry.drawProgress(false, 20, 0.5F) == 0, "idle bow must not draw");
        require(BowGeometry.drawProgress(true, 20, 0.5F) == 1, "full bow draw must clamp");
        require(BowGeometry.drawProgress(true, 0, 0.5F) > 0, "bow draw must interpolate partial ticks");
        BowGeometry.pose(rightArm, leftArm, bow, 0, 0, true, 0);
        float initialPullArmYaw = leftArm.yRot;
        BowGeometry.pose(rightArm, leftArm, bow, 0, 0, true, 1);
        require(leftArm.yRot > initialPullArmYaw, model + " drawing arm must pull back");
        require(bow.getChild("upper_string").xRot > 0 && bow.getChild("lower_string").xRot < 0
                && bow.getChild("upper_string").yScale > 1, model + " string must draw without detaching");
        PoseStack orientation = new PoseStack();
        rightArm.translateAndRotate(orientation);
        bow.translateAndRotate(orientation);
        require(Math.abs(orientation.last().pose().m11() - 1) < 0.0001F,
                model + " aiming bow must remain upright");
        CountingVertices drawnVertices = new CountingVertices(model + "_drawn");
        root.render(new PoseStack(), drawnVertices, 15728880, 0);
        require(drawnVertices.count > 0, model + " drawn bow must render");
        root.getAllParts().forEach(part -> {
            part.resetPose();
            part.visible = true;
        });
        BowGeometry.setHoldingBow(bow, false, melee);
        require(!bow.visible, model + " close-range fallback must hide its bow");
        for (ModelPart part : melee) {
            require(part.visible, model + " close-range fallback must restore melee equipment");
        }
        require(bow.xRot == 0 && leftArm.yRot == 0 && bow.getChild("upper_string").yScale == 1,
                model + " bow pose must reset after switching to melee");
        System.out.println(model + ": bow draw, upright aim and melee visibility validated");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
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
