package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.SongbirdEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class SongbirdModel extends EntityModel<SongbirdEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart tail;

    SongbirdModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        leftWing = root.getChild("left_wing");
        rightWing = root.getChild("right_wing");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        tail = root.getChild("tail");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -1, -2.5F, 5, 3.5F, 6)
                .texOffs(64, 24).addBox(-2, -0.5F, -2.8F, 4, 3, 1),
                PartPose.offset(0, 19.5F, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-2, -2, -2, 4, 4, 4)
                .texOffs(48, 32).addBox(-1, 0, -4, 2, 1, 2)
                .texOffs(32, 48).addBox(-2.15F, -0.5F, -1.2F, 0.3F, 0.8F, 0.8F)
                .texOffs(32, 48).addBox(1.85F, -0.5F, -1.2F, 0.3F, 0.8F, 0.8F),
                PartPose.offset(0, 19, -2.5F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0, 0, -2, 5, 1, 5), PartPose.offset(2, 19, 0));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 32).mirror()
                .addBox(-5, 0, -2, 5, 1, 5), PartPose.offset(-2, 19, 0));
        for (int side : new int[]{-1, 1}) {
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create()
                    .texOffs(32, 32).addBox(-0.5F, 0, -0.5F, 1, 2, 1)
                    .addBox(-0.5F, 1.5F, -1.5F, 1, 0.5F, 2),
                    PartPose.offset(side, 22, 0.5F));
        }
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(64, 48)
                .addBox(-1.5F, 0, 0, 3, 1, 5),
                PartPose.offsetAndRotation(0, 21, 3, -0.25F, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(SongbirdEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(netHeadYaw, -60, 60) * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        boolean airborne = !entity.onGround();
        float flap = airborne ? Mth.sin(ageInTicks * 2.1F) * 0.85F + 0.15F
                : 1.05F + Mth.sin(ageInTicks * 0.09F) * 0.035F;
        leftWing.zRot = flap;
        rightWing.zRot = -flap;
        float stride = Mth.cos(limbSwing * 1.2F) * limbSwingAmount;
        leftLeg.xRot = airborne ? 0.65F : stride;
        rightLeg.xRot = airborne ? 0.65F : -stride;
        tail.xRot += Mth.sin(ageInTicks * 0.14F) * 0.07F;
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
