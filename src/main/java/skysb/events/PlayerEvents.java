package skysb.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.util.BlockIterator;
import skysb.Utils.PlayerUtils;
import skysb.islands.Island;
import skysb.localization.Messages;
import skysb.skysb.SkySB;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class PlayerEvents implements Listener {
    SkySB plugin;

    public PlayerEvents(SkySB plugin)
    {
        this.plugin = plugin;
    }

    private boolean actionAllowed(Player player, Location location, Island.SettingsFlag flag) {
        if (player == null) {
            return actionAllowed(location, flag);
        }
        if (player.isOp() || player.hasPermission("skyblock.bypass")) {
            return true;
        }
        Island island = getIslandAt(location);
        if (island != null && (island.getFlag(flag) || island.members.contains(player.getUniqueId()))){
            return true;
        }
        return island == null;
    }

    private boolean actionAllowed(Location location, Island.SettingsFlag flag) {
        Island island = getIslandAt(location);
        if (island != null && island.getFlag(flag)){
            return true;
        }
        return island == null;
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent e)
    {
        UUID uuid = e.getPlayer().getUniqueId();

        Island is = Island.getPlayerIsland(e.getPlayer());

        if (is == null)
            return;

        is.lastConnectionAt = new Timestamp(System.currentTimeMillis());
        is.updateInDatabase();
    }

    @EventHandler
    public void OnPlayerLeave(PlayerQuitEvent e)
    {
        plugin.invitationManager.handleDisconnection(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void OnIslandEnter(IslandEnterEvent e)
    {

    }

    @EventHandler
    public void OnIslandExit(IslandExitEvent e)
    {

    }

    @EventHandler
    public void OnPlayerMove(PlayerMoveEvent e)
    {
        Player p = e.getPlayer();

        if (p.getWorld() != Island.skyWorld)
            return;

        if (p.isDead())
            return;

        if (e.getTo().getBlockX() - e.getFrom().getBlockX() == 0 && e.getTo().getBlockZ() - e.getFrom().getBlockZ() == 0)
            return;

        Island islandTo = getIslandAt(e.getTo());
        Island islandFrom = getIslandAt(e.getFrom());

        //TODO: Check lock de islandTo

        if (islandTo != null && islandFrom == null)
        {
            p.sendMessage("Vous entrez sur l'ile " + islandTo.name);

            final IslandEnterEvent event = new IslandEnterEvent(p.getUniqueId(), islandTo, e.getTo());
            plugin.getServer().getPluginManager().callEvent(event);
        }
        else if (islandFrom != null && islandTo == null)
        {
            p.sendMessage("Vous quittez l'ile " + islandFrom.name);

            final IslandExitEvent event = new IslandExitEvent(p.getUniqueId(), islandFrom, e.getFrom());
            plugin.getServer().getPluginManager().callEvent(event);
        }
        else if (islandTo != null && !islandTo.equals(islandFrom))
        {
            p.sendMessage("Vous quittez l'ile " + islandFrom.name);
            p.sendMessage("Vous entrez sur l'ile " + islandTo.name);

            final IslandExitEvent event = new IslandExitEvent(p.getUniqueId(), islandFrom, e.getFrom());
            plugin.getServer().getPluginManager().callEvent(event);

            final IslandEnterEvent event2 = new IslandEnterEvent(p.getUniqueId(), islandTo, e.getTo());
            plugin.getServer().getPluginManager().callEvent(event2);
        }
    }

    boolean inWorld(Player p)
    {
        return p.getWorld() == Island.skyWorld;
    }

    boolean inWorld(HumanEntity p)
    {
        return p.getWorld() == Island.skyWorld;
    }

    boolean inWorld(Entity entity) {
        return entity.getLocation().getWorld() == Island.skyWorld;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHorseInventoryClick(InventoryClickEvent e) {
        plugin.getLogger().info(e.getWhoClicked().toString());
        if (e.getInventory().getHolder() == null)
            return;
        if (!inWorld(e.getWhoClicked()))
            return;

        if (e.getInventory().getHolder() instanceof Animals) {
            if (actionAllowed((Player)e.getWhoClicked(), e.getWhoClicked().getLocation(), Island.SettingsFlag.HORSE_INVENTORY))
                return;

            ((Player) e.getWhoClicked()).getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerHitEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (!inWorld(p)) {
            return;
        }
        if (p.isOp() || p.hasPermission("skyblock.bypass"))
            return;

        if (PlayerUtils.playerIsHolding(p, Material.LEAD) && e.getRightClicked() != null) {

            if (e.getRightClicked() instanceof Horse) {
                boolean skellyZombieHorse = false;

                if (e.getRightClicked().getType().name().equals("ZOMBIE_HORSE") || e.getRightClicked().getType().name().equals("SKELETON_HORSE")) {
                    skellyZombieHorse = true;
                }

                if (!skellyZombieHorse) return;
            }
        }

        Island island = getIslandAt(e.getPlayer().getLocation());
        if (!Island.playerIsOnIsland(e.getPlayer())) {
            if (e.getRightClicked() != null && e.getRightClicked().getType().equals(EntityType.VILLAGER)) {
                if (island != null) {
                    if (!actionAllowed(e.getPlayer(), e.getRightClicked().getLocation(), Island.SettingsFlag.VILLAGER_TRADING)) {
                        p.sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if (PlayerUtils.playerIsHolding(p, Material.NAME_TAG) || p.getInventory().getItemInMainHand().getType().name().contains("DYE")
                    || p.getInventory().getItemInOffHand().getType().name().contains("DYE")) {
                p.sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
                e.getPlayer().updateInventory();
                return;
            }
            if (PlayerUtils.playerIsHolding(p, Material.COOKIE) && e.getRightClicked() instanceof Animals) {
                if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.HURT_MOBS)) {
                    Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                    e.setCancelled(true);
                    return;
                }
                if (island != null) {
                    if ((!island.getIgsFlag(SettingsFlag.HURT_MOBS) && !island.getMembers().contains(p.getUniqueId()))) {
                        Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            // Handle breeding
            if (e.getRightClicked() instanceof Animals) {
                for (ItemStack item : Util.getPlayerInHandItems(p)) {
                    Material type = item.getType();
                    if (type == Material.EGG || type == Material.WHEAT || type == Material.CARROT_ITEM || type == Material.SEEDS) {
                        if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.BREEDING)) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                            e.setCancelled(true);
                            return;
                        }
                        if (island != null) {
                            if ((!island.getIgsFlag(SettingsFlag.BREEDING) && !island.getMembers().contains(p.getUniqueId()))) {
                                Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
            switch (e.getRightClicked().getType()) {
                case CREEPER:
                    // This seems to be called when the player is in Creative mode...
                    if (!Settings.allowCreeperGriefing) {
                        for (ItemStack item : Util.getPlayerInHandItems(e.getPlayer())) {
                            if (item != null && item.getType().equals(Material.FLINT_AND_STEEL)) {
                                if (!island.getMembers().contains(e.getPlayer().getUniqueId())) {
                                    // Visitor
                                    litCreeper.add(e.getRightClicked().getUniqueId());
                                    if (DEBUG) {
                                        plugin.getLogger().info("DEBUG: visitor lit creeper");
                                    }
                                }
                            }
                        }
                    }
                    break;
                case LLAMA:
                case SKELETON_HORSE:
                case ZOMBIE_HORSE:
                case HORSE:
                    //plugin.getLogger().info("Horse riding");
                    if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.HORSE_RIDING)) {
                        Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                        e.setCancelled(true);
                        e.getPlayer().updateInventory();
                    }
                    if (island != null && !island.getIgsFlag(SettingsFlag.HORSE_RIDING)) {
                        Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                        e.setCancelled(true);
                        e.getPlayer().updateInventory();
                    }
                    break;
                case ITEM_FRAME:
                    // This is to place items in an item frame
                    if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                        Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                        e.setCancelled(true);
                    }
                    if (island != null) {
                        if (!island.getIgsFlag(SettingsFlag.PLACE_BLOCKS)) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                            e.setCancelled(true);
                        }
                    }
                    break;
                case MINECART_CHEST:
                case MINECART_FURNACE:
                case MINECART_HOPPER:
                    //plugin.getLogger().info("Minecarts");
                    if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.CHEST)) {
                        Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                        e.setCancelled(true);
                    }
                    if (island != null) {
                        if (!island.getIgsFlag(SettingsFlag.CHEST)) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                            e.setCancelled(true);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleDamageEvent(VehicleDamageEvent e) {
        if (inWorld(e.getVehicle())) {
            if (!(e.getAttacker() instanceof Player)) {
                return;
            }

            Island island = getIslandAt(e.getVehicle().getLocation());
            if (actionAllowed((Player)e.getAttacker(), e.getVehicle().getLocation(), Island.SettingsFlag.BREAK_BLOCKS))
                return;

            ((Player) e.getAttacker()).getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() == null)
            return;

        Island island = getIslandAt(e.getRightClicked().getLocation());
        if (island != null && e.getRightClicked() instanceof ArmorStand) {
            if (actionAllowed(e.getPlayer(), e.getRightClicked().getLocation(), Island.SettingsFlag.ARMOR_STAND))
                return;

            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }

        if (island != null && e.getRightClicked() instanceof Animals) {
            if (actionAllowed(e.getPlayer(), e.getRightClicked().getLocation(), Island.SettingsFlag.HORSE_INVENTORY))
                return;

            if (PlayerUtils.playerIsHolding(e.getPlayer(), Material.SADDLE))
            {
                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerLeashHitch(final HangingPlaceEvent e) {
        if (!inWorld(e.getPlayer()))
            return;

        if (e.getEntity() != null && e.getEntity().getType().equals(EntityType.LEASH_HITCH)) {
            if (actionAllowed(e.getPlayer(), e.getEntity().getLocation(), Island.SettingsFlag.LEASH))
                return;

            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
        if (!inWorld(e.getPlayer()))
            return;

        if (actionAllowed(e.getPlayer(),e.getBed().getLocation(), Island.SettingsFlag.BED))
            return;

        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeashUse(final PlayerLeashEntityEvent e) {
        if (inWorld(e.getEntity())) {
            if (e.getPlayer() != null) {
                Player player = e.getPlayer();

                if (actionAllowed(e.getPlayer(), e.getEntity().getLocation(), Island.SettingsFlag.LEASH))
                    return;

                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
                e.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeashUse(final PlayerUnleashEntityEvent e) {
        if (inWorld(e.getEntity())) {
            if (e.getPlayer() != null) {
                Player player = e.getPlayer();

                if (actionAllowed(e.getPlayer(), e.getEntity().getLocation(), Island.SettingsFlag.LEASH))
                    return;

                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
                e.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(final PlayerBucketFillEvent e) {
        if (inWorld(e.getPlayer())) {
            if (actionAllowed(e.getPlayer(), e.getPlayer().getLocation(), Island.SettingsFlag.COLLECT_LAVA) && e.getItemStack().getType().equals(Material.LAVA_BUCKET)) {
                return;
            }
            if (actionAllowed(e.getPlayer(), e.getPlayer().getLocation(), Island.SettingsFlag.COLLECT_WATER) && e.getItemStack().getType().equals(Material.WATER_BUCKET)) {
                return;
            }
            if (actionAllowed(e.getPlayer(), e.getPlayer().getLocation(), Island.SettingsFlag.MILKING) && e.getItemStack().getType().equals(Material.MILK_BUCKET)) {
                return;
            }
            if (actionAllowed(e.getPlayer(), e.getPlayer().getLocation(), Island.SettingsFlag.BUCKET)) {
                return;
            }

            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onShear(final PlayerShearEntityEvent e) {
        if (inWorld(e.getPlayer())) {
            if (actionAllowed(e.getPlayer(), e.getPlayer().getLocation(), Island.SettingsFlag.SHEARING)) {
                return;
            }

            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
        if (inWorld(e.getPlayer())) {
            Player p = e.getPlayer();

            if (actionAllowed(e.getPlayer(), e.getBlock().getLocation(), Island.SettingsFlag.BUCKET))
                return;

            if (e.getBlockClicked() != null) {
                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(final VehicleMoveEvent e) {
        if (!inWorld(e.getVehicle()))
            return;

        Entity passenger = e.getVehicle().getPassenger();
        if (!(passenger instanceof Player))
            return;

        Player p = (Player)passenger;

        Island islandTo = getIslandAt(e.getTo());
        Island islandFrom = getIslandAt(e.getFrom());

        //TODO: Check lock de islandTo

        if (islandTo != null && islandFrom == null)
        {
            p.sendMessage("Vous entrez sur l'ile " + islandTo.name);

            final IslandEnterEvent event = new IslandEnterEvent(p.getUniqueId(), islandTo, e.getTo());
            plugin.getServer().getPluginManager().callEvent(event);
        }
        else if (islandFrom != null && islandTo == null)
        {
            p.sendMessage("Vous quittez l'ile " + islandFrom.name);

            final IslandExitEvent event = new IslandExitEvent(p.getUniqueId(), islandFrom, e.getFrom());
            plugin.getServer().getPluginManager().callEvent(event);
        }
        else if (islandTo != null && !islandTo.equals(islandFrom))
        {
            p.sendMessage("Vous quittez l'ile " + islandFrom.name);
            p.sendMessage("Vous entrez sur l'ile " + islandTo.name);

            final IslandExitEvent event = new IslandExitEvent(p.getUniqueId(), islandFrom, e.getFrom());
            plugin.getServer().getPluginManager().callEvent(event);

            final IslandEnterEvent event2 = new IslandEnterEvent(p.getUniqueId(), islandTo, e.getTo());
            plugin.getServer().getPluginManager().callEvent(event2);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (inWorld(e.getPlayer())) {
            if (actionAllowed(e.getPlayer(), e.getBlock().getLocation(), Island.SettingsFlag.BREAK_BLOCKS))
                return;

            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
        if (!inWorld(e.getEntity()))
            return;

        Island island = getIslandAt(e.getEntity().getLocation());

        boolean inNether = e.getEntity().getWorld().equals(Island.enderSkyWorld);

        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if (p.isOp())
                return;
        }

        boolean flamingArrow = false;
        boolean projectile = false;
        Player attacker = null;
        if (e.getDamager() instanceof Player) {
            attacker = (Player)e.getDamager();
        } else if (e.getDamager() instanceof Projectile) {
            projectile = true;

            Projectile p = (Projectile) e.getDamager();
            if (p.getShooter() instanceof Player) {
                attacker = (Player) p.getShooter();
                if (p.getFireTicks() > 0) {
                    flamingArrow = true;
                }
            }
        }
        if (attacker == null)
            return;

        if (e.getEntity() instanceof Player && attacker.equals(e.getEntity()))
            return;

        if (e.getEntity() instanceof ItemFrame || e.getEntityType().toString().endsWith("STAND")) {
            if (actionAllowed(attacker, e.getEntity().getLocation(), Island.SettingsFlag.BREAK_BLOCKS))
                return;

            attacker.sendMessage(Messages.getString("Island_protected"));
            if (flamingArrow)
                e.getEntity().setFireTicks(0);
            if (projectile)
                e.getDamager().remove();
            e.setCancelled(true);
            return;
        }

        if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime || e.getEntity() instanceof Squid) {
            if (island != null && island.members.contains(attacker.getUniqueId()))
                return;

            if (actionAllowed(attacker, e.getEntity().getLocation(), Island.SettingsFlag.HURT_MONSTERS))
                return;

            attacker.sendMessage(Messages.getString("Island_protected"));
            if (flamingArrow)
                e.getEntity().setFireTicks(0);
            if (projectile)
                e.getDamager().remove();
            e.setCancelled(true);
            return;
        }

        if (e.getEntity() instanceof Animals || e.getEntity() instanceof IronGolem || e.getEntity() instanceof Snowman || e.getEntity() instanceof Villager) {
            if (actionAllowed(attacker, e.getEntity().getLocation(), Island.SettingsFlag.HURT_MOBS))
                return;

            attacker.sendMessage(Messages.getString("Island_protected"));
            if (flamingArrow)
                e.getEntity().setFireTicks(0);
            if (projectile)
                e.getDamager().remove();
            e.setCancelled(true);
            return;
        }

        boolean pvp = false;
        if ((inNether && actionAllowed(attacker, e.getEntity().getLocation(), Island.SettingsFlag.NETHER_PVP) ||
                (!inNether && actionAllowed(attacker, e.getEntity().getLocation(), Island.SettingsFlag.PVP))))
            pvp = true;

        if (e.getEntity() instanceof Player) {
            if (!pvp)
            {
                attacker.sendMessage(Messages.getString("Island_protected"));
                if (flamingArrow)
                    e.getEntity().setFireTicks(0);
                if (projectile)
                    e.getDamager().remove();
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void chorusTP(PlayerTeleportEvent e) {
        if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) && !actionAllowed(e.getPlayer(), e.getFrom(), Island.SettingsFlag.CHORUS_FRUIT))
        {
            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (e.getPlayer().getWorld() != Island.skyWorld)
            return;
        if (e.getPlayer().isOp())
            return;
        if ((e.getClickedBlock() != null && Island.locationIsOnIsland(e.getPlayer(), e.getClickedBlock().getLocation())))
            return;
        if (e.getClickedBlock() == null && (e.getMaterial() != null && Island.playerIsOnIsland(e.getPlayer())))
            return;

        Island island = getIslandAt(e.getPlayer().getLocation());

        if (e.getClickedBlock() != null) {
            try {
                BlockIterator iter = new BlockIterator(e.getPlayer(), 10);
                Block lastBlock = iter.next();
                while (iter.hasNext()) {
                    lastBlock = iter.next();
                    if (lastBlock.equals(e.getClickedBlock())) {
                        break;
                    }
                    if (lastBlock.getType().equals(Material.LEGACY_SKULL)) {
                        if (island != null) {
                            if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.BREAK_BLOCKS)) {
                                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                                e.setCancelled(true);
                                lastBlock.getState().update();
                                return;
                            }
                        }
                    } else if (lastBlock.getType().equals(Material.FIRE)) {
                        if (island != null) {
                            if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.FIRE_EXTINGUISH)) {
                                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (e.getClickedBlock().getType().toString().contains("SHULKER_BOX")) {
                if (island != null) {
                    e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                    e.setCancelled(true);
                }
                return;
            }
            if (e.getMaterial().equals(Material.FIREWORK_ROCKET) || e.getMaterial().equals(Material.FIREWORK_STAR)) {
                if (island != null) {
                    e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                    e.setCancelled(true);
                }
                return;
            }
            if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BONE_MEAL)) {
                if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.FERTILIZE)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG)) {
                if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.NAMETAG)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.EGG) ||
                    e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SNOWBALL)) {
                if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.EGGS)) {
                    e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                    e.setCancelled(true);
                    return;
                }
            }

            switch (e.getClickedBlock().getType()) {
                case CRIMSON_DOOR:
                case IRON_DOOR:
                case MANGROVE_DOOR:
                case OAK_DOOR:
                case WARPED_DOOR:
                case SPRUCE_DOOR:
                case ACACIA_DOOR:
                case DARK_OAK_DOOR:
                case BIRCH_DOOR:
                case JUNGLE_DOOR:
                case ACACIA_TRAPDOOR:
                case BIRCH_TRAPDOOR:
                case CRIMSON_TRAPDOOR:
                case DARK_OAK_TRAPDOOR:
                case IRON_TRAPDOOR:
                case JUNGLE_TRAPDOOR:
                case MANGROVE_TRAPDOOR:
                case OAK_TRAPDOOR:
                case SPRUCE_TRAPDOOR:
                case WARPED_TRAPDOOR:
                    if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                        if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.DOOR)) {
                            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    break;
                case CRIMSON_FENCE_GATE:
                case MANGROVE_FENCE_GATE:
                case OAK_FENCE_GATE:
                case WARPED_FENCE_GATE:
                case SPRUCE_FENCE_GATE:
                case ACACIA_FENCE_GATE:
                case DARK_OAK_FENCE_GATE:
                case BIRCH_FENCE_GATE:
                case JUNGLE_FENCE_GATE:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.GATE)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ENDER_CHEST:
                    break;
                case BARREL:
                case BEEHIVE:
                case FLOWER_POT:
                case LECTERN:
                case CHEST:
                case TRAPPED_CHEST:
                case DISPENSER:
                case DROPPER:
                case HOPPER:
                case HOPPER_MINECART:
                case CHEST_MINECART:
                case ACACIA_CHEST_BOAT:
                case SPRUCE_CHEST_BOAT:
                case BIRCH_CHEST_BOAT:
                case DARK_OAK_CHEST_BOAT:
                case OAK_CHEST_BOAT:
                case JUNGLE_CHEST_BOAT:
                case MANGROVE_CHEST_BOAT:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.CHEST)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case FARMLAND:
                case BIG_DRIPLEAF:
                case SMALL_DRIPLEAF:
                    if (e.getAction().equals(Action.PHYSICAL)) {
                        if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.CROP_TRAMPLE)) {
                            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    break;
                case BREWING_STAND:
                case CAULDRON:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.BREWING)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case BELL:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.BELL)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case REPEATER:
                case COMMAND_BLOCK:
                case COMMAND_BLOCK_MINECART:
                case CHAIN_COMMAND_BLOCK:
                case REPEATING_COMMAND_BLOCK:
                case DAYLIGHT_DETECTOR:
                case OBSERVER:
                case COMPARATOR:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.REDSTONE)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ENCHANTING_TABLE:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.ENCHANTING)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case COMPOSTER:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.COMPOST)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case FURNACE:
                case CAMPFIRE:
                case SMOKER:
                case BLAST_FURNACE:
                case FURNACE_MINECART:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.FURNACE)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ICE:
                    break;
                case ITEM_FRAME:
                    break;
                case JUKEBOX:
                case NOTE_BLOCK:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.MUSIC)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case PACKED_ICE:
                    break;
                case BIRCH_BUTTON:
                case ACACIA_BUTTON:
                case CRIMSON_BUTTON:
                case DARK_OAK_BUTTON:
                case JUNGLE_BUTTON:
                case MANGROVE_BUTTON:
                case OAK_BUTTON:
                case POLISHED_BLACKSTONE_BUTTON:
                case SPRUCE_BUTTON:
                case WARPED_BUTTON:
                case STONE_BUTTON:
                case LEVER:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.LEVER_BUTTON)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case TNT:
                    break;
                case CARTOGRAPHY_TABLE:
                case CRAFTING_TABLE:
                case FLETCHING_TABLE:
                case GRINDSTONE:
                case LOOM:
                case SMITHING_TABLE:
                case STONECUTTER:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.CRAFTING)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ANVIL:
                case CHIPPED_ANVIL:
                case DAMAGED_ANVIL:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.ANVIL)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case RAIL:
                case POWERED_RAIL:
                case DETECTOR_RAIL:
                case ACTIVATOR_RAIL:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.PLACE_BLOCKS)) {
                        if (e.getMaterial() == Material.MINECART || e.getMaterial() == Material.CHEST_MINECART || e.getMaterial() == Material.HOPPER_MINECART
                                || e.getMaterial() == Material.FURNACE_MINECART || e.getMaterial() == Material.COMMAND_BLOCK_MINECART
                                || e.getMaterial() == Material.TNT_MINECART) {
                            e.setCancelled(true);
                            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                            e.getPlayer().updateInventory();
                            return;
                        }
                    }
                    break;
                case BEACON:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.BEACON)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ACACIA_LOG:
                case BIRCH_LOG:
                case OAK_LOG:
                case DARK_OAK_LOG:
                case JUNGLE_LOG:
                case MANGROVE_LOG:
                case SPRUCE_LOG:
                case STRIPPED_ACACIA_LOG:
                case STRIPPED_BIRCH_LOG:
                case STRIPPED_DARK_OAK_LOG:
                case STRIPPED_JUNGLE_LOG:
                case STRIPPED_MANGROVE_LOG:
                case STRIPPED_OAK_LOG:
                case STRIPPED_SPRUCE_LOG:
                case CAKE:
                case DRAGON_EGG:
                    if (actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.BREAK_BLOCKS)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case GRAY_BED:
                case BLUE_BED:
                case BROWN_BED:
                case CYAN_BED:
                case GREEN_BED:
                case LIGHT_BLUE_BED:
                case LIME_BED:
                case LIGHT_GRAY_BED:
                case MAGENTA_BED:
                case BLACK_BED:
                case ORANGE_BED:
                case PINK_BED:
                case RED_BED:
                case PURPLE_BED:
                case WHITE_BED:
                case YELLOW_BED:
                    if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.BED) || e.getPlayer().getWorld().getEnvironment().equals(World.Environment.NETHER)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case CANDLE:
                case GRAY_CANDLE:
                case BLUE_CANDLE:
                case BROWN_CANDLE:
                case CYAN_CANDLE:
                case GREEN_CANDLE:
                case LIGHT_BLUE_CANDLE:
                case LIME_CANDLE:
                case LIGHT_GRAY_CANDLE:
                case MAGENTA_CANDLE:
                case BLACK_CANDLE:
                case ORANGE_CANDLE:
                case PINK_CANDLE:
                case RED_CANDLE:
                case PURPLE_CANDLE:
                case WHITE_CANDLE:
                case YELLOW_CANDLE:
                    if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.CANDLE)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case SPRUCE_SIGN:
                case SPRUCE_WALL_SIGN:
                case ACACIA_SIGN:
                case ACACIA_WALL_SIGN:
                case BIRCH_SIGN:
                case BIRCH_WALL_SIGN:
                case CRIMSON_SIGN:
                case CRIMSON_WALL_SIGN:
                case DARK_OAK_SIGN:
                case DARK_OAK_WALL_SIGN:
                case JUNGLE_SIGN:
                case JUNGLE_WALL_SIGN:
                case MANGROVE_SIGN:
                case MANGROVE_WALL_SIGN:
                case OAK_SIGN:
                case OAK_WALL_SIGN:
                case WARPED_SIGN:
                case WARPED_WALL_SIGN:
                    if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.COLOR_SIGN)) {
                        e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                default:
                    break;
            }
        }

        if (e.getMaterial().name().contains("BOAT") && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
            if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.PLACE_BLOCKS)) {
                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
            }
        } else if (e.getMaterial().equals(Material.ENDER_PEARL)) {
            if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.ENDER_PEARL)) {
                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
            }
        } else if (e.getMaterial().equals(Material.FLINT_AND_STEEL)) {
            if (e.getClickedBlock() != null) {
                if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.FIRE)) {
                    e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                    e.setCancelled(true);
                }
            }
        } else if (e.getMaterial().name().contains("SPAWN_EGG")) {
            if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.SPAWN_EGGS)) {
                e.getPlayer().sendMessage(Messages.getString("Island_protected"));
                e.setCancelled(true);
            }
        } else if (e.getMaterial().equals(Material.POTION)) {
            if (e.getMaterial().name().contains("SPLASH_POTION"))
                return;

            boolean inNether = e.getPlayer().getWorld().equals(Island.enderSkyWorld);

            if ((inNether && actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.NETHER_PVP)) ||
                    (!inNether && actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), Island.SettingsFlag.PVP)))
                return;

            e.getPlayer().sendMessage(Messages.getString("Island_protected"));
            e.setCancelled(true);
        }
    }

    private Island getIslandAt(Location loc) {
        float size = plugin.getConfig().getInt("islands_space");
        float playerX = ((loc.getBlockX() + size / 2) / size);
        float playerZ = ((loc.getBlockZ() + size / 2) / size);

        int playerXint = 0;
        int playerZint = 0;

        if (playerX < 0)
            playerXint = (int)(playerX - 1) * (int)size;
        else
            playerXint = (int)(playerX) * (int)size;

        if (playerZ < 0)
            playerZint = (int)(playerZ - 1) * (int)size;
        else
            playerZint = (int)(playerZ) * (int)size;

        return plugin.islandsCache.getIsland(playerXint, playerZint);
    }
}
