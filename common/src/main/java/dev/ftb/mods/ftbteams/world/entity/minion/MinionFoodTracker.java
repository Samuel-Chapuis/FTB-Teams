package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Tracks the last charged Minecraft day and applies the minion's daily food cost. */
public final class MinionFoodTracker {
	private static final String DAY_TAG = "MinionFoodDay";
	private long lastDay = Long.MIN_VALUE;

	/** Removes one food point for every elapsed day, capped by the minion's complete food bar. */
	public void tick(MinionEntity minion, ServerLevel level) {
		long currentDay = level.getDayTime() / 24000L;
		if (lastDay == Long.MIN_VALUE || currentDay < lastDay) {
			lastDay = currentDay;
			return;
		}
		if (currentDay > lastDay) {
			int elapsedDays = (int) Math.min(currentDay - lastDay, MinionEntity.MAX_FOOD);
			minion.setFood(minion.getFood() - elapsedDays);
			lastDay = currentDay;
		}
	}

	/** Writes only the tracker state; the current food value remains synced by the entity. */
	public void save(CompoundTag tag) {
		tag.putLong(DAY_TAG, lastDay);
	}

	/** Restores the tracker without charging food during the initial load tick. */
	public void load(CompoundTag tag) {
		lastDay = tag.contains(DAY_TAG) ? tag.getLong(DAY_TAG) : Long.MIN_VALUE;
	}
}
