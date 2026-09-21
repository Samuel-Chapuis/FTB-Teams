package dev.ftb.mods.ftbteams.world.inventory;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Read-only session data for the minion profile screen. The illustrated storage grid is deliberately
 * not a container yet: minion work inventories will define their own rules in the next population step.
 */
public class MinionMenu extends AbstractContainerMenu {
	public static final int CLEAR_PROFESSION_BUTTON = 0;
	private final Player viewer;
	private final int minionId;
	private final String factionName;
	private final int factionColor;
	private final String factionLogo;
	private final String profession;
	private final ItemStack professionStack;
	private final BlockPos home;
	private final BlockPos workstation;

	public MinionMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
		this(id, inventory, buf.readVarInt(), buf.readUtf(96), buf.readInt(), buf.readUtf(8192), buf.readUtf(256), ItemStack.EMPTY,
				buf.readBoolean() ? buf.readBlockPos() : null, buf.readBoolean() ? buf.readBlockPos() : null);
	}

	public MinionMenu(int id, Inventory inventory, MinionEntity minion) {
		this(id, inventory, minion.getId(), minion.getFactionName(), minion.getFactionColor(), minion.getFactionLogo(), minion.getProfession(),
				minion.getProfessionStack(), minion.getHome().orElse(null), minion.getWorkstation().orElse(null));
	}

	private MinionMenu(int id, Inventory inventory, int minionId, String factionName, int factionColor, String factionLogo, String profession,
			ItemStack professionStack, BlockPos home, BlockPos workstation) {
		super(FTBTeams.MINION_MENU.get(), id);
		viewer = inventory.player;
		this.minionId = minionId;
		this.factionName = factionName;
		this.factionColor = factionColor;
		this.factionLogo = factionLogo;
		this.profession = profession;
		this.professionStack = professionStack;
		this.home = home;
		this.workstation = workstation;
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
	public ItemStack getProfessionStack() { return professionStack; }
	public BlockPos getHome() { return home; }
	public BlockPos getWorkstation() { return workstation; }

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == CLEAR_PROFESSION_BUTTON && player instanceof ServerPlayer serverPlayer && stillValid(player)
				&& player.level().getEntity(minionId) instanceof MinionEntity minion && minion.canAccess(serverPlayer)) {
			minion.clearProfession();
			return true;
		}
		return false;
	}

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
