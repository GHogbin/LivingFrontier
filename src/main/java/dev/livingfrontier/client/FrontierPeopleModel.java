package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.ArmedGuard;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;

final class FrontierPeopleModel<T extends PathfinderMob> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart cloak;
    private final ModelPart satchel;
    private final ModelPart sword;
    private final ModelPart shield;
    private final ModelPart bow;
    private final boolean guard;
    private float partialTick;

    FrontierPeopleModel(ModelPart root, boolean guard) {
        this.root = root;
        this.guard = guard;
        torso = root.getChild("torso");
        head = torso.getChild("head");
        rightArm = torso.getChild("right_arm");
        leftArm = torso.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        cloak = guard ? null : torso.getChild("cloak");
        satchel = guard ? null : torso.getChild("satchel");
        sword = guard ? rightArm.getChild("sword") : null;
        shield = guard ? leftArm.getChild("shield") : null;
        bow = guard ? rightArm.getChild("bow") : null;
    }

    static LayerDefinition createBodyLayer(boolean guard) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4, 0, -2, 8, 12, 4)
                .texOffs(96, 16).addBox(-4, 9, -2, 8, 2, 4, new CubeDeformation(0.15F))
                .texOffs(120, 16).addBox(-1, 9, -2.8F, 2, 2, 1)
                .texOffs(112, 0).addBox(-3, 0, -2.3F, 6, 2, 1), PartPose.ZERO);
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -7, -3.5F, 7, 7, 7)
                .texOffs(28, 0).addBox(-3.5F, -7, -3.65F, 7, 2, 1)
                .texOffs(28, 0).addBox(-3.5F, -7, 2.75F, 7, 5, 1)
                .texOffs(44, 0).addBox(-3.65F, -6, -2.5F, 1, 4, 6)
                .texOffs(44, 0).addBox(2.65F, -6, -2.5F, 1, 4, 6)
                .texOffs(60, 0).addBox(-1, -3.5F, -4.2F, 2, 2, 1)
                .texOffs(68, 0).addBox(-4, -3.5F, -0.5F, 1, 2, 1)
                .texOffs(68, 0).addBox(3, -3.5F, -0.5F, 1, 2, 1), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            PartDefinition arm = torso.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm",
                    CubeListBuilder.create().texOffs(32, 16).addBox(-1.5F, -2, -2, 3, 12, 4)
                            .texOffs(82, 16).addBox(-1.5F, 7, -2, 3, 3, 4,
                                    new CubeDeformation(0.04F)),
                    PartPose.offset(side * 5.5F, 2, 0));
            if (guard) {
                arm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(62, 34)
                        .addBox(-2, -2.3F, -2.5F, 4, 3, 5), PartPose.ZERO);
            }
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create()
                    .texOffs(48, 16).addBox(-1.5F, 0, -2, 3, 12, 4)
                    .texOffs(64, 16).addBox(-1.5F, 8, -2.6F, 3, 4, 5,
                            new CubeDeformation(0.06F)), PartPose.offset(side * 2, 12, 0));
        }
        if (guard) {
            head.addOrReplaceChild("helmet", CubeListBuilder.create()
                    .texOffs(80, 0).addBox(-4, -7.2F, -4, 8, 2, 8)
                    .texOffs(80, 11).addBox(-4, -5.2F, -4.2F, 8, 1, 2)
                    .texOffs(24, 34).addBox(-4, -5.2F, -3, 1, 5, 7)
                    .texOffs(24, 34).addBox(3, -5.2F, -3, 1, 5, 7)
                    .texOffs(42, 34).addBox(-4, -5.2F, 3.25F, 8, 5, 1), PartPose.ZERO);
            torso.addOrReplaceChild("tabard", CubeListBuilder.create().texOffs(0, 34)
                    .addBox(-3, 1, -2.65F, 6, 12, 1)
                    .addBox(-3, 1, 2.15F, 6, 12, 1), PartPose.ZERO);
            torso.getChild("right_arm").addOrReplaceChild("sword", CubeListBuilder.create()
                    .texOffs(94, 34).addBox(-1, -0.5F, -14, 2, 1, 11)
                    .texOffs(110, 48).addBox(-0.5F, -0.5F, -16, 1, 1, 2)
                    .texOffs(94, 48).addBox(-0.5F, -0.5F, -2, 1, 1, 4)
                    .texOffs(94, 56).addBox(-2.5F, -0.5F, -3, 5, 1, 1)
                    .texOffs(110, 54).addBox(-1, -1, 1.5F, 2, 2, 2),
                    PartPose.offset(0, 9, -2.2F));
            torso.getChild("left_arm").addOrReplaceChild("shield", CubeListBuilder.create()
                    .texOffs(70, 44).addBox(-3, -4, -1, 6, 8, 1)
                    .texOffs(70, 54).addBox(-2, 4, -1, 4, 2, 1)
                    .texOffs(84, 48).addBox(-3, -4, -1.35F, 1, 8, 1)
                    .texOffs(84, 48).addBox(2, -4, -1.35F, 1, 8, 1)
                    .texOffs(70, 58).addBox(-3, -4, -1.35F, 6, 1, 1)
                    .texOffs(118, 48).addBox(-2, 4, -1.35F, 1, 2, 1)
                    .texOffs(118, 48).addBox(1, 4, -1.35F, 1, 2, 1)
                    .texOffs(84, 58).addBox(-1, -1, -1.7F, 2, 2, 1),
                    PartPose.offsetAndRotation(0, 7, -2.6F, 0, -0.15F, 0));
            BowGeometry.add(torso.getChild("right_arm"), 96, 16, 0, 35);
        } else {
            head.addOrReplaceChild("cap", CubeListBuilder.create()
                    .texOffs(80, 0).addBox(-4, -7.2F, -4, 8, 2, 8)
                    .texOffs(80, 11).addBox(-4, -5.2F, -5, 8, 1, 2), PartPose.ZERO);
            torso.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 34)
                    .addBox(-4.5F, 0, 0, 9, 13, 1),
                    PartPose.offsetAndRotation(0, 0, 2.6F, 0.12F, 0, 0));
            torso.addOrReplaceChild("backpack", CubeListBuilder.create()
                    .texOffs(24, 34).addBox(-3, 2, 3.8F, 6, 8, 4)
                    .texOffs(46, 34).addBox(-3, 2, 3.6F, 6, 2, 5)
                    .texOffs(24, 48).addBox(-3, -1, 4, 6, 3, 3), PartPose.ZERO);
            torso.addOrReplaceChild("straps", CubeListBuilder.create().texOffs(88, 34)
                    .addBox(-3, 0, -2.4F, 1, 12, 1)
                    .addBox(2, 0, -2.4F, 1, 12, 1), PartPose.ZERO);
            torso.addOrReplaceChild("satchel_strap", CubeListBuilder.create().texOffs(88, 34)
                    .addBox(-0.5F, 0, 0, 1, 12, 1),
                    PartPose.offsetAndRotation(-1.5F, 0, -2.55F, 0, 0, -0.3F));
            torso.addOrReplaceChild("satchel", CubeListBuilder.create()
                    .texOffs(70, 34).addBox(-2, 0, 0, 4, 5, 3)
                    .texOffs(70, 44).addBox(-2, 0, -0.2F, 4, 2, 4),
                    PartPose.offsetAndRotation(4.5F, 8, 2.3F, 0, -0.1F, 0));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        this.partialTick = partialTick;
        attackTime = entity.getAttackAnim(partialTick);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(part -> {
            part.resetPose();
            part.visible = true;
        });
        head.yRot = Mth.clamp(netHeadYaw, -75, 75) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -40, 45) * Mth.DEG_TO_RAD;
        float movement = Mth.clamp(limbSwingAmount, 0, 1);
        float stride = Mth.cos(limbSwing * 0.6662F) * movement * 1.2F;
        rightLeg.xRot = stride;
        leftLeg.xRot = -stride;
        rightArm.xRot = -stride * 0.65F;
        leftArm.xRot = stride * 0.65F;
        float idle = Mth.sin(ageInTicks * 0.08F) * 0.025F;
        rightArm.zRot = -0.04F - idle;
        leftArm.zRot = 0.04F + idle;
        if (guard) {
            boolean holdingBow = entity instanceof ArmedGuard armed && armed.isHoldingBow();
            BowGeometry.setHoldingBow(bow, holdingBow, sword, shield);
            if (holdingBow) {
                boolean drawing = entity.isUsingItem();
                BowGeometry.pose(rightArm, leftArm, bow, head.yRot, head.xRot,
                        drawing || entity.isAggressive(),
                        BowGeometry.drawProgress(drawing, entity.getTicksUsingItem(), partialTick));
            } else {
                float swing = Mth.sin(Mth.sqrt(Mth.clamp(attackTime, 0, 1)) * Mth.PI);
                boolean ready = entity.isAggressive() || attackTime > 0;
                rightArm.xRot = -0.15F - stride * 0.25F - (ready ? 0.55F : 0) - swing * 1.8F;
                rightArm.yRot = -swing * 0.35F;
                leftArm.xRot = ready ? -1.05F : stride * 0.3F - 0.12F;
                leftArm.yRot = ready ? 0.45F : 0;
                leftArm.zRot = ready ? -0.18F : leftArm.zRot;
                torso.yRot = -swing * 0.18F;
            }
        } else {
            cloak.xRot += movement * 0.18F + Mth.sin(ageInTicks * 0.09F) * 0.025F;
            cloak.zRot = Mth.sin(limbSwing * 0.6662F) * movement * 0.035F;
            satchel.xRot = stride * 0.12F;
            satchel.zRot = Mth.sin(limbSwing * 0.6662F) * movement * 0.06F;
        }
        head.yRot -= torso.yRot;
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
