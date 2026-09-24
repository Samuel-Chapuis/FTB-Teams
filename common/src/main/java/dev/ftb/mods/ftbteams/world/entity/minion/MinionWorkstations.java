package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.block.entity.EnclosureBlockEntity;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

/** Stateless queries for locating and reserving a faction workstation. */
public final class MinionWorkstations {
	private static final int SEARCH_RADIUS = 48;
	private static final int VERTICAL_RADIUS = 16;

	private MinionWorkstations() {
	}

	public static BlockPos findNearest(MinionEntity minion, Predicate<BlockState> supportedBlock) {
		if (!(minion.level() instanceof ServerLevel level) || minion.getHome().isEmpty()) {
			return null;
		}
		BlockPos home = minion.getHome().orElseThrow();
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		int minChunkX = SectionPos.blockToSectionCoord(home.getX() - SEARCH_RADIUS);
		int maxChunkX = SectionPos.blockToSectionCoord(home.getX() + SEARCH_RADIUS);
		int minChunkZ = SectionPos.blockToSectionCoord(home.getZ() - SEARCH_RADIUS);
		int maxChunkZ = SectionPos.blockToSectionCoord(home.getZ() + SEARCH_RADIUS);
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}
				for (BlockPos candidate : chunk.getBlockEntities().keySet()) {
					double distance = candidate.distSqr(home);
					if (Math.abs(candidate.getY() - home.getY()) <= VERTICAL_RADIUS
							&& distance <= SEARCH_RADIUS * SEARCH_RADIUS && distance < nearestDistance
							&& isAvailable(minion, candidate, supportedBlock)) {
						nearest = candidate.immutable();
						nearestDistance = distance;
					}
				}
			}
		}
		return nearest;
	}

	public static boolean isAvailable(MinionEntity minion, BlockPos pos, Predicate<BlockState> supportedBlock) {
		if (!(minion.level() instanceof ServerLevel level) || !level.hasChunkAt(pos)
				|| !supportedBlock.test(level.getBlockState(pos))
				|| !(level.getBlockEntity(pos) instanceof EnclosureBlockEntity enclosure)
				|| minion.getFactionId() == null || !minion.getFactionId().equals(enclosure.getFactionId())) {
			return false;
		}
		return level.getEntitiesOfClass(MinionEntity.class, new AABB(pos).inflate(3),
				other -> other != minion && other.isAssignedTo(pos)).isEmpty();
	}

	public static ItemStack icon(MinionEntity minion) {
		return minion.getWorkstation().map(pos -> minion.level().getBlockState(pos).getBlock().asItem())
				.filter(item -> item != Items.AIR).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}
}
