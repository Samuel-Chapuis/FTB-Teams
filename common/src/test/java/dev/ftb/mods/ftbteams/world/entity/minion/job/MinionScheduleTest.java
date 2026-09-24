package dev.ftb.mods.ftbteams.world.entity.minion.job;

/** Boundary regression tests for minion work, leisure, and sleep periods. */
public final class MinionScheduleTest {
	private static int assertions;

	public static void main(String[] args) {
		check(MinionSchedule.tickAtHour(6) == 0, "06:00 starts at world tick zero");
		check(MinionSchedule.tickAtHour(7) == 1_000, "07:00 conversion");
		check(MinionSchedule.tickAtHour(8) == 2_000, "08:00 conversion");
		check(MinionSchedule.tickAtHour(17) == 11_000, "17:00 conversion");
		check(MinionSchedule.tickAtHour(21) == 15_000, "21:00 conversion");

		check(!CashierJob.INSTANCE.isWorkTime(1_999), "cashier is off duty before 08:00");
		check(CashierJob.INSTANCE.isWorkTime(2_000), "cashier starts at 08:00");
		check(CashierJob.INSTANCE.isWorkTime(10_999), "cashier remains on duty before 17:00");
		check(!CashierJob.INSTANCE.isWorkTime(11_000), "cashier stops at 17:00");

		check(!MinionSchedule.isSleepTime(14_999), "minion remains awake before 21:00");
		check(MinionSchedule.isSleepTime(15_000), "minion sleeps from 21:00");
		check(MinionSchedule.isSleepTime(999), "minion remains asleep before 07:00");
		check(!MinionSchedule.isSleepTime(1_000), "minion wakes at 07:00");
		check(MinionSchedule.isSleepTime(39_000), "schedule repeats on following days");
		check(isCashierLeisure(1_000), "cashier can wander from 07:00 until 08:00");
		check(!isCashierLeisure(2_000), "cashier does not wander after starting work");
		check(isCashierLeisure(11_000), "cashier can wander from 17:00 until 21:00");
		check(!isCashierLeisure(15_000), "cashier stops wandering when sleep begins");
		System.out.println("MinionScheduleTest: " + assertions + " assertions passed");
	}

	private static boolean isCashierLeisure(long dayTime) {
		return !MinionSchedule.isSleepTime(dayTime) && !CashierJob.INSTANCE.isWorkTime(dayTime);
	}

	private static void check(boolean condition, String message) {
		assertions++;
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
