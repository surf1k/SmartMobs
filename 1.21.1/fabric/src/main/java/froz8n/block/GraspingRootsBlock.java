package froz8n.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class GraspingRootsBlock extends Block {
    public static final IntegerProperty ROTATION=IntegerProperty.create("rotation",0,3);
    public GraspingRootsBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(ROTATION,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ROTATION);}
}
