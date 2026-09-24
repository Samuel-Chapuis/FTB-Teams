package dev.ftb.mods.ftbteams.client.gui.enclosure;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ftb.mods.ftbteams.world.block.enclosure.EnclosurePreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static dev.ftb.mods.ftbteams.client.gui.enclosure.PreviewGeometry.Vertex;

/** Cached textured block geometry, sliced by a camera-facing plane before drawing into the GUI. */
public final class EnclosurePreviewRenderer {
	private static final RenderType TYPE = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
	private static final int MAX_FACES = 120_000;
	private EnclosurePreview snapshot;
	private BakedModel resourceMarker;
	private final List<List<Vertex>> mesh = new ArrayList<>();
	private final List<List<Vertex>> visible = new ArrayList<>();
	private float centerX, centerY, centerZ, viewWidth = 1, viewHeight = 1;
	private float yaw, pitch, cut, zoom, panX, panY;
	private boolean dirty = true;
	private boolean simplified;
	private boolean truncated;

	public EnclosurePreviewRenderer() {
		reset();
	}

	public void reset() {
		yaw = (float) Math.toRadians(-35);
		pitch = (float) Math.toRadians(28);
		cut = 0.38F;
		zoom = 1;
		panX = panY = 0;
		dirty = true;
	}

	public void rotate(double dx, double dy) {
		yaw = (float) ((yaw + dx * 0.012) % (Math.PI * 2));
		pitch = Math.clamp((float) (pitch + dy * 0.012), (float) Math.toRadians(-75), (float) Math.toRadians(85));
		dirty = true;
	}

	public void pan(double dx, double dy) {
		panX = Math.clamp(panX + (float) dx, -150, 150);
		panY = Math.clamp(panY + (float) dy, -100, 100);
	}

	public void zoom(double delta) {
		zoom = Math.clamp(zoom * (float) Math.pow(1.15, delta), 0.35F, 6);
	}

	public float getCut() {
		return cut;
	}

	public void setCut(float value) {
		cut = Math.clamp(value, 0, 1);
		dirty = true;
	}

	public boolean isSimplified() {
		return simplified;
	}

	public boolean isTruncated() {
		return truncated;
	}

