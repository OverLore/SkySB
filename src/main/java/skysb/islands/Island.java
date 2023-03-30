package skysb.islands;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import skysb.caches.IslandsCache;
import skysb.database.MySQLDatabase;
import skysb.localization.Messages;
import skysb.skysb.SkySB;
import skysb.world.EmptyChunkGenerator;

import java.io.File;
import java.sql.*;
import java.util.*;

import static skysb.Utils.VectorUtils.StringToVector;
import static skysb.Utils.VectorUtils.VectorToString;

public class Island {
    private static MySQLDatabase db = SkySB.Instance.db;

    public int id;
    public String name;
    public int level;
    public UUID owner;
    public List<UUID> members;
    public Vector center;
    public String settings;
    private HashMap<SettingsFlag, Boolean> flags = new HashMap<>();
    public Timestamp createdAt;
    public Timestamp modifiedAt;
    public Timestamp lastConnectionAt;
    public static World skyWorld;
    public static World enderSkyWorld;

    public static Island CreateIslandForPlayer(Player player)
    {
        Island is = new Island();

        is.id = getFirstAvailableID();
        is.level = 0;
        is.name = "Ile de " + player.getName();
        is.owner = player.getUniqueId();
        is.members = new ArrayList<>();
        is.center = getCenterFromID(is.id);
        is.settings = "settings []";
        is.flags = is.getFlagsDefaults();
        is.createdAt = new Timestamp(System.currentTimeMillis());
        is.modifiedAt = new Timestamp(System.currentTimeMillis());
        is.lastConnectionAt = new Timestamp(System.currentTimeMillis());

        is.addInDatabase();

        return is;
    }

    public boolean getFlag(SettingsFlag flag)
    {
        return flags.get(flag);
    }

    public static boolean locationIsOnIsland(Player p, Location loc)
    {
        Island is = Island.getPlayerIsland(p);

        if (is == null)
            return false;

        int size = SkySB.Instance.getConfig().getInt("islands_max_size");

        return (loc.getBlockX() > is.center.getBlockX() - size && loc.getBlockX() < is.center.getBlockX() + size &&
                loc.getBlockZ() > is.center.getBlockZ() - size && loc.getBlockZ() < is.center.getBlockZ() + size);
    }

    public static boolean playerIsOnIsland(Player p)
    {
        Island is = Island.getPlayerIsland(p);

        if (is == null)
            return false;

        int size = SkySB.Instance.getConfig().getInt("islands_max_size");

        return (p.getLocation().getBlockX() > is.center.getBlockX() - size && p.getLocation().getBlockX() < is.center.getBlockX() + size &&
                p.getLocation().getBlockZ() > is.center.getBlockZ() - size && p.getLocation().getBlockZ() < is.center.getBlockZ() + size);
    }

