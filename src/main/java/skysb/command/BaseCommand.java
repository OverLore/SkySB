package skysb.command;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skysb.islands.Island;
import skysb.localization.Messages;
import skysb.skysb.SkySB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected SkySB plugin;

    protected boolean isConsole = false;
    private boolean playerCanDo = false;
    protected Player senderPlayer;

    protected CommandSender sender;

    public BaseCommand(SkySB _plugin) {
        plugin = _plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!parseSender(sender, label)) return true;

        try {
            return parseCommand(args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!parseSender(sender, alias)) return new ArrayList<>();

        return tabComplete(sender, command, alias, args);
    }

    protected boolean parseSender(CommandSender sender, String alias) {

        playerCanDo = false;
        isConsole = false;

        if (sender instanceof BlockCommandSender) {
            return false;
        }

        if (sender instanceof Player) {
            senderPlayer = (Player) sender;

            if (senderPlayer.isOp() || senderPlayer.hasPermission("skyblock." + alias)) { //$NON-NLS-1$
                playerCanDo = true;
            }
        } else {
            isConsole = true;
        }

        this.sender = sender;

        if (!isConsole && !playerCanDo) {
            sender.sendMessage(ChatColor.RED + Messages.getString("Command_no_permission"));
            return false;
        }

        return true;
    }

    protected List<String> tabCompleteUsers(String arg) {

        List<String> result = new ArrayList<>();
        arg = arg.toLowerCase();

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if((p != null) && (p.getName() != null) && (p.getName().toLowerCase().contains(arg))) {
                result.add(p.getName());
            }
        }

        return result;
    }

    protected List<String> tabCompleteArgs1(String arg) {

        List<String> result = new ArrayList<>();
        arg = arg.toLowerCase();

        List<String> availableArg = new ArrayList<>(Arrays.asList("visit"));

        if (senderPlayer.isOp())
            availableArg.addAll(Arrays.asList("cache"));

        if (Island.hasPlayerAnIsland(senderPlayer)) {
            availableArg.addAll(Arrays.asList("go", "invite", "cp"));

            Island is = Island.getPlayerIsland(senderPlayer);

            if (plugin.invitationManager.hasPendingInvite(senderPlayer.getUniqueId()))
                availableArg.addAll(Arrays.asList("accept", "deny"));

            if (plugin.deletionManager.hasPendingDeletion(senderPlayer.getUniqueId()))
                availableArg.addAll(Arrays.asList("confirm"));

            if (is.isOwner(senderPlayer))
                availableArg.addAll(Arrays.asList("delete"));
            else
                availableArg.addAll(Arrays.asList("leave"));
        }
        else
        {
            availableArg.addAll(Arrays.asList("create"));
        }

        for (String s : availableArg) {
            if ((s != null) && (s.toLowerCase().contains(arg.toLowerCase())) && senderPlayer.hasPermission("skyblock." + s.toLowerCase()))
                result.add(s);
        }

        return result;
    }
    protected abstract boolean parseCommand(@NotNull String[] args);

    protected @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}