package dev.emilian.chestdelay.mixin;

import dev.emilian.chestdelay.ChestDelayMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public class ChestBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void onUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult,
                       CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Container container = null;
        try {
            container = ChestBlock.getContainer((ChestBlock) (Object) this, state, level, pos, false);
        } catch (Exception ignored) {}

        if (container == null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container c) {
                container = c;
            }
        }

        if (container == null) return;

        int size = container.getContainerSize();
        boolean isDouble = size > 27;
        NonNullList<ItemStack> snapshot = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                snapshot.set(i, stack.copy());
            }
        }

        ChestDelayMod.scheduleChestOpen(
                serverPlayer.getUUID(),
                level.dimension(),
                pos,
                snapshot,
                isDouble
        );
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
