package dev.ftb.mods.ftbteams.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MinionRenderer extends MobRenderer<MinionEntity, MinionModel> {
	private static final String[] SKINS = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};
	private static final Map<UUID, ResourceLocation> CUSTOM_TEXTURES = new ConcurrentHashMap<>();
	private static final Set<UUID> REQUESTED_TEXTURES = ConcurrentHashMap.newKeySet();

	public MinionRenderer(EntityRendererProvider.Context context) {
		super(context, new MinionModel(context.bakeLayer(ModelLayers.PLAYER)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(MinionEntity entity) {
		if (!entity.getCustomSkinUuid().isBlank()) {
			try {
				UUID id = UUID.fromString(entity.getCustomSkinUuid());
				var connection = Minecraft.getInstance().getConnection();
				if (connection != null && connection.getPlayerInfo(id) != null) {
					return connection.getPlayerInfo(id).getSkin().texture();
				}
				ResourceLocation texture = CUSTOM_TEXTURES.get(id);
				if (texture != null) return texture;
				if (REQUESTED_TEXTURES.add(id)) {
					Minecraft.getInstance().getSkinManager().getOrLoad(new GameProfile(id, entity.getCustomSkinId()))
							.thenAccept(skin -> CUSTOM_TEXTURES.put(id, skin.texture()));
				}
			} catch (IllegalArgumentException ignored) { }
		}
		return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/" + SKINS[Math.floorMod(entity.getSkinId(), SKINS.length)] + ".png");
	}

	@Override
	protected void scale(MinionEntity entity, PoseStack pose, float partialTick) {
		if (entity.isSittingOnBench()) {
			pose.translate(0, 0.3
				, 0);
		}
		float scale = 0.9375F / MinionEntity.BODY_DIVISOR;
		pose.scale(scale, scale, scale);
	}
}
