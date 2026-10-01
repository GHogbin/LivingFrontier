package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.BoarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class BoarModel extends EntityModel<BoarEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart[] legs;

    BoarModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = root.getChild("head");
        tail = root.getChild("tail");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        legs = new ModelPart[]{root.getChild("front_left"), root.getChild("front_right"),
                root.getChild("back_left"), root.getChild("back_right")};
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -4, -8, 10, 8, 16)
                .texOffs(0, 24).addBox(-5.5F, -3, -7, 11, 6, 7)
                .texOffs(36, 24).addBox(-3, 3.4F, -6, 6, 1, 12),
                PartPose.offset(0, 15, 2));
        body.addOrReplaceChild("ridge", CubeListBuilder.create().texOffs(0, 50)
                .addBox(-1, -2, -5, 2, 2, 10), PartPose.offset(0, -4, 0));
        for (int index = 0; index < 4; index++) {
            body.addOrReplaceChild("bristle_" + index, CubeListBuilder.create().texOffs(24, 52)
                    .addBox(-0.5F, -3, -1, 1, 3, 2),
                    PartPose.offsetAndRotation(0, -5, -4 + index * 3, -0.22F, 0, 0));
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-4, -3, -4, 8, 7, 6)
                .texOffs(92, 0).addBox(-3, 0, -8, 6, 4, 5)
                .texOffs(92, 10).addBox(-3, 0.5F, -8.6F, 6, 3, 1)
                .texOffs(108, 12).addBox(-4.25F, -1.2F, -2.5F, 0.5F, 1, 1)
                .texOffs(108, 12).addBox(3.75F, -1.2F, -2.5F, 0.5F, 1, 1)
                .texOffs(112, 12).addBox(-4.35F, -1, -2.3F, 0.25F, 0.6F, 0.6F)
                .texOffs(112, 12).addBox(4.1F, -1, -2.3F, 0.25F, 0.6F, 0.6F),
                PartPose.offset(0, 15, -7));
        for (int side : new int[]{-1, 1}) {
            head.addOrReplaceChild(side < 0 ? "right_ear" : "left_ear", CubeListBuilder.create()
                    .texOffs(80, 24).addBox(-1.5F, -3, -0.5F, 3, 3, 1)
                    .texOffs(88, 24).addBox(-1, -2.6F, -0.65F, 2, 2, 1),
                    PartPose.offsetAndRotation(side * 3, -2.4F, 0.5F, 0.18F, 0, side * 0.42F));
            PartDefinition tusk = head.addOrReplaceChild(side < 0 ? "right_tusk" : "left_tusk",
                    CubeListBuilder.create().texOffs(72, 24).addBox(-0.5F, -4, -0.5F, 1, 4, 1),
                    PartPose.offsetAndRotation(side * 3.1F, 3, -5.8F, -0.1F, 0, side * 0.35F));
            tusk.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(72, 24)
                    .addBox(-0.4F, -2, -0.4F, 0.8F, 2, 0.8F),
                    PartPose.offsetAndRotation(0, -3.7F, 0, -0.5F, 0, side * 0.2F));
        }
        leg(root, "front_left", 3.5F, -4.5F);
        leg(root, "front_right", -3.5F, -4.5F);
        leg(root, "back_left", 3.5F, 7);
        leg(root, "back_right", -3.5F, 7);
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 40)
                .addBox(-0.5F, -0.5F, 0, 1, 1, 4),
                PartPose.offsetAndRotation(0, 13, 10, -0.6F, 0, 0));
        tail.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(34, 40)
                .addBox(-0.5F, -0.5F, 0, 1, 2, 2),
                PartPose.offsetAndRotation(0, 0, 3, -0.65F, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void leg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 40).addBox(-1.5F, 0, -1.5F, 3, 4, 3)
                .texOffs(12, 40).addBox(-1.5F, 4, -1.6F, 3, 2, 3),
                PartPose.offset(x, 18, z));
    }

    @Override
    public void setupAnim(BoarEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean aggressive = entity.isAggressive();
        float movement = Mth.clamp(limbSwingAmount, 0, 1);
        head.yRot = Mth.clamp(netHeadYaw, -40, 40) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -20, 30) * Mth.DEG_TO_RAD + (aggressive ? 0.22F : 0);
        float stride = Mth.cos(limbSwing * 0.8F) * movement * (aggressive ? 0.95F : 0.7F);
        legs[0].xRot = stride;
        legs[1].xRot = -stride;
        legs[2].xRot = -stride;
        legs[3].xRot = stride;
        body.zRot = Mth.sin(limbSwing * 0.8F) * movement * 0.025F;
        head.y += Mth.cos(limbSwing * 1.6F) * movement * 0.18F;
        leftEar.zRot += aggressive ? -0.22F : Mth.sin(ageInTicks * 0.08F) * 0.025F;
        rightEar.zRot -= aggressive ? -0.22F : Mth.sin(ageInTicks * 0.08F) * 0.025F;
        tail.yRot = Mth.sin(ageInTicks * 0.12F) * (aggressive ? 0.08F : 0.2F);
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
