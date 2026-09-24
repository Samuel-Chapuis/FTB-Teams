package dev.ftb.mods.ftbteams.world.entity.minion.food;

import dev.ftb.mods.ftbteams.world.block.CashRegisterBlock;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlock;
import dev.ftb.mods.ftbteams.world.block.entity.EnclosureBlockEntity;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Locates staffed faction cash registers and consumes food from their input inventory. */
public final class CashRegisterFoodSource {
	private static final int SEARCH_RADIUS = 48;
	private static final int VERTICAL_RADIUS = 16;

	private CashRegisterFoodSource() {
	}

	public static BlockPos findNearest(MinionEntity minion) {
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
							&& isAvailable(minion, candidate)) {
						nearest = candidate.immutable();
						nearestDistance = distance;
					}
				}
			}
		}
		return nearest;
	}

	/** A source stays valid while it has food and is staffed by this minion or another worker. */
	public static boolean isAvailable(MinionEntity minion, BlockPos pos) {
		if (!(minion.level() instanceof ServerLevel level) || !level.hasChunkAt(pos)
				|| !(level.getBlockState(pos).getBlock() instanceof CashRegisterBlock)
				|| !(level.getBlockEntity(pos) instanceof EnclosureBlockEntity enclosure)
				|| minion.getFactionId() == null || !minion.getFactionId().equals(enclosure.getFactionId())
				|| !hasFood(enclosure)) {
			return false;
		}
		return minion.isAssignedTo(pos) || !level.getEntitiesOfClass(MinionEntity.class, new AABB(pos).inflate(3),
				other -> other.isWorkingAt(pos)).isEmpty();
	}

	/** Consumes one edible item and returns its vanilla nutrition value. */
	public static int takeOne(MinionEntity minion, BlockPos pos) {
		if (!isAvailable(minion, pos) || !(minion.level().getBlockEntity(pos) instanceof EnclosureBlockEntity enclosure)) {
			return 0;
		}
		for (int slot = 0; slot < Math.min(EnclosureBlock.INPUT_SLOTS, enclosure.getContainerSize()); slot++) {
			ItemStack stack = enclosure.getItem(slot);
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food != null && food.nutrition() > 0) {
				stack.shrink(1);
				enclosure.setChanged();
				return food.nutrition();
			}
		}
		return 0;
	}

	private static boolean hasFood(EnclosureBlockEntity enclosure) {
		for (int slot = 0; slot < Math.min(EnclosureBlock.INPUT_SLOTS, enclosure.getContainerSize()); slot++) {
			FoodProperties food = enclosure.getItem(slot).get(DataComponents.FOOD);
			if (food != null && food.nutrition() > 0) {
				return true;
			}
		}
		return false;
	}
}
