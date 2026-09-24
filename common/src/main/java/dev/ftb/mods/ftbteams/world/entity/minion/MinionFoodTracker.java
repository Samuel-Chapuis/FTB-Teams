package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Tracks the last charged Minecraft day and applies the minion's daily food cost. */
public final class MinionFoodTracker {
	private static final String DAY_TAG = "MinionFoodDay";
	private static final String NUTRITION_TAG = "MinionNutrition";
	private static final int NUTRITION_PER_LEVEL = 10;
	private long lastDay = Long.MIN_VALUE;
	private int nutrition;

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

	/** Adds vanilla food nutrition and awards one minion food level for each complete group of ten. */
	public void consume(MinionEntity minion, int points) {
		nutrition += Math.max(0, points);
		while (nutrition >= NUTRITION_PER_LEVEL && minion.getFood() < MinionEntity.MAX_FOOD) {
			nutrition -= NUTRITION_PER_LEVEL;
			minion.setFood(minion.getFood() + 1);
		}
	}

	/** Writes only the tracker state; the current food value remains synced by the entity. */
	public void save(CompoundTag tag) {
		tag.putLong(DAY_TAG, lastDay);
		tag.putInt(NUTRITION_TAG, nutrition);
	}

	/** Restores the tracker without charging food during the initial load tick. */
	public void load(CompoundTag tag) {
		lastDay = tag.contains(DAY_TAG) ? tag.getLong(DAY_TAG) : Long.MIN_VALUE;
		nutrition = Math.clamp(tag.getInt(NUTRITION_TAG), 0, NUTRITION_PER_LEVEL - 1);
	}
}
