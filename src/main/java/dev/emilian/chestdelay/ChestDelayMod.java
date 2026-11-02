package dev.emilian.chestdelay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ChestDelayMod implements ModInitializer {

    private static final List<PendingChestOpen> PENDING = new ArrayList<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<PendingChestOpen> it = PENDING.iterator();
            while (it.hasNext()) {
                PendingChestOpen pending = it.next();
                pending.ticksLeft--;
                if (pending.ticksLeft <= 0) {
                    it.remove();
                    ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUUID);
                    if (player == null) continue;

                    ServerLevel level = server.getLevel(pending.dimension);
                    if (level != null && level.getBlockState(pending.pos).getBlock() instanceof ChestBlock) {
                        MenuProvider menuProvider = level.getBlockState(pending.pos).getMenuProvider(level, pending.pos);
                        if (menuProvider != null) {
                            player.openMenu(menuProvider);
                        }
                    } else {
                        openSnapshotChest(player, pending.items, pending.doubleChest);
                    }
                }
            }
        });
    }

    public static void scheduleChestOpen(UUID playerUUID, ResourceKey<Level> dimension, BlockPos pos,
                                          NonNullList<ItemStack> items, boolean doubleChest) {
        PENDING.removeIf(p -> p.playerUUID.equals(playerUUID));
        PENDING.add(new PendingChestOpen(playerUUID, dimension, pos, items, doubleChest, 20));
    }

    private static void openSnapshotChest(ServerPlayer player, NonNullList<ItemStack> items, boolean doubleChest) {
        int size = doubleChest ? 54 : 27;
        SimpleContainer container = new SimpleContainer(size);
        for (int i = 0; i < Math.min(items.size(), size); i++) {
            container.setItem(i, items.get(i));
        }
        player.openMenu(new SimpleMenuProvider(
                (syncId, playerInv, p) -> doubleChest
                        ? ChestMenu.sixRows(syncId, playerInv, container)
                        : ChestMenu.threeRows(syncId, playerInv, container),
                Component.literal("Chest")
        ));
    }

    private static class PendingChestOpen {
        final UUID playerUUID;
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        final NonNullList<ItemStack> items;
        final boolean doubleChest;
        int ticksLeft;

        PendingChestOpen(UUID playerUUID, ResourceKey<Level> dimension, BlockPos pos,
                         NonNullList<ItemStack> items, boolean doubleChest, int ticksLeft) {
            this.playerUUID = playerUUID;
            this.dimension = dimension;
            this.pos = pos;
            this.items = items;
            this.doubleChest = doubleChest;
            this.ticksLeft = ticksLeft;
        }
    }
}
