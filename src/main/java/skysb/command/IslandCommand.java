package skysb.command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skysb.Utils.SchematicsUtils;
import skysb.islands.Island;
import skysb.localization.Messages;
import skysb.skysb.SkySB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IslandCommand extends BaseCommand {
    public IslandCommand(SkySB plugin) { super(plugin); }

    @Override
    protected boolean parseCommand(@NotNull String[] args) {
        if (isConsole)
        {
            plugin.getLogger().info(Messages.getString("No_console"));
            return true;
        }

        if (args.length > 0)
        {
            if (args[0].equalsIgnoreCase("cache"))
                plugin.islandsCache.LogToPlayer(senderPlayer);
            if (args[0].equalsIgnoreCase("create"))
                islandDefault(senderPlayer);
            if (args[0].equalsIgnoreCase("go"))
                goToIsland(senderPlayer.getName());
            if (args[0].equalsIgnoreCase("accept"))
                plugin.invitationManager.respondInvite(senderPlayer.getUniqueId(), true);
            if (args[0].equalsIgnoreCase("deny"))
                plugin.invitationManager.respondInvite(senderPlayer.getUniqueId(), false);
            if (args[0].equalsIgnoreCase("delete"))
                plugin.deletionManager.requestDeletion(senderPlayer);
            if (args[0].equalsIgnoreCase("confirm"))
                plugin.deletionManager.confirmDeletion(senderPlayer);
            if (args[0].equalsIgnoreCase("visit"))
            {
                if (args.length == 1)
                {
                    senderPlayer.sendMessage(Messages.getString("Not_enough_args"));
                    senderPlayer.sendMessage(ChatColor.RED + Messages.getString("Usage") + " : /is visit <player>");

                    return true;
                }

                goToIsland(args[1]);
            }
            if (args[0].equalsIgnoreCase("invite"))
            {
                if (args.length == 1)
                {
                    senderPlayer.sendMessage(Messages.getString("Not_enough_args"));
                    senderPlayer.sendMessage(ChatColor.RED + Messages.getString("Usage") + " : /is invite <player>");

                    return true;
                }

                if (!Island.hasPlayerAnIsland(senderPlayer))
                {
                    senderPlayer.sendMessage(Messages.getString("No_island"));

                    return true;
                }

                Player invitedPlayer = Bukkit.getPlayer(args[1]);
                if (invitedPlayer == null) {
                    senderPlayer.sendMessage(Messages.getString("Player_not_connected").replace("%player%", args[1]));

                    return true;
                }

                if (senderPlayer.getUniqueId().equals(invitedPlayer.getUniqueId())){
                    senderPlayer.sendMessage(Messages.getString("Cannot_invite_self"));

                    return true;
                }

                if (Island.areFromSameIsland(senderPlayer.getUniqueId(), invitedPlayer.getUniqueId())){
                    senderPlayer.sendMessage(Messages.getString("Already_member"));

                    return true;
                }

                sendInvitation(invitedPlayer);
            }

            return true;
        }

        if (Island.hasPlayerAnIsland(senderPlayer))
        {
            goToIsland(senderPlayer.getName());

            return true;
        }

        islandDefault(senderPlayer);

        return true;
    }

    void islandDefault(Player player)
    {
        player.sendMessage(Messages.getString("Creating_island"));
        World skyWorld = Island.skyWorld;
        createIsland(skyWorld, player);
    }

    void goToIsland(String playerName)
    {
        Player onlinePlayer = Bukkit.getPlayer(playerName);

        if (onlinePlayer == null)
        {
            UUID uuid = plugin.skyPlayer.getPlayerUUID(playerName);

            if (uuid == null)
            {
                senderPlayer.sendMessage(Messages.getString("Player_dont_exist").replace("%player%", playerName));

                return;
            }

            Island.getPlayerIsland(uuid).teleportTo(senderPlayer);

            return;
        }

        if (onlinePlayer.equals(senderPlayer))
            senderPlayer.sendMessage(Messages.getString("Teleport_to_island"));
        else
            senderPlayer.sendMessage(Messages.getString("Teleport_to_someone_island").replace("%player%", playerName));

        Island.teleportToIsland(senderPlayer, onlinePlayer);
    }

    void createIsland(World skyWorld, Player p)
    {
        Island is = Island.CreateIslandForPlayer(p);
        plugin.islandsCache.addIsland(is);

        SchematicsUtils.paste(new Location(skyWorld, is.center.getBlockX(), is.center.getBlockY(), is.center.getBlockZ()), new File(plugin.getDataFolder(), "schematics\\island.schem"));
        p.sendMessage(Messages.getString("Created_island"));

        p.teleport(new Location(skyWorld, is.center.getBlockX(), is.center.getBlockY() + 2, is.center.getBlockZ(), 0, 0));
    }

    public void sendInvitation(Player invitedPlayer) {
        plugin.invitationManager.sendInvite(senderPlayer.getUniqueId(), invitedPlayer.getUniqueId());
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        List<String> result = new ArrayList<>();

        switch (args.length)
        {
            case 0:
                break;
            case 1:
                result = tabCompleteArgs1(args[0]);
                break;
            default:
                result = tabCompleteUsers(args[args.length - 1]);
                break;
        }

        return result;
    }

}