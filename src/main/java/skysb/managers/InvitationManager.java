package skysb.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import skysb.islands.Island;
import skysb.localization.Messages;
import skysb.skysb.SkySB;

import java.util.*;

public class InvitationManager {
    public SkySB plugin;
    public Map<UUID, UUID> pendingInvites = new HashMap<>(); //<invited player, invited by>

    public InvitationManager(SkySB plugin)
    {
        this.plugin = plugin;
    }

    public UUID getPendingInvite(UUID uuid)
    {
        return pendingInvites.get(uuid);
    }

    public void handleDisconnection(UUID player)
    {
        if (pendingInvites.containsKey(player))
            pendingInvites.remove(player);

        if (pendingInvites.containsValue(player))
            removeValue(pendingInvites, player);
    }

    public boolean hasPendingInvite(UUID uuid)
    {
        return pendingInvites.containsKey(uuid);
    }

    public void respondInvite(UUID invitedPlayer, boolean accepted)
    {
        Player InvitedPlayer = Bukkit.getPlayer(invitedPlayer);

        if (pendingInvites.get(invitedPlayer) == null)
        {
            InvitedPlayer.sendMessage(Messages.getString("No_pending_invitation"));

            return;
        }

        Player InvitingPlayer = Bukkit.getPlayer(pendingInvites.get(invitedPlayer));

        if (accepted)
        {
            if (Island.hasPlayerAnIsland(InvitedPlayer))
            {
                Island is = Island.getPlayerIsland(InvitedPlayer);

                if (is.isOwner(InvitedPlayer)) {
                    InvitedPlayer.sendMessage(Messages.getString("Already_have_island"));

                    return;
                }

                InvitedPlayer.sendMessage(Messages.getString("Already_part_of_island"));

                return;
            }

            if (Island.getPlayerIsland(InvitingPlayer).getAmountOfMembers() == plugin.getConfig().getInt("max_team_size"))
            {
                InvitedPlayer.sendMessage(Messages.getString("Trying_to_join_full_team"));
                InvitingPlayer.sendMessage(Messages.getString("Invitation_other_declined").replace("%player%", InvitedPlayer.getName()));

                pendingInvites.remove(invitedPlayer);

                return;
            }

            pendingInvites.remove(invitedPlayer);
            Island ownerIsland = Island.getPlayerIsland(InvitingPlayer);

            InvitedPlayer.sendMessage(Messages.getString("Invitation_accepted"));
            InvitingPlayer.sendMessage(Messages.getString("Invitation_other_accepted").replace("%player%", InvitedPlayer.getName()));

            ownerIsland.addPlayerToIsland(invitedPlayer);
            ownerIsland.teleportTo(InvitedPlayer);

            return;
        }

        pendingInvites.remove(invitedPlayer);

        InvitedPlayer.sendMessage(Messages.getString("Invitation_declined"));
        InvitingPlayer.sendMessage(Messages.getString("Invitation_other_declined").replace("%player%", InvitedPlayer.getName()));
    }

    public void sendInvite(UUID from, UUID to)
    {
        Player InvitingPlayer = Bukkit.getPlayer(from);
        Player InvitedPlayer = Bukkit.getPlayer(to);

        if (getPendingInvite(to) != null)
        {
            InvitingPlayer.sendMessage(Messages.getString("Already_pending_invite"));

            return;
        }

        if (plugin.islandsCache.getIsland(from).getAmountOfMembers() == plugin.getConfig().getInt("max_team_size"))
        {
            InvitingPlayer.sendMessage(Messages.getString("Max_team_size"));

            return;
        }

        InvitingPlayer.sendMessage(to + " " + from);

        pendingInvites.put(to, from);
        InvitingPlayer.sendMessage(Messages.getString("Player_has_invited"));
        InvitedPlayer.sendMessage(Messages.getString("Player_have_invited_you").replace("%player%", InvitingPlayer.getName()));

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(plugin, () -> {
            if (!pendingInvites.containsKey(to))
                return;

            if (InvitingPlayer != null)
                InvitingPlayer.sendMessage(Messages.getString("Invitation_not_responded").replace("%player%", InvitedPlayer.getName()));
            if (InvitedPlayer != null)
                InvitedPlayer.sendMessage(Messages.getString("Invitation_expired"));

            pendingInvites.remove(to);
        }, 200L);
    }

    public static <K, V> void removeValue(Map<K, V> map, V valueToRemove) {
        Set<K> keysToRemove = new HashSet<>();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(valueToRemove)) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (K key : keysToRemove) {
            map.remove(key);
        }
    }
}
