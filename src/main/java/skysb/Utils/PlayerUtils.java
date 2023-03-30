package skysb.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import skysb.skysb.SkySB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerUtils {
    public static UUID getPlayerUUID(String name)
    {
        Player onlinePlayer = Bukkit.getPlayer(name);

        if (onlinePlayer == null)
        {
            OfflinePlayer p = Bukkit.getOfflinePlayer(name);

            if (p == null)
                return null;

            return p.getUniqueId();
        }

        return onlinePlayer.getUniqueId();
    }

    public static boolean playerIsHolding(Player player, Material type) {
        player.getInventory().getItemInMainHand();
        if (player.getInventory().getItemInMainHand().getType().equals(type)) {
            return true;
        }
        player.getInventory().getItemInMainHand();
        return player.getInventory().getItemInOffHand().getType().equals(type);
    }

    public static List<ItemStack> getPlayerInHandItems(Player player) {
        List<ItemStack> result = new ArrayList<ItemStack>(2);
        player.getInventory().getItemInMainHand();
        result.add(player.getInventory().getItemInMainHand());
        player.getInventory().getItemInOffHand();
        result.add(player.getInventory().getItemInOffHand());
        return result;
    }

    public static String prettifyText(String ugly) {
        if (!ugly.contains("_") && (!ugly.equals(ugly.toUpperCase())))
            return ugly;
        String fin = "";
        ugly = ugly.toLowerCase();
        if (ugly.contains("_")) {
            String[] splt = ugly.split("_");
            int i = 0;
            for (String s : splt) {
                i += 1;
                fin += Character.toUpperCase(s.charAt(0)) + s.substring(1);
                if (i < splt.length)
                    fin += " ";
            }
        } else {
            fin += Character.toUpperCase(ugly.charAt(0)) + ugly.substring(1);
        }
        return fin;
    }

    public boolean isSpawnEgg(Material egg)
    {
        return egg.name().contains("SPAWN_EGG");
    }
}
