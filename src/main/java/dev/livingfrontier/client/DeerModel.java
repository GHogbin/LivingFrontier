package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.DeerEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class DeerModel extends EntityModel<DeerEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart antlers;
    private final ModelPart tail;
    private final ModelPart[] legs;

    DeerModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        antlers = head.getChild("antlers");
        tail = root.getChild("tail");
        legs = new ModelPart[]{root.getChild("front_left"), root.getChild("front_right"),
                root.getChild("back_left"), root.getChild("back_right")};
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, 0, -8, 8, 7, 16)
                .texOffs(64, 24).addBox(-3, 6, -6, 6, 1.1F, 13), PartPose.offset(0, 10, 0));
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -6, -2, 4, 8, 4),
                PartPose.offsetAndRotation(0, 10, -6, -0.28F, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-2.5F, -3, -4, 5, 5, 7)
                .texOffs(96, 0).addBox(-1.5F, -0.5F, -7, 3, 3, 4)
                .texOffs(32, 48).addBox(-1.5F, 0, -7.1F, 3, 1.5F, 1)
                .texOffs(32, 32).addBox(-2.7F, -1.5F, -2.5F, 0.4F, 1, 1)
                .texOffs(32, 32).addBox(2.3F, -1.5F, -2.5F, 0.4F, 1, 1),
                PartPose.offset(0, 5, -9));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(48, 48)
                .addBox(0, -0.5F, -1, 3, 1, 2),
                PartPose.offsetAndRotation(2.2F, -2.5F, 1, 0, -0.25F, -0.4F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(48, 48)
                .addBox(-3, -0.5F, -1, 3, 1, 2),
                PartPose.offsetAndRotation(-2.2F, -2.5F, 1, 0, 0.25F, 0.4F));
        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            PartDefinition antler = antlers.addOrReplaceChild(side < 0 ? "right" : "left",
                    CubeListBuilder.create().texOffs(16, 32).addBox(-0.5F, -6, -0.5F, 1, 6, 1),
                    PartPose.offsetAndRotation(side * 1.8F, -2.8F, 1, 0.18F, 0, side * 0.25F));
            antler.addOrReplaceChild("fork", CubeListBuilder.create().texOffs(16, 32)
                    .addBox(side < 0 ? -3.5F : 0, -0.5F, -0.5F, 3.5F, 1, 1)
                    .addBox(side < 0 ? -3.5F : 2.5F, -3, -0.5F, 1, 3, 1),
                    PartPose.offset(0, -3, 0));
            antler.addOrReplaceChild("front_tine", CubeListBuilder.create().texOffs(16, 32)
                    .addBox(-0.5F, -2, -2.5F, 1, 2, 3),
                    PartPose.offsetAndRotation(0, -4.5F, 0, -0.3F, 0, 0));
        }
        leg(root, "front_left", 2.8F, -5.5F);
        leg(root, "front_right", -2.8F, -5.5F);
        leg(root, "back_left", 2.8F, 5.5F);
        leg(root, "back_right", -2.8F, 5.5F);
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(64, 48)
                .addBox(-1, 0, 0, 2, 3, 5),
                PartPose.offsetAndRotation(0, 11, 7, -0.65F, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void leg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1, 0, -1, 2, 7, 2)
                .texOffs(32, 32).addBox(-1, 7, -1.3F, 2, 2, 2.3F), PartPose.offset(x, 15, z));
    }

    @Override
    public void setupAnim(DeerEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(netHeadYaw, -50, 50) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -25, 35) * Mth.DEG_TO_RAD;
        antlers.visible = !entity.isBaby();
        float stride = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 1.15F;
        legs[0].xRot = stride;
        legs[1].xRot = -stride;
        legs[2].xRot = -stride;
        legs[3].xRot = stride;
        tail.yRot = Mth.sin(ageInTicks * 0.12F) * 0.16F;
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
