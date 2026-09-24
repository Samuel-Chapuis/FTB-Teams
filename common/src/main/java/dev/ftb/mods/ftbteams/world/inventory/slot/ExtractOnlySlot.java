package dev.ftb.mods.ftbteams.world.inventory.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A slot that accepts extraction from players and automation but rejects every insertion path. */
public final class ExtractOnlySlot extends Slot {
	public ExtractOnlySlot(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}
}
