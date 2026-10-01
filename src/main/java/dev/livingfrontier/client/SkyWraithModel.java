package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.SkyWraithEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class SkyWraithModel extends EntityModel<SkyWraithEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftOuterWing;
    private final ModelPart rightOuterWing;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;

    SkyWraithModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("tail");
        leftWing = body.getChild("left_wing");
        rightWing = body.getChild("right_wing");
        leftOuterWing = leftWing.getChild("outer");
        rightOuterWing = rightWing.getChild("outer");
        leftFoot = body.getChild("left_foot");
        rightFoot = body.getChild("right_foot");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -4, -2, 6, 7, 5)
                .texOffs(24, 0).addBox(-2, -3.4F, -2.4F, 4, 6, 1),
                PartPose.offset(0, 13, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-2.5F, -3, -3, 5, 5, 5)
                .texOffs(60, 0).addBox(-1.5F, 0.1F, -4.2F, 3, 2, 2)
                .texOffs(80, 0).addBox(-2, -1.4F, -3.25F, 1.3F, 1, 0.5F)
                .texOffs(80, 0).addBox(0.7F, -1.4F, -3.25F, 1.3F, 1, 0.5F)
                .texOffs(96, 0).addBox(-1, 0.8F, -4.35F, 2, 1, 0.5F)
                .texOffs(88, 0).addBox(-1.3F, 1.7F, -4, 0.7F, 1.5F, 0.7F)
                .texOffs(88, 0).addBox(0.6F, 1.7F, -4, 0.7F, 1.5F, 0.7F),
                PartPose.offset(0, -4.5F, -1.5F));
        for (int side : new int[]{-1, 1}) {
            head.addOrReplaceChild(side < 0 ? "right_horn" : "left_horn",
                    CubeListBuilder.create().texOffs(70, 0).addBox(-0.5F, -4, -1, 1, 4, 2),
                    PartPose.offsetAndRotation(side * 1.7F, -2.2F, 0.5F, 0.15F, 0, side * 0.35F));
            foot(body, side);
            wing(body, side);
        }
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(68, 16)
                .addBox(-1, -0.5F, 0, 2, 1, 6),
                PartPose.offsetAndRotation(0, 2, 3, -0.25F, 0, 0));
        tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(88, 16)
                .addBox(-0.5F, -1, 0, 1, 2, 3), PartPose.offset(0, 0, 5));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void foot(PartDefinition body, int side) {
        body.addOrReplaceChild(side < 0 ? "right_foot" : "left_foot", CubeListBuilder.create()
                .texOffs(44, 32).addBox(-0.5F, 0, -0.5F, 1, 4, 1)
                .texOffs(48, 32).addBox(-1.2F, 3.2F, -2, 1, 1, 2)
                .texOffs(48, 32).addBox(0.2F, 3.2F, -2, 1, 1, 2),
                PartPose.offsetAndRotation(side * 1.5F, 2.4F, 1.5F, 0.6F, 0, side * 0.12F));
    }

    private static void wing(PartDefinition body, int side) {
        boolean right = side < 0;
        PartDefinition wing = body.addOrReplaceChild(right ? "right_wing" : "left_wing",
                CubeListBuilder.create().mirror(right)
                        .texOffs(0, 16).addBox(right ? -6 : 0, 0, -0.5F, 6, 0.5F, 7)
                        .texOffs(0, 32).addBox(right ? -6 : 0, -0.5F, -1, 6, 1, 2),
                PartPose.offset(side * 2.8F, -2, 0));
        PartDefinition outer = wing.addOrReplaceChild("outer", CubeListBuilder.create().mirror(right)
                .texOffs(28, 16).addBox(right ? -7 : 0, 0, 0, 7, 0.5F, 5)
                .texOffs(16, 32).addBox(right ? -7 : 0, -0.5F, -0.5F, 7, 1, 1)
                .texOffs(40, 32).addBox(-0.5F, -3, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(side * 5.8F, 0, 0, 0, -side * 0.12F, 0));
        outer.addOrReplaceChild("tip", CubeListBuilder.create().mirror(right)
                .texOffs(54, 16).addBox(right ? -3 : 0, 0, 0, 3, 0.5F, 3)
                .texOffs(32, 32).addBox(right ? -3 : 0, -0.5F, -0.5F, 3, 1, 1),
                PartPose.offsetAndRotation(side * 6.8F, 0, 0, 0, -side * 0.24F, 0));
    }

    @Override
    public void setupAnim(SkyWraithEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean diving = entity.isDiving();
        float flap = Mth.sin(ageInTicks * (diving ? 1.05F : 0.75F)) * (diving ? 0.12F : 0.55F);
        body.y += Mth.sin(ageInTicks * 0.17F) * (diving ? 0.12F : 0.45F);
        body.xRot = diving ? 0.8F : -0.1F + Mth.sin(ageInTicks * 0.17F) * 0.025F;
        head.yRot = Mth.clamp(netHeadYaw, -40, 40) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -25, 25) * Mth.DEG_TO_RAD + (diving ? -0.35F : 0);
        leftWing.zRot = (diving ? -0.3F : -0.15F) + flap;
        rightWing.zRot = -leftWing.zRot;
        leftWing.yRot = diving ? -0.7F : -0.08F;
        rightWing.yRot = -leftWing.yRot;
        leftOuterWing.yRot = diving ? -0.9F : -0.12F;
        rightOuterWing.yRot = -leftOuterWing.yRot;
        leftOuterWing.zRot = diving ? -0.15F : Mth.sin(ageInTicks * 0.75F - 0.55F) * 0.28F;
        rightOuterWing.zRot = -leftOuterWing.zRot;
        leftFoot.xRot += diving ? 0.5F : Mth.sin(ageInTicks * 0.17F) * 0.08F;
        rightFoot.xRot = leftFoot.xRot;
        tail.xRot += diving ? 0.4F : Mth.sin(ageInTicks * 0.17F - 0.8F) * 0.12F;
        tail.yRot = Mth.sin(ageInTicks * 0.09F) * 0.12F;
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
