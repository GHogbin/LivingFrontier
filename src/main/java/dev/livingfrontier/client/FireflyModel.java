package dev.livingfrontier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livingfrontier.entity.FireflyEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

final class FireflyModel extends EntityModel<FireflyEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart lantern;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    FireflyModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        lantern = body.getChild("lantern");
        leftWing = body.getChild("left_wing");
        rightWing = body.getChild("right_wing");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1, -1, -1.5F, 2, 2, 3)
                .texOffs(64, 0).addBox(-1, -0.7F, -3, 2, 1.5F, 2)
                .texOffs(48, 32).addBox(-1.2F, -0.4F, -2.7F, 0.4F, 0.6F, 0.6F)
                .texOffs(48, 32).addBox(0.8F, -0.4F, -2.7F, 0.4F, 0.6F, 0.6F)
                .texOffs(32, 32).addBox(-0.8F, -1.8F, -3, 0.4F, 1.4F, 0.4F)
                .texOffs(32, 32).addBox(0.4F, -1.8F, -3, 0.4F, 1.4F, 0.4F),
                PartPose.offset(0, 22.3F, 0));
        body.addOrReplaceChild("lantern", CubeListBuilder.create().texOffs(64, 24)
                .addBox(-1.1F, -0.9F, 0, 2.2F, 1.8F, 3), PartPose.offset(0, 0, 1));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0, 0, -1.5F, 4, 0.5F, 4), PartPose.offset(0.8F, -0.8F, 0));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 32).mirror()
                .addBox(-4, 0, -1.5F, 4, 0.5F, 4), PartPose.offset(-0.8F, -0.8F, 0));
        for (int side : new int[]{-1, 1}) {
            body.addOrReplaceChild(side < 0 ? "right_legs" : "left_legs", CubeListBuilder.create()
                    .texOffs(32, 32).addBox(0, 0, -1, 1.5F, 0.35F, 0.35F)
                    .addBox(0, 0, 0.5F, 1.5F, 0.35F, 0.35F)
                    .addBox(0, 0, 2, 1.5F, 0.35F, 0.35F),
                    PartPose.offsetAndRotation(side * 0.7F, 0.5F, 0, 0, 0,
                            side < 0 ? 2.7F : 0.45F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(FireflyEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float flap = Mth.sin(ageInTicks * 3.5F) * 0.65F + 0.15F;
        leftWing.zRot = flap;
        rightWing.zRot = -flap;
        body.y += Mth.sin(ageInTicks * 0.22F) * 0.22F;
        body.xRot = Mth.clamp(headPitch, -25, 25) * Mth.DEG_TO_RAD;
        float pulse = entity.isNight() ? 1.0F + Mth.sin(ageInTicks * 0.16F) * 0.08F : 0.88F;
        lantern.xScale = pulse;
        lantern.yScale = pulse;
        lantern.zScale = pulse;
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poses, vertices, light, overlay, red, green, blue, alpha);
    }
}
