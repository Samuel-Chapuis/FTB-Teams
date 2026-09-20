package dev.ftb.mods.ftbteams.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MinionRenderer extends MobRenderer<MinionEntity, MinionModel> {
	private static final ResourceLocation SKIN = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

	public MinionRenderer(EntityRendererProvider.Context context) {
		super(context, new MinionModel(context.bakeLayer(ModelLayers.PLAYER)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(MinionEntity entity) {
		return SKIN;
	}

	@Override
	protected void scale(MinionEntity entity, PoseStack pose, float partialTick) {
		float scale = 0.9375F / MinionEntity.BODY_DIVISOR;
		pose.scale(scale, scale, scale);
	}
}
