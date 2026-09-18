package dev.ftb.mods.ftbteams.world.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Base class for blocks that can represent an enclosure building.
 *
 * <p>The enclosure validation and occupation rules will be added here as the
 * population system is implemented. Keeping the common block behaviour in one
 * place lets every enclosure share those rules.</p>
 */
public abstract class Enclosure extends Block {
	protected Enclosure(BlockBehaviour.Properties properties) {
		super(properties);
	}
}