    public void addInDatabase()
    {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement("INSERT INTO islands (id, name, level, owner, members," +
                    "center, settings, flags, created_at, modified_at, last_connection_at) VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setInt(3, level);
            ps.setString(4, owner.toString());
            ps.setString(5, getStringFromMembers(members));
            ps.setString(6, VectorToString(center));
            ps.setString(7, settings);
            ps.setString(8, flagsToString(flags));
            ps.setTimestamp(9, createdAt);
            ps.setTimestamp(10, modifiedAt);
            ps.setTimestamp(11, lastConnectionAt);

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Island loadIsland(ResultSet rs)
    {
        try {
            Island is = new Island();

            is.lastConnectionAt = rs.getTimestamp("last_connection_at");
            is.modifiedAt = rs.getTimestamp("modified_at");
            is.createdAt = rs.getTimestamp("created_at");
            is.center = StringToVector(rs.getString("center"));
            is.settings = rs.getString("settings");
            is.flags = stringToFlags(rs.getString("flags"));
            is.members = getMembersFromString(rs.getString("members"));
            is.owner = UUID.fromString(rs.getString("owner"));
            is.name = rs.getString("name");
            is.level = rs.getInt("level");
            is.id = rs.getInt("id");

            return is;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateInDatabase()
    {
        modifiedAt = new Timestamp(System.currentTimeMillis());

        try {
            PreparedStatement ps = db.getConnection().prepareStatement("UPDATE islands SET name=?,level=?,owner=?,members=?,center=?,settings=?,flags=?,modified_at=?,last_connection_at=? WHERE id = ?;");

            ps.setString(1, name);
            ps.setInt(2, level);
            ps.setString(3, owner == null ? "" : owner.toString());
            ps.setString(4, getStringFromMembers(members));
            ps.setString(5, VectorToString(center));
            ps.setString(6, settings);
            ps.setString(7, flagsToString(flags));
            ps.setTimestamp(8, modifiedAt);
            ps.setTimestamp(9, lastConnectionAt);
            ps.setInt(10, id);

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static String flagsToString(HashMap<SettingsFlag, Boolean> _flags)
    {
        String s = "";

        int i = 0;
        for (Boolean b : _flags.values())
            s += (b ? "1" : "0");

        return s;
    }

    static HashMap<SettingsFlag, Boolean> stringToFlags(String s)
    {
        HashMap<SettingsFlag, Boolean> fl = new HashMap<>();

        int i = 0;
        for (SettingsFlag flag : SettingsFlag.values()) {
            fl.put(flag, s.charAt(i) == '1');
            i++;
        }

        return fl;
    }

    public static boolean hasPlayerAnIsland(Player player)
    {
        try {
            return db.executeQuery("SELECT * FROM islands WHERE owner = '" + player.getUniqueId() + "' OR members LIKE '%" + player.getUniqueId() + "%';").next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static Vector getCenterFromID(int id)
    {
        List<Integer> vals = getLoopParameters(id);

        int minmax = vals.get(0);
        int passes = vals.get(1);
        int spaceinBlock = SkySB.Instance.getConfig().getInt("islands_space");

        int k = 0;
        for (int i = -minmax; i <= minmax; i++)
        {
            for (int j = -minmax; j <= minmax; j++)
            {
                if (!(i == -minmax || i == minmax || j == -minmax || j == minmax))
                    continue;

                k++;

                if (k == passes)
                    return new Vector(i * spaceinBlock, 0, j * spaceinBlock);
            }
        }

        return new Vector(0, 0, 0);
    }

    private static List<Integer> getLoopParameters(int id)
    {
        if (id == 0)
            return Arrays.asList(0, 1);

        int minmax = 1;
        while (true)
        {
            int meh = (int)(Math.pow((minmax) * 2 + 1, 2));
            int trueBlockCount = (int)(Math.pow((minmax - 1) * 2 + 1, 2));

            if (id < meh)
                return Arrays.asList(minmax, id - trueBlockCount + 1);

            if (minmax >= 1000)
                return Arrays.asList(-1, -1);

            minmax++;
        }
    }

    public static int getPlayerIslandId(Player player)
    {
        try {
            ResultSet rs = db.executeQuery("SELECT * FROM islands WHERE owner = '" + player.getUniqueId() + "' OR members LIKE '%" + player.getUniqueId() + "%';");

            if (!rs.next())
                return -1;

            return rs.getInt("id");
        } catch (SQLException e) {
            return -1;
        }
    }

    public static Island getPlayerIsland(Player player)
    {
        return getPlayerIsland(player.getUniqueId());
    }

    public static Island getPlayerIsland(UUID uuid)
    {
        return SkySB.Instance.islandsCache.getIsland(uuid);
    }

    private static String getStringFromMembers(List<UUID> uuids)
    {
        String s = "";

        for (int i = 0; i < uuids.size(); i++)
        {
            s += uuids.get(i);

            if (i < uuids.size() - 1)
                s += "|";
        }

        return s;
    }

    private static List<UUID> getMembersFromString(String s)
    {
        List<UUID> uuids = new ArrayList<>();

        if (s.isEmpty())
            return uuids;

        for (String uuid : s.split("[|]]"))
        {
            uuids.add(UUID.fromString(uuid));
        }

        return uuids;
    }

    private static int getFirstAvailableID()
    {
        ResultSet rs = db.executeQuery("SELECT COALESCE(MIN(t1.id) + 1, 1) AS first_unused_id FROM islands t1 LEFT JOIN islands t2 ON t1.id + 1 = t2.id WHERE t2.id IS NULL;");

        try {
            if(rs.next())
                return rs.getInt("first_unused_id");

            return 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isOwnerOnline()
    {
        return Bukkit.getPlayer(owner) != null;
    }

    public boolean isOwner(Player p)
    {
        return p.getUniqueId().equals(owner);
    }

    public boolean isOwner(UUID uuid)
    {
        return uuid.equals(owner);
    }

    public List<Player> getOnlineMembers(boolean ignoreOwner)
    {
        List<Player> ps = new ArrayList<>();

        for (UUID uuid : members)
        {
            Player p = Bukkit.getPlayer(uuid);

            if (p != null)
                ps.add(p);
        }

        if (!ignoreOwner)
        {
            Player ownerP = Bukkit.getPlayer(owner);

            if (ownerP != null)
                ps.add(ownerP);
        }

        return ps;
    }

    public void addPlayerToIsland(UUID uuid)
    {
        members.add(uuid);

        SkySB.Instance.islandsCache.addMemberToIsland(this, uuid);

        updateInDatabase();
    }

    public static void teleportToIsland(Player playerToTeleport, Player playerOwnerOfTargetIsland)
    {
        Island is = SkySB.Instance.islandsCache.getIsland(playerOwnerOfTargetIsland);

        playerToTeleport.teleport(new Location(skyWorld, is.center.getBlockX() + .5, is.center.getBlockY(), is.center.getBlockZ() +.5, 0, 0));
    }

    public void teleportTo(Player playerToTeleport)
    {
        playerToTeleport.teleport(new Location(skyWorld, center.getBlockX() + .5, center.getBlockY(), center.getBlockZ() +.5, 0, 0));
    }

    public static void setupSkyblockWorld(String worldName) {
        skyWorld = Bukkit.getWorld(worldName);

        if (skyWorld == null) {
            WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new EmptyChunkGenerator());
            skyWorld = wc.createWorld();
        }
    }

    public void delete()
    {
        owner = null;
        members = new ArrayList<>();

        updateInDatabase();
    }

    public int getAmountOfMembers()
    {
        return members.size() + 1;
    }

    public static boolean areFromSameIsland(UUID uuid1, UUID uuid2)
    {
        Island uuid1Island = getPlayerIsland(uuid1);
        Island uuid2Island = getPlayerIsland(uuid2);

        if (uuid1Island == null || uuid2Island == null)
            return false;

        return uuid1Island == uuid2Island;
    }

    public static HashMap<SettingsFlag, Boolean> getFlagsDefaults() {
        HashMap<SettingsFlag, Boolean> flags = new HashMap<>();

        flags.put(SettingsFlag.ANVIL, false);
        flags.put(SettingsFlag.ARMOR_STAND, false);
        flags.put(SettingsFlag.BEACON, false);
        flags.put(SettingsFlag.BED, false);
        flags.put(SettingsFlag.BELL, false);
        flags.put(SettingsFlag.BREAK_BLOCKS, false);
        flags.put(SettingsFlag.BREEDING, true);
        flags.put(SettingsFlag.BREWING, false);
        flags.put(SettingsFlag.BUCKET, false);
        flags.put(SettingsFlag.CANDLE, false);
        flags.put(SettingsFlag.COLLECT_LAVA, false);
        flags.put(SettingsFlag.COLLECT_WATER, false);
        flags.put(SettingsFlag.COLOR_SIGN, false);
        flags.put(SettingsFlag.COMPOST, false);
        flags.put(SettingsFlag.CHEST, false);
        flags.put(SettingsFlag.CHORUS_FRUIT, false);
        flags.put(SettingsFlag.CRAFTING, true);
        flags.put(SettingsFlag.CROP_TRAMPLE, false);
        flags.put(SettingsFlag.DOOR, false);
        flags.put(SettingsFlag.EGGS, false);
        flags.put(SettingsFlag.ENCHANTING, true);
        flags.put(SettingsFlag.ENDER_PEARL, false);
        flags.put(SettingsFlag.FERTILIZE, true);
        flags.put(SettingsFlag.FIRE, false);
        flags.put(SettingsFlag.FIRE_EXTINGUISH, false);
        flags.put(SettingsFlag.FURNACE, false);
        flags.put(SettingsFlag.GATE, false);
        flags.put(SettingsFlag.HORSE_INVENTORY, false);
        flags.put(SettingsFlag.HORSE_RIDING, false);
        flags.put(SettingsFlag.HURT_MOBS, false);
        flags.put(SettingsFlag.HURT_MONSTERS, true);
        flags.put(SettingsFlag.LEASH, false);
        flags.put(SettingsFlag.LEVER_BUTTON, false);
        flags.put(SettingsFlag.MILKING, true);
        flags.put(SettingsFlag.MUSIC, false);
        flags.put(SettingsFlag.NAMETAG, false);
        flags.put(SettingsFlag.NETHER_PVP, false);
        flags.put(SettingsFlag.PLACE_BLOCKS, false);
        flags.put(SettingsFlag.PORTAL, true);
        flags.put(SettingsFlag.PRESSURE_PLATE, false);
        flags.put(SettingsFlag.PVP, false);
        flags.put(SettingsFlag.REDSTONE, false);
        flags.put(SettingsFlag.SPAWN_EGGS, false);
        flags.put(SettingsFlag.SHEARING, false);
        flags.put(SettingsFlag.VILLAGER_TRADING, true);
        flags.put(SettingsFlag.VISITOR_ITEM_DROP, true);
        flags.put(SettingsFlag.VISITOR_ITEM_PICKUP, false);

        return flags;
    }

    public enum SettingsFlag {
        ANVIL,
        ARMOR_STAND,
        BEACON,
        BED,
        BELL,
        BREAK_BLOCKS,
        BREEDING,
        BREWING,
        BUCKET,
        CANDLE,
        COLLECT_LAVA,
        COLLECT_WATER,
        COLOR_SIGN,
        COMPOST,
        CHEST,
        CHORUS_FRUIT,
        CRAFTING,
        CROP_TRAMPLE,
        DOOR,
        EGGS,
        ENCHANTING,
        ENDER_PEARL,
        FERTILIZE,
        FIRE,
        FIRE_EXTINGUISH,
        FURNACE,
        GATE,
        HORSE_INVENTORY,
        HORSE_RIDING,
        HURT_MOBS,
        HURT_MONSTERS,
        LEASH,
        LEVER_BUTTON,
        MILKING,
        MUSIC,
        NAMETAG,
        NETHER_PVP,
        PLACE_BLOCKS,
        PORTAL,
        PRESSURE_PLATE,
        PVP,
        REDSTONE,
        SPAWN_EGGS,
        SHEARING,
        VILLAGER_TRADING,
        VISITOR_ITEM_DROP,
        VISITOR_ITEM_PICKUP
    }
}
