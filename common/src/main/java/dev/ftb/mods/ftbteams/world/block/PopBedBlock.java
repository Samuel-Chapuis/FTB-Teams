package dev.ftb.mods.ftbteams.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Vanilla bed placement and shape, with enclosure interaction instead of player sleeping. */
public class PopBedBlock extends EnclosureBlock {
	public static final MapCodec<PopBedBlock> CODEC = simpleCodec(PopBedBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

	public PopBedBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT));
	}

	@Override
	protected MapCodec<PopBedBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	@Override
	public BlockPos getControllerPos(BlockState state, BlockPos pos) {
		return state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
	}

	@Override
	public List<BlockPos> getInteriorSeeds(BlockState state, BlockPos pos) {
		BlockPos head = getControllerPos(state, pos);
		return List.of(head.above(), head.relative(state.getValue(FACING).getOpposite()).above());
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction direction = context.getHorizontalDirection();
		BlockPos head = context.getClickedPos().relative(direction);
		return context.getLevel().getWorldBorder().isWithinBounds(head)
				&& context.getLevel().getBlockState(head).canBeReplaced(context)
				? defaultBlockState().setValue(FACING, direction) : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide) {
			level.setBlock(pos.relative(state.getValue(FACING)), state.setValue(PART, BedPart.HEAD), UPDATE_ALL);
			level.blockUpdated(pos, Blocks.AIR);
			state.updateNeighbourShapes(level, pos, UPDATE_ALL);
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		Direction partner = state.getValue(PART) == BedPart.FOOT ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
		if (direction == partner) {
			return neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART)
					&& neighbor.getValue(FACING) == state.getValue(FACING) ? state : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// Like vanilla: removing the foot in creative must not drop the head's block item.
		if (!level.isClientSide && player.isCreative() && state.getValue(PART) == BedPart.FOOT) {
			BlockPos head = getControllerPos(state, pos);
			BlockState headState = level.getBlockState(head);
			if (headState.is(this) && headState.getValue(PART) == BedPart.HEAD) {
				level.setBlock(head, Blocks.AIR.defaultBlockState(), UPDATE_ALL | UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, head, Block.getId(headState));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Blocks.CYAN_BED.defaultBlockState().setValue(BedBlock.FACING, state.getValue(FACING))
				.setValue(BedBlock.PART, state.getValue(PART)).getShape(level, pos, context);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
