package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.ProwlerEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class ProwlerModel extends EntityModel<ProwlerEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart[] legs;

    ProwlerModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = root.getChild("head");
        jaw = head.getChild("jaw");
        tail = root.getChild("tail");
        tailTip = tail.getChild("tip");
        legs = new ModelPart[]{root.getChild("front_left"), root.getChild("front_right"),
                root.getChild("back_left"), root.getChild("back_right")};
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -2.5F, -7, 7, 5, 14)
                .texOffs(0, 20).addBox(-4, -3, -6, 8, 6, 6)
                .texOffs(28, 20).addBox(-2.5F, 2.1F, -5, 5, 1, 11),
                PartPose.offset(0, 14, 1));
        for (int side : new int[]{-1, 1}) {
            body.addOrReplaceChild(side < 0 ? "right_ruff" : "left_ruff",
                    CubeListBuilder.create().texOffs(40, 48).addBox(-1, -1.5F, -2, 2, 3, 4),
                    PartPose.offsetAndRotation(side * 3.3F, -1.5F, -3, -0.2F, 0, side * 0.25F));
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.5F, -3, -4, 7, 5, 6)
                .texOffs(90, 0).addBox(-2, -0.5F, -7, 4, 2, 4)
                .texOffs(106, 0).addBox(-2, -0.6F, -7.6F, 4, 1, 1)
                .texOffs(24, 36).addBox(-1.8F, 1, -6.5F, 1, 2, 1)
                .texOffs(24, 36).addBox(0.8F, 1, -6.5F, 1, 2, 1)
                .texOffs(28, 36).addBox(-3.7F, -1.7F, -3.3F, 0.5F, 1.5F, 2)
                .texOffs(28, 36).addBox(3.2F, -1.7F, -3.3F, 0.5F, 1.5F, 2)
                .texOffs(36, 36).addBox(-3.8F, -1.4F, -3, 0.25F, 0.8F, 1.4F)
                .texOffs(36, 36).addBox(3.55F, -1.4F, -3, 0.25F, 0.8F, 1.4F),
                PartPose.offset(0, 13, -7));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(90, 8)
                .addBox(-2, 0, -3, 4, 1, 4), PartPose.offset(0, 1.3F, -4));
        for (int side : new int[]{-1, 1}) {
            head.addOrReplaceChild(side < 0 ? "right_ear" : "left_ear", CubeListBuilder.create()
                    .texOffs(64, 16).addBox(-1, -4, -1, 2, 4, 2)
                    .texOffs(72, 16).addBox(-0.5F, -3.5F, -1.15F, 1, 3, 1),
                    PartPose.offsetAndRotation(side * 2.4F, -2.5F, 0.5F, 0.1F, 0, side * 0.18F));
            head.addOrReplaceChild(side < 0 ? "right_cheek" : "left_cheek",
                    CubeListBuilder.create().texOffs(80, 16).addBox(-1, -1, -1.5F, 2, 2, 3),
                    PartPose.offsetAndRotation(side * 3.1F, 0.2F, 0.5F, 0.15F, side * 0.3F, 0));
            head.addOrReplaceChild(side < 0 ? "right_brow" : "left_brow",
                    CubeListBuilder.create().texOffs(90, 16).addBox(-0.5F, -0.5F, -1.5F, 1, 1, 3),
                    PartPose.offsetAndRotation(side * 3.3F, -1.8F, -2.2F, 0.15F, 0, side * 0.18F));
        }
        leg(root, "front_left", 2.5F, -4.5F);
        leg(root, "front_right", -2.5F, -4.5F);
        leg(root, "back_left", 2.5F, 6);
        leg(root, "back_right", -2.5F, 6);
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-1, -1, 0, 2, 2, 8),
                PartPose.offsetAndRotation(0, 13, 8, -0.35F, 0, 0));
        tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(20, 48)
                .addBox(-1, -1, 0, 2, 2, 6),
                PartPose.offsetAndRotation(0, 0, 7, 0.3F, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void leg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 36).addBox(-1, 0, -1, 2, 6, 2)
                .texOffs(8, 36).addBox(-1, 6, -2, 2, 2, 3),
                PartPose.offset(x, 16, z));
    }

    @Override
    public void setupAnim(ProwlerEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean stalking = entity.isAggressive();
        float movement = Mth.clamp(limbSwingAmount, 0, 1);
        head.yRot = Mth.clamp(netHeadYaw, -55, 55) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -25, 30) * Mth.DEG_TO_RAD - (stalking ? 0.08F : 0);
        body.y += stalking ? 0.65F : 0;
        head.y += stalking ? 1 : 0;
        body.xRot = stalking ? 0.04F : 0;
        jaw.xRot = stalking ? 0.23F : 0;
        float stride = Mth.cos(limbSwing * 0.85F) * movement * (stalking ? 1.0F : 0.8F);
        legs[0].xRot = stride;
        legs[1].xRot = -stride;
        legs[2].xRot = -stride;
        legs[3].xRot = stride;
        body.zRot = Mth.sin(limbSwing * 0.85F) * movement * 0.035F;
        head.y += Mth.cos(limbSwing * 1.7F) * movement * 0.12F;
        tail.xRot = stalking ? -0.52F : -0.35F;
        tail.yRot = Mth.sin(ageInTicks * 0.11F) * (stalking ? 0.12F : 0.26F);
        tailTip.yRot = Mth.sin(ageInTicks * 0.11F - 0.8F) * 0.22F;
        tailTip.xRot += Mth.sin(ageInTicks * 0.09F) * 0.08F;
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
