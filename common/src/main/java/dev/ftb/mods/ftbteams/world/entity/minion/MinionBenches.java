package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.block.PopBenchBlock;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/** Finds nearby unoccupied bench halves without loading new chunks. */
public final class MinionBenches {
	private static final int HORIZONTAL_RADIUS = 24;
	private static final int VERTICAL_RADIUS = 8;

	private MinionBenches() {
	}

	public static BlockPos findSeat(MinionEntity minion) {
		if (!(minion.level() instanceof ServerLevel level)) {
			return null;
		}
		BlockPos origin = minion.blockPosition();
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-HORIZONTAL_RADIUS, -VERTICAL_RADIUS, -HORIZONTAL_RADIUS),
				origin.offset(HORIZONTAL_RADIUS, VERTICAL_RADIUS, HORIZONTAL_RADIUS))) {
			double distance = candidate.distSqr(origin);
			if (distance < nearestDistance && level.hasChunkAt(candidate)
					&& level.getBlockState(candidate).getBlock() instanceof PopBenchBlock && isFree(level, candidate, minion)) {
				nearest = candidate.immutable();
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	public static boolean isFree(ServerLevel level, BlockPos seat, MinionEntity minion) {
		return level.getBlockState(seat).getBlock() instanceof PopBenchBlock
				&& level.getEntitiesOfClass(MinionEntity.class, new AABB(seat).inflate(0.5),
						other -> other != minion && other.isSittingAt(seat)).isEmpty();
	}
}
