package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.RaiderEntity;
import dev.livingfrontier.entity.WarlordEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class RaiderModel<T extends RaiderEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    RaiderModel(ModelPart root) {
        this.root = root;
        torso = root.getChild("torso");
        head = torso.getChild("head");
        leftArm = torso.getChild("left_arm");
        rightArm = torso.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }

    static LayerDefinition createBodyLayer(boolean warlord) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, 0, -2, 8, 11, 4)
                .texOffs(64, 24).addBox(-4.2F, 10, -2.5F, 8.4F, 2, 5)
                .texOffs(64, 24).addBox(-1.5F, 5, -2.3F, 3, 9, 0.5F),
                PartPose.ZERO);
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.5F, -7, -3.5F, 7, 7, 7)
                .texOffs(96, 0).addBox(-4, -8, -4, 8, 2, 8)
                .texOffs(96, 0).addBox(-4, -6, -3.5F, 1, 6, 7)
                .texOffs(96, 0).addBox(3, -6, -3.5F, 1, 6, 7)
                .texOffs(96, 0).addBox(-4, -6, 2.5F, 8, 6, 1)
                .texOffs(96, 0).addBox(-3.5F, -2.5F, -3.7F, 7, 2.5F, 1)
                .texOffs(112, 48).addBox(-2.5F, -4.8F, -3.7F, 1.5F, 0.8F, 0.4F)
                .texOffs(112, 48).addBox(1, -4.8F, -3.7F, 1.5F, 0.8F, 0.4F),
                PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            torso.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create()
                    .texOffs(0, 32).addBox(-1.5F, -2, -2, 3, 12, 4)
                    .texOffs(0, 0).addBox(-2.5F, -2.5F, -3, 5, 3, 6)
                    .texOffs(32, 32).addBox(-1.6F, 7, -2.1F, 3.2F, 3, 4.2F),
                    PartPose.offset(side * 5.5F, 2, 0));
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create()
                    .texOffs(0, 32).addBox(-1.5F, 0, -2, 3, 12, 4)
                    .texOffs(32, 32).addBox(-1.6F, 9, -2.3F, 3.2F, 3, 4.4F),
                    PartPose.offset(side * 2, 12, 0));
        }
        CubeListBuilder weapon = CubeListBuilder.create().texOffs(48, 32)
                .addBox(-0.5F, -9, -0.5F, 1, 16, 1);
        if (warlord) {
            weapon.texOffs(64, 48).addBox(-4, -10, -2, 8, 5, 4)
                    .texOffs(96, 24).addBox(-1, -10.2F, -2.2F, 2, 5.4F, 4.4F);
            head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(96, 24)
                    .addBox(-4, -8, -4, 8, 1.5F, 8, new CubeDeformation(0.2F))
                    .texOffs(16, 32).addBox(-0.6F, -10.5F, -4, 1.2F, 3, 1),
                    PartPose.ZERO);
            for (int side : new int[]{-1, 1}) {
                PartDefinition horn = head.addOrReplaceChild(side < 0 ? "right_horn" : "left_horn",
                        CubeListBuilder.create().texOffs(16, 32).addBox(-1, -5, -1, 2, 5, 2),
                        PartPose.offsetAndRotation(side * 3.7F, -7, 0, -0.2F, 0, side * 0.65F));
                horn.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(16, 32)
                        .addBox(-0.5F, -3, -0.5F, 1, 3, 1),
                        PartPose.offsetAndRotation(0, -4.5F, 0, 0, 0, -side * 0.4F));
            }
        } else {
            weapon.texOffs(64, 48).addBox(-4, -9, -1, 7, 3, 2)
                    .addBox(-5, -8, -1, 2, 4, 2);
        }
        torso.getChild("right_arm").addOrReplaceChild("weapon", weapon,
                PartPose.offsetAndRotation(0, 9, -2.3F, -0.15F, 0, -0.12F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        attackTime = entity.getAttackAnim(partialTick);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float stride = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 1.25F;
        leftLeg.xRot = stride;
        rightLeg.xRot = -stride;
        leftArm.xRot = -stride * 0.65F;
        rightArm.xRot = stride * 0.35F - (entity.isAggressive() ? 0.6F : 0.2F);
        leftArm.zRot = 0.07F;
        rightArm.zRot = -0.07F;
        float swing = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
        rightArm.xRot -= swing * 2.0F;
        rightArm.yRot = -swing * 0.25F;
        torso.yRot = -swing * 0.12F;
        if (entity instanceof WarlordEntity warlord && warlord.isCharging()) {
            torso.xRot = 0.13F;
            head.xRot = 0.18F;
            rightArm.xRot = -2.65F + Mth.sin(ageInTicks * 0.45F) * 0.04F;
            rightArm.yRot = -0.25F;
            leftArm.xRot = -2.25F;
            leftArm.yRot = 0.35F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
