package dev.ftb.mods.ftbteams.world.entity;

import dev.ftb.mods.ftbteams.world.entity.minion.MinionAssignments;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionWorkAssignments;
import java.util.UUID;

public final class MinionAssignmentsTest {
	private static int assertions;

	public static void main(String[] args) {
		var homes = new MinionAssignments();
		UUID faction = UUID.randomUUID(), owner = UUID.randomUUID(), resident = UUID.randomUUID(), replacement = UUID.randomUUID();
		check(homes.claim(42, resident, owner, faction), "first validation assigns the resident");
		check(!homes.claim(42, replacement, owner, faction), "repeated validation must not duplicate a resident");
		check(!homes.claim(84, resident, owner, faction), "one resident cannot occupy two beds");
		check(!homes.claim(42, replacement, UUID.randomUUID(), UUID.randomUUID()), "another faction cannot replace the resident");
		// Simulate saving/reloading the independent ledger while the entity's chunk is absent.
		var saved = homes.entries();
		var loaded = new MinionAssignments();
		saved.forEach((home, entry) -> loaded.claim(home, entry.minion(), entry.owner(), entry.faction()));
		check(loaded.get(42).minion().equals(resident), "resident survives a ledger reload");
		check(loaded.get(42).owner().equals(owner), "owner survives a ledger reload");
		check(loaded.get(42).faction().equals(faction), "faction survives a ledger reload");
		check(!loaded.claim(42, replacement, owner, faction), "an unloaded resident still occupies its bed");
		check(!loaded.release(42, replacement), "a different entity cannot release the bed");
		check(loaded.release(42, resident), "death or bed destruction releases the resident");
		check(!loaded.release(42, resident), "repeated removal is harmless");
		check(loaded.claim(42, replacement, owner, faction), "later validation can replace a dead resident");
		check(!loaded.release(42, resident), "late removal of old resident cannot remove replacement");
		check(loaded.get(42).minion().equals(replacement), "replacement remains assigned");
		check(loaded.containsResident(replacement), "the resident reverse index identifies an assigned minion");
		check(saved.get(42L).minion().equals(resident), "save snapshots remain immutable");
		check(loaded.claim(84, resident, owner, faction), "released resident no longer reserves another bed");
		testWorkstations();
		System.out.println("MinionAssignmentsTest: " + assertions + " assertions passed");
	}

	private static void testWorkstations() {
		var workplaces = new MinionWorkAssignments();
		UUID cashier = UUID.randomUUID(), replacement = UUID.randomUUID();
		check(workplaces.claim(12, cashier), "a free workstation accepts its first minion");
		check(workplaces.claim(12, cashier), "reclaiming the same workstation is idempotent");
		check(!workplaces.claim(12, replacement), "another minion cannot take an occupied workstation");
		check(!workplaces.claim(24, cashier), "one minion cannot reserve two workstation blocks");
		check(workplaces.workerAt(12).equals(cashier), "the workstation remembers its minion");
		check(workplaces.positionOf(cashier) == 12, "the minion remembers its workstation");
		check(workplaces.release(cashier), "clearing a profession releases the workstation");
		check(workplaces.claim(12, replacement), "a released workstation accepts a replacement");
		check(workplaces.remove(12), "destroying a workstation removes its assignment");
		check(workplaces.positionOf(replacement) == null, "destroying a workstation clears the reverse association");
	}

	private static void check(boolean condition, String message) {
		assertions++;
		if (!condition) throw new AssertionError(message);
	}
}
