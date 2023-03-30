package skysb.skysb;

import org.bukkit.plugin.java.JavaPlugin;
import skysb.Utils.ThreadManager;
import skysb.caches.IslandsCache;
import skysb.command.IslandCommand;
import skysb.database.MySQLDatabase;
import skysb.events.PlayerEvents;
import skysb.islands.Island;
import skysb.managers.DeletionManager;
import skysb.managers.InvitationManager;
import yelloxwind.skyplayer.SkyPlayer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class SkySB extends JavaPlugin {
    public static SkySB Instance;

    public MySQLDatabase db;
    public ThreadManager threadManager;
    public IslandsCache islandsCache;
    public InvitationManager invitationManager;
    public DeletionManager deletionManager;
    public SkyPlayer skyPlayer;

    public void setupIslandsTable()
    {
        ResultSet resultSet = db.executeQuery("SHOW TABLES LIKE 'islands'");
        try {
            if (resultSet.next()) {
                return;
            }

            getLogger().info("Missing table islands in database, creating it...");

            db.executeUpdate("CREATE TABLE `skyblock`.`islands` (`id` INT NOT NULL , `name` VARCHAR(50) NOT NULL," +
                    "`level` INT NOT NULL, `owner` VARCHAR(36) NOT NULL , `members` VARCHAR(370) NOT NULL , `center` VARCHAR(30) NOT NULL," +
                    "`settings` VARCHAR(255) NOT NULL, `flags` VARCHAR(255) NOT NULL, `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "`modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, `last_connection_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`)) ENGINE = InnoDB;");

            getLogger().info("Created !");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        Instance = this;

        db = new MySQLDatabase("127.0.0.1", "skyblock", "3306", "root", "");
        setupIslandsTable();

        skyPlayer = SkyPlayer.getPlugin();

        islandsCache = new IslandsCache(this);
        islandsCache.loadAllIsland();

        threadManager = new ThreadManager(this);
        invitationManager = new InvitationManager(this);
        deletionManager = new DeletionManager(this);

        Island.setupSkyblockWorld(getConfig().getString("world_name"));

        getCommand("is").setExecutor(new IslandCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);

        getLogger().info("Plugin started");
    }

    @Override
    public void onDisable() {
        db.close();

        getLogger().info("Plugin stopped");
    }
}
