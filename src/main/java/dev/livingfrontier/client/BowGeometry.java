package dev.livingfrontier.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class BowGeometry {
    private static final float STRING_HALF_HEIGHT = 9.4F;

    private BowGeometry() {
    }

    static void add(PartDefinition rightArm, int woodU, int woodV, int stringU, int stringV) {
        PartDefinition bow = rightArm.addOrReplaceChild("bow", CubeListBuilder.create()
                .texOffs(woodU, woodV).addBox(-0.55F, -1.8F, -0.45F, 1.1F, 3.6F, 0.9F),
                PartPose.offset(0, 9, -2.2F));
        for (int side : new int[]{-1, 1}) {
            String limb = side < 0 ? "upper" : "lower";
            bow.addOrReplaceChild(limb + "_inner", CubeListBuilder.create()
                    .texOffs(woodU, woodV).addBox(-0.45F, -1.7F, -0.45F, 0.9F, 3.4F, 0.9F),
                    PartPose.offsetAndRotation(0, side * 3, 0.35F, side * 0.25F, 0, 0));
            bow.addOrReplaceChild(limb + "_middle", CubeListBuilder.create()
                    .texOffs(woodU, woodV).addBox(-0.4F, -1.7F, -0.4F, 0.8F, 3.4F, 0.8F),
                    PartPose.offsetAndRotation(0, side * 6, 1.35F, side * 0.42F, 0, 0));
            bow.addOrReplaceChild(limb + "_tip", CubeListBuilder.create()
                    .texOffs(woodU, woodV).addBox(-0.35F, -1.4F, -0.35F, 0.7F, 2.8F, 0.7F),
                    PartPose.offsetAndRotation(0, side * 8.25F, 2.45F, side * 0.6F, 0, 0));
        }
        bow.addOrReplaceChild("upper_string", CubeListBuilder.create()
                .texOffs(stringU, stringV).addBox(-0.1F, 0, -0.1F, 0.2F, STRING_HALF_HEIGHT, 0.2F),
                PartPose.offset(0, -STRING_HALF_HEIGHT, 3.25F));
        bow.addOrReplaceChild("lower_string", CubeListBuilder.create()
                .texOffs(stringU, stringV).addBox(-0.1F, -STRING_HALF_HEIGHT, -0.1F,
                        0.2F, STRING_HALF_HEIGHT, 0.2F),
                PartPose.offset(0, STRING_HALF_HEIGHT, 3.25F));
    }

    static void setHoldingBow(ModelPart bow, boolean holdingBow, ModelPart... meleeWeapons) {
        if (bow != null) {
            bow.visible = holdingBow;
        }
        for (ModelPart weapon : meleeWeapons) {
            weapon.visible = !holdingBow;
        }
    }

    static float drawProgress(boolean usingBow, int ticksUsingItem, float partialTick) {
        if (!usingBow) {
            return 0;
        }
        float charge = Mth.clamp((ticksUsingItem + partialTick) / 20.0F, 0, 1);
        return (charge * charge + charge * 2) / 3;
    }

    static void pose(ModelPart rightArm, ModelPart leftArm, ModelPart bow,
            float yaw, float pitch, boolean aiming, float drawProgress) {
        float draw = Mth.clamp(drawProgress, 0, 1);
        float pull = draw * 3.8F;
        float stringAngle = (float) Math.atan2(pull, STRING_HALF_HEIGHT);
        float stringScale = (float) Math.sqrt(STRING_HALF_HEIGHT * STRING_HALF_HEIGHT + pull * pull)
                / STRING_HALF_HEIGHT;
        ModelPart upperString = bow.getChild("upper_string");
        ModelPart lowerString = bow.getChild("lower_string");
        upperString.xRot = stringAngle;
        lowerString.xRot = -stringAngle;
        upperString.yScale = stringScale;
        lowerString.yScale = stringScale;
        if (aiming) {
            rightArm.xRot = -Mth.PI / 2 + pitch;
            rightArm.yRot = yaw - 0.1F;
            rightArm.zRot = 0;
            leftArm.xRot = -Mth.PI / 2 + pitch - draw * 0.08F;
            leftArm.yRot = yaw + 0.55F + draw * 0.5F;
            leftArm.zRot = -0.08F * (1 - draw);
            // Cancel the raised arm's quarter turn so the bow stays upright.
            bow.xRot = Mth.PI / 2;
        }
    }
}
