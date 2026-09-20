package dev.ftb.mods.ftbteams.world.inventory;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Read-only session data for the minion profile screen. The illustrated storage grid is deliberately
 * not a container yet: minion work inventories will define their own rules in the next population step.
 */
public class MinionMenu extends AbstractContainerMenu {
	private final Player viewer;
	private final int minionId;
	private final String factionName;
	private final int factionColor;
	private final String factionLogo;
	private final String profession;

	public MinionMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
		this(id, inventory, buf.readVarInt(), buf.readUtf(96), buf.readInt(), buf.readUtf(8192), buf.readUtf(256));
	}

	public MinionMenu(int id, Inventory inventory, MinionEntity minion) {
		this(id, inventory, minion.getId(), minion.getFactionName(), minion.getFactionColor(), minion.getFactionLogo(), minion.getProfession());
	}

	private MinionMenu(int id, Inventory inventory, int minionId, String factionName, int factionColor, String factionLogo, String profession) {
		super(FTBTeams.MINION_MENU.get(), id);
		viewer = inventory.player;
		this.minionId = minionId;
		this.factionName = factionName;
		this.factionColor = factionColor;
		this.factionLogo = factionLogo;
		this.profession = profession;
	}

	public int getMinionId() {
		return minionId;
	}

	public String getFactionName() {
		return factionName;
	}

	public int getFactionColor() {
		return factionColor;
	}

	public String getFactionLogo() {
		return factionLogo;
	}

	public String getProfession() { return profession; }

	@Override
	public boolean stillValid(Player player) {
		Entity entity = player.level().getEntity(minionId);
		return entity instanceof MinionEntity minion && minion.isAlive() && player.distanceToSqr(minion) <= 64D
				&& (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) || minion.canAccess(serverPlayer));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
