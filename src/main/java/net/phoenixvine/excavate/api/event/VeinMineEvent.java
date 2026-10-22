package net.phoenixvine.excavate.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class VeinMineEvent extends Event {

    private final Player player;
    private final BlockPos origin;

    private VeinMineEvent(Player player, BlockPos origin) {
        this.player = player;
        this.origin = origin;
    }

    public Player getPlayer() {
        return player;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public static class Pre extends VeinMineEvent implements ICancellableEvent {

        private final List<BlockPos> candidates;

        public Pre(Player player, BlockPos origin, List<BlockPos> candidates) {
            super(player, origin);
            this.candidates = new ArrayList<>(candidates);
        }

        public List<BlockPos> getCandidates() {
            return candidates;
        }
    }

    public static class Post extends VeinMineEvent {

        private final int brokenCount;

        public Post(Player player, BlockPos origin, int brokenCount) {
            super(player, origin);
            this.brokenCount = brokenCount;
        }

        public int getBrokenCount() {
            return brokenCount;
        }
    }
}
