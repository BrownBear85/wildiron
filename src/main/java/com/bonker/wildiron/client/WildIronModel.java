package com.bonker.wildiron.client;

import com.bonker.wildiron.WildIron;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class WildIronModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(WildIron.MODID, "wild_iron"), "main");

    private final ModelPart base;

    public WildIronModel(ModelPart root) {
        super(RenderType::entityTranslucentCull);
        this.base = root.getChild("base");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, -2.0F, 1.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(20, 14).addBox(-2.0F, -2.0F, 8.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(12, 14).addBox(-1.5F, -1.5F, 8.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-1.5F, -2.5F, 1.5F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(0, 25).addBox(-2.0F, -2.0F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 15.5F, -5.75F));

        PartDefinition hammer_r1 = base.addOrReplaceChild("hammer_r1", CubeListBuilder.create().texOffs(0, -2).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -1.3F, -0.9F, -0.3927F, 0.0F, 0.0F));

        PartDefinition guard_r1 = base.addOrReplaceChild("guard_r1", CubeListBuilder.create().texOffs(15, 1).addBox(-1.0F, -2.0F, 0.0F, 1.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 1.75F, 0.75F, 0.3927F, 0.0F, 0.0F));

        PartDefinition grip_r1 = base.addOrReplaceChild("grip_r1", CubeListBuilder.create().texOffs(19, 21).addBox(-2.0F, -6.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.1F, -2.35F, -0.3927F, 0.0F, 0.0F));

        PartDefinition trigger_r1 = base.addOrReplaceChild("trigger_r1", CubeListBuilder.create().texOffs(1, 2).addBox(-2.0F, -3.0F, 0.1F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 0.75F, 0.2F, -0.3927F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
