package skysb.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import skysb.islands.Island;
import skysb.localization.Messages;
import skysb.skysb.SkySB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeletionManager {
    SkySB plugin;

    List<UUID> pendingDeletions = new ArrayList<>();

    public DeletionManager(SkySB plugin)
    {
        this.plugin = plugin;
    }

    public boolean hasPendingDeletion(UUID uuid)
    {
        return pendingDeletions.contains(uuid);
    }

    public void requestDeletion(Player player)
    {
        UUID uuid = player.getUniqueId();

        if (!Island.hasPlayerAnIsland(player))
        {
            player.sendMessage(Messages.getString("No_island"));

            return;
        }

        Island is = Island.getPlayerIsland(uuid);

        if (!is.isOwner(uuid))
        {
            player.sendMessage(Messages.getString("Delete_as_member"));

            return;
        }

        if (is.getAmountOfMembers() > 1)
        {
            player.sendMessage(Messages.getString("Delete_not_empty"));

            return;
        }

        pendingDeletions.add(uuid);
        player.sendMessage(Messages.getString("Delete_please_confirm"));

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(plugin, () -> {
            if (!pendingDeletions.contains(uuid))
                return;

            if (player != null)
                player.sendMessage(Messages.getString("Delete_not_responded"));

            pendingDeletions.remove(uuid);
        }, 200L);
    }

    public void confirmDeletion(Player player)
    {
        UUID uuid = player.getUniqueId();

        if (!pendingDeletions.contains(uuid))
            return;

        Island is = Island.getPlayerIsland(uuid);

        plugin.islandsCache.deleteIsland(is);
        is.delete();

        is = null;

        player.sendMessage(Messages.getString("Delete_finish"));
    }
}
