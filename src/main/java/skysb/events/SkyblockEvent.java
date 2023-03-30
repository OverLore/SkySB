package skysb.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import skysb.islands.Island;

public abstract class SkyblockEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID player;
    private final Island island;

    public SkyblockEvent(UUID player, Island island) {
        this.player = player;
        this.island = island;
    }

    public SkyblockEvent(UUID player) {
        this.player = player;
        this.island = null;
    }

    public UUID getPlayer() {
        return player;
    }

    public Island getIsland() {
        return island;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}