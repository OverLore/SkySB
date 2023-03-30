package skysb.caches;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import skysb.islands.Island;
import skysb.skysb.SkySB;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class IslandsCache {
    SkySB plugin;

    private TreeMap<Integer, TreeMap<Integer, Island>> islandGrid = new TreeMap<>();

    private Map<UUID, Island> islands = new HashMap<>();

    public IslandsCache(SkySB plugin)
    {
        this.plugin = plugin;
    }

    public void loadAllIsland() {
        plugin.getLogger().info("Loading all island...");

        ResultSet rs = plugin.db.executeQuery("SELECT * FROM `islands`;");

        try {
            while (rs.next())
            {
                Island is = Island.loadIsland(rs);

                if (is.owner == null)
                    continue;

                plugin.getLogger().info("Island " + is.id + " found, owned by " + plugin.skyPlayer.getPlayerName(is.owner) + "(" + is.owner + ")");

                if (islandGrid.containsKey(is.center.getBlockX())) {
                    TreeMap<Integer, Island> zEntry = islandGrid.get(is.center.getBlockX());

                    zEntry.put(is.center.getBlockZ(), is);
                    islandGrid.put(is.center.getBlockX(), zEntry);
                } else {
                    TreeMap<Integer, Island> zEntry = new TreeMap<Integer, Island>();

                    zEntry.put(is.center.getBlockZ(), is);
                    islandGrid.put(is.center.getBlockX(), zEntry);
                }

                for (UUID member: is.members)
                {
                    islands.put(member, is);
                }

                islands.put(is.owner, is);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addIsland(Island is)
    {
        if (islandGrid.containsKey(is.center.getBlockX())) {
            TreeMap<Integer, Island> zEntry = islandGrid.get(is.center.getBlockX());

            zEntry.put(is.center.getBlockZ(), is);
            islandGrid.put(is.center.getBlockX(), zEntry);
        } else {
            TreeMap<Integer, Island> zEntry = new TreeMap<Integer, Island>();

            zEntry.put(is.center.getBlockZ(), is);
            islandGrid.put(is.center.getBlockX(), zEntry);
        }

        islands.put(is.owner, is);
    }

    public void addMemberToIsland(Island is, UUID uuid)
    {
        islands.put(uuid, is);
    }

    public void removeMemberFromIsland(UUID uuid)
    {
        islands.remove(uuid);
    }

    public Island getIsland(UUID uuid)
    {
        return islands.get(uuid);
    }

    public Island getIsland(Player player)
    {
        return islands.get(player.getUniqueId());
    }

    public Island getIsland(int x, int z)
    {
        if (!islandGrid.containsKey(z))
            return null;

        if (!islandGrid.get(x).containsKey(z))
            return null;

        return islandGrid.get(x).get(z);
    }

    public void deleteIsland(Island is)
    {
        islandGrid.get(is.center.getBlockX()).remove(is.center.getBlockZ());

        for (UUID member: is.members)
        {
            islands.remove(member);
        }

        islands.remove(is.owner);
    }

    public void LogToPlayer(Player p)
    {
        for (Map.Entry<UUID, Island> entry : islands.entrySet()) {
            p.sendMessage(entry.getKey() + " member of " + entry.getValue().name);
        }
    }
}