	public void render(GuiGraphics graphics, EnclosurePreview preview, BlockPos origin, int x, int y, int width, int height) {
		Minecraft minecraft = Minecraft.getInstance();
		BakedModel marker = minecraft.getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState());
		if (preview != snapshot || resourceMarker != marker) {
			snapshot = preview;
			resourceMarker = marker;
			buildMesh(minecraft, origin);
		}
		if (mesh.isEmpty()) return;
		if (dirty) updateCut();
		float scale = Math.min((width - 8) / viewWidth, (height - 8) / viewHeight) * 0.92F * zoom;
		graphics.flush();
		graphics.enableScissor(x, y, x + width, y + height);
		graphics.pose().pushPose();
		try {
			graphics.pose().translate(x + width / 2F + panX, y + height / 2F + panY, 200);
			graphics.pose().scale(scale, -scale, 1);
			Lighting.setupForFlatItems();
			RenderSystem.enableDepthTest();
			VertexConsumer buffer = graphics.bufferSource().getBuffer(TYPE);
			Matrix4f pose = graphics.pose().last().pose();
			for (List<Vertex> face : visible) {
				// Clipped quads can become pentagons. Emit a triangle fan as degenerate quads.
				for (int i = 1; i < face.size() - 1; i++) {
					emit(buffer, pose, face.getFirst());
					emit(buffer, pose, face.get(i));
					emit(buffer, pose, face.get(i + 1));
					emit(buffer, pose, face.get(i + 1));
				}
			}
			// GuiGraphics.flush disables depth testing: end this batch directly to preserve 3D occlusion.
			graphics.bufferSource().endBatch(TYPE);
		} finally {
			graphics.pose().popPose();
			graphics.disableScissor();
			Lighting.setupFor3DItems();
			RenderSystem.enableDepthTest();
		}
	}

	private static void emit(VertexConsumer buffer, Matrix4f pose, Vertex vertex) {
		buffer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
				.setColor(vertex.red(), vertex.green(), vertex.blue(), vertex.alpha()).setUv(vertex.u(), vertex.v())
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
	}

	private void updateCut() {
		visible.clear();
		float nearest = -Float.MAX_VALUE, furthest = Float.MAX_VALUE;
		float extentX = 0.5F, extentY = 0.5F;
		List<List<Vertex>> rotated = new ArrayList<>(mesh.size());
		for (List<Vertex> face : mesh) {
			List<Vertex> transformed = new ArrayList<>(4);
			for (Vertex vertex : face) {
				Vertex view = PreviewGeometry.rotate(vertex, yaw, pitch);
				transformed.add(view);
				nearest = Math.max(nearest, view.z());
				furthest = Math.min(furthest, view.z());
				extentX = Math.max(extentX, Math.abs(view.x()));
				extentY = Math.max(extentY, Math.abs(view.y()));
			}
			rotated.add(transformed);
		}
		float plane = nearest - (nearest - furthest) * cut;
		viewWidth = extentX * 2;
		viewHeight = extentY * 2;
		for (List<Vertex> face : rotated) {
			List<Vertex> clipped = PreviewGeometry.clip(face, plane);
			if (!clipped.isEmpty()) visible.add(clipped);
		}
		dirty = false;
	}

	private void buildMesh(Minecraft minecraft, BlockPos origin) {
		mesh.clear();
		visible.clear();
		simplified = truncated = false;
		dirty = true;
		if (snapshot.blocks().isEmpty()) return;
		int minX = 32, minY = 32, minZ = 32, maxX = -32, maxY = -32, maxZ = -32;
		for (var entry : snapshot.blocks()) {
			BlockPos p = entry.offset();
			minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
			maxX = Math.max(maxX, p.getX() + 1); maxY = Math.max(maxY, p.getY() + 1); maxZ = Math.max(maxZ, p.getZ() + 1);
		}
		centerX = (minX + maxX) / 2F; centerY = (minY + maxY) / 2F; centerZ = (minZ + maxZ) / 2F;
		RandomSource random = RandomSource.create();
		for (var entry : snapshot.blocks()) {
			if (mesh.size() >= MAX_FACES) { truncated = true; break; }
			var state = entry.state();
			BlockPos worldPos = origin.offset(entry.offset());
			BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
			if (state.getRenderShape() == RenderShape.MODEL) {
				long seed = state.getSeed(worldPos);
				random.setSeed(seed);
				for (BakedQuad quad : model.getQuads(state, null, random)) addQuad(minecraft, entry, worldPos, quad);
				for (Direction face : Direction.values()) {
					random.setSeed(seed);
					for (BakedQuad quad : model.getQuads(state, face, random)) addQuad(minecraft, entry, worldPos, quad);
				}
			} else if (minecraft.level != null && (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED || !state.getFluidState().isEmpty())) {
				// Snapshot contains states only: special renderers (e.g. chests) use their shape and particle texture.
				simplified = true;
				TextureAtlasSprite sprite = model.getParticleIcon();
				List<AABB> boxes = state.getShape(minecraft.level, worldPos).toAabbs();
				if (!state.getFluidState().isEmpty()) {
					boolean lava = state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
					sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace(lava ? "block/lava_still" : "block/water_still"));
					boxes = List.of(new AABB(0, 0, 0, 1, 0.875, 1));
				}
				for (AABB box : boxes) addBox(entry.offset(), box, sprite);
			}
		}
	}

	private void addQuad(Minecraft minecraft, EnclosurePreview.Entry entry, BlockPos worldPos, BakedQuad quad) {
		if (mesh.size() >= MAX_FACES) { truncated = true; return; }
		int tint = quad.isTinted() ? minecraft.getBlockColors().getColor(entry.state(), minecraft.level, worldPos, quad.getTintIndex()) : 0xFFFFFF;
		float shade = shade(quad.getDirection());
		int[] data = quad.getVertices();
		int stride = data.length / 4;
		List<Vertex> face = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) {
			int offset = i * stride, color = data[offset + 3];
			face.add(vertex(entry.offset(), Float.intBitsToFloat(data[offset]), Float.intBitsToFloat(data[offset + 1]), Float.intBitsToFloat(data[offset + 2]),
					Float.intBitsToFloat(data[offset + 4]), Float.intBitsToFloat(data[offset + 5]),
					(color & 255) / 255F * ((tint >> 16) & 255) / 255F * shade,
					((color >> 8) & 255) / 255F * ((tint >> 8) & 255) / 255F * shade,
					((color >> 16) & 255) / 255F * (tint & 255) / 255F * shade));
		}
		mesh.add(face);
	}

	private Vertex vertex(BlockPos offset, float x, float y, float z, float u, float v, float r, float g, float b) {
		return new Vertex(offset.getX() + x - centerX, offset.getY() + y - centerY, offset.getZ() + z - centerZ, u, v, r, g, b, 1);
	}

	private void addBox(BlockPos offset, AABB box, TextureAtlasSprite sprite) {
		float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
		float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
		float[][][] faces = {
				{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}},
				{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}},
				{{x1,y1,z0},{x1,y0,z0},{x0,y0,z0},{x0,y1,z0}},
				{{x0,y1,z1},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1}},
				{{x0,y1,z0},{x0,y0,z0},{x0,y0,z1},{x0,y1,z1}},
				{{x1,y1,z1},{x1,y0,z1},{x1,y0,z0},{x1,y1,z0}}
		};
		Direction[] directions = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
		for (int f = 0; f < faces.length; f++) {
			if (mesh.size() >= MAX_FACES) { truncated = true; return; }
			float shade = shade(directions[f]);
			List<Vertex> face = new ArrayList<>(4);
			for (int i = 0; i < 4; i++) {
				float[] p = faces[f][i];
				face.add(vertex(offset, p[0], p[1], p[2], i < 2 ? sprite.getU0() : sprite.getU1(),
						i == 0 || i == 3 ? sprite.getV0() : sprite.getV1(), shade, shade, shade));
			}
			mesh.add(face);
		}
	}

	private static float shade(Direction direction) {
		return switch (direction) {
			case UP -> 1;
			case DOWN -> 0.65F;
			case NORTH, SOUTH -> 0.85F;
			case EAST, WEST -> 0.75F;
		};
	}
}
