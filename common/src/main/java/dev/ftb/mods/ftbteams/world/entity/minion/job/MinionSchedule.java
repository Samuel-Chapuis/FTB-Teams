package dev.ftb.mods.ftbteams.world.entity.minion.job;

/** Converts Minecraft day ticks to clock hours and evaluates repeating daily time ranges. */
public final class MinionSchedule {
	public static final int TICKS_PER_DAY = 24_000;
	public static final int SLEEP_START = tickAtHour(21);
	public static final int SLEEP_END = tickAtHour(7);

	private MinionSchedule() {
	}

	/** Converts a clock hour to Minecraft day time, where tick zero represents 06:00. */
	public static int tickAtHour(int hour) {
		return Math.floorMod(hour - 6, 24) * 1_000;
	}

	/** Returns whether a world time lies in a half-open daily range, including ranges across midnight. */
	public static boolean isBetween(long dayTime, int start, int end) {
		int time = (int) Math.floorMod(dayTime, TICKS_PER_DAY);
		return start <= end ? time >= start && time < end : time >= start || time < end;
	}

	/** Minions sleep from 21:00 until 07:00 independently of weather and sky brightness. */
	public static boolean isSleepTime(long dayTime) {
		return isBetween(dayTime, SLEEP_START, SLEEP_END);
	}
}
