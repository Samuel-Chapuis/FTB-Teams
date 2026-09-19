package dev.ftb.mods.ftbteams.world.inventory;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlock;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlockEntity;
import dev.ftb.mods.ftbteams.world.block.EnclosureScanner;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class EnclosureMenu extends AbstractContainerMenu {
	public static final int CHECK_BUTTON = 0;
	public static final int STORAGE_X = 14;
	public static final int STORAGE_Y = 40;
	public static final int PLAYER_X = 79;
	public static final int PLAYER_Y = 152;
	private final Container container;
	private final ContainerData data;
	private final int storageSize;

	public EnclosureMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
		this(id, inventory, new SimpleContainer(buf.readVarInt()), new SimpleContainerData(2));
	}

	public EnclosureMenu(int id, Inventory inventory, Container container, ContainerData data) {
		super(FTBTeams.ENCLOSURE_MENU.get(), id);
		this.container = container;
		this.data = data;
		storageSize = container.getContainerSize();
		if (storageSize != 0 && storageSize != EnclosureBlock.STORAGE_SIZE) {
			throw new IllegalArgumentException("Enclosure storage must have 0 or 25 slots");
		}
		checkContainerDataCount(data, 2);
		container.startOpen(inventory.player);
		for (int slot = 0; slot < storageSize; slot++) {
			addSlot(new Slot(container, slot, STORAGE_X + slot % 5 * 18, storageY(slot)));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_X + col * 18, PLAYER_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, PLAYER_X + col * 18, PLAYER_Y + 58));
		}
		addDataSlots(data);
	}

	public static int storageY(int slot) {
		return STORAGE_Y + slot / 5 * 18 + (slot >= EnclosureBlock.INPUT_SLOTS ? 4 : 0);
	}

	/** Input and output roles are shared by the UI and future building processing logic. */
	public boolean isInputSlot(int slot) {
		return hasStorage() && slot >= 0 && slot < EnclosureBlock.INPUT_SLOTS;
	}

	public boolean isOutputSlot(int slot) {
		return hasStorage() && slot >= EnclosureBlock.INPUT_SLOTS && slot < storageSize;
	}

	public boolean hasStorage() {
		return storageSize > 0;
	}

	public EnclosureScanner.Status getStatus() {
		return EnclosureScanner.Status.values()[data.get(0)];
	}

	public int getVolume() {
		return data.get(1);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == CHECK_BUTTON && !player.isSpectator() && stillValid(player)
				&& container instanceof EnclosureBlockEntity enclosure && enclosure.checkEnclosure()) {
			broadcastChanges();
			return true;
		}
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(index);
		if (!slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < storageSize) {
			if (!moveItemStackTo(stack, storageSize, slots.size(), true)) return ItemStack.EMPTY;
		} else if (hasStorage()) {
			if (!moveItemStackTo(stack, 0, storageSize, false)) return ItemStack.EMPTY;
		} else if (index < storageSize + 27) {
			if (!moveItemStackTo(stack, storageSize + 27, slots.size(), false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, storageSize, storageSize + 27, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}
}
