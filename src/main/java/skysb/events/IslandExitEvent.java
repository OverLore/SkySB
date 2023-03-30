package skysb.events;

import org.bukkit.Location;
import skysb.islands.Island;

import java.util.UUID;

public class IslandExitEvent extends SkyblockEvent {
    private final Location location;

    public IslandExitEvent(UUID player, Island island, Location location) {
        super(player, island);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

}
