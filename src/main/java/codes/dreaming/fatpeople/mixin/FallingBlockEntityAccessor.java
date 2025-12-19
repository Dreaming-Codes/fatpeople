package codes.dreaming.fatpeople.mixin;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccessor {
    
    @Accessor("blockState")
    @Mutable
    void setBlockState(BlockState state);
    
    @Accessor("cancelDrop")
    @Mutable
    void setCancelDrop(boolean cancelDrop);
}
