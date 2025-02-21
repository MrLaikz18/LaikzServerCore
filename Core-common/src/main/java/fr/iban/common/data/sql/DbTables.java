package fr.iban.common.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DbTables {

    public static void createTables() {
        createplayersTable();
        createAnnounceBLTable();
        createIpTable();
        createOptionsTable();
        createIgnoredPlayersTable();
        createOnlinePlayersTable();
		createTrustedPlayersTable();
		createTrustedCommandsTable();
    }

    /*
     * Création de la table
     */
    private static void createplayersTable() {
        createTable("CREATE TABLE IF NOT EXISTS sc_players (" +
                "  id          int auto_increment PRIMARY KEY," +
                "  uuid    varchar(36)  not null," +
                "  name        varchar(16)  not null," +
                "  date_created timestamp default now()," +
                "  lastseen       bigint DEFAULT 0," +
                "  maxclaims smallint DEFAULT 5," +
                "  CONSTRAINT  UC_sc_players" +
                "  UNIQUE (id)," +
                "  CONSTRAINT UC_sc_players_uuid" +
                "  UNIQUE (uuid)" +
                ") engine = InnoDB;");
    }

    private static void createOptionsTable() {
        createTable("CREATE TABLE IF NOT EXISTS sc_options (" +
                "  id int," +
                "  idOption varchar(16)," +
                "  CONSTRAINT PK_sc_options" +
                "  PRIMARY KEY (id, idOption)," +
                "  CONSTRAINT FK_sc_options" +
                "  FOREIGN KEY (id) REFERENCES sc_players(id)" +
                ") engine = InnoDB;");
    }

    private static void createAnnounceBLTable() {
        createTable("CREATE TABLE IF NOT EXISTS sc_annonces_blacklist (" +
                "  id int," +
                "  idAnnonce int," +
                "  CONSTRAINT PK_sc_annonces_blacklist" +
                "  PRIMARY KEY (id, idAnnonce)," +
                "  CONSTRAINT FK_sc_annonces" +
                "  FOREIGN KEY (id) REFERENCES sc_players(id)" +
                ") engine = InnoDB;");
    }

    private static void createIgnoredPlayersTable() {
        createTable("CREATE TABLE IF NOT EXISTS sc_ignored_players (" +
                "  id int," +
                "  uuidPlayer varchar(36) not null," +
                "  CONSTRAINT PK_sc_ignored_players" +
                "  PRIMARY KEY (id, uuidPlayer)," +
                "  CONSTRAINT FK_sc_ignored" +
                "  FOREIGN KEY (id) REFERENCES sc_players(id)" +
                ") engine = InnoDB;");
    }

    private static void createIpTable() {
        createTable("CREATE TABLE IF NOT EXISTS sc_logins (" +
                "  id int," +
                "  date_time DATETIME," +
                "  ip VARCHAR(45)," +
                "  CONSTRAINT PK_sc_logins" +
                "  PRIMARY KEY (id, date_time)," +
                "  CONSTRAINT FK_sc_logins" +
                "  FOREIGN KEY (id) REFERENCES sc_players(id)" +
                ") engine = InnoDB;");
    }

    private static void createOnlinePlayersTable() {
        createTable("CREATE TABLE IF NOT EXISTS sc_online_players (" +
                "  player_id INTEGER PRIMARY KEY," +
                "  FOREIGN KEY (player_id) REFERENCES sc_players(id)" +
                ");");
    }

    private static void createTrustedPlayersTable() {
        createTable("CREATE TABLE IF NOT EXISTS sc_trusted_players (" +
                "  uuid varchar(36)," +
                "  ip VARCHAR(45)," +
				"  date_time DATETIME DEFAULT NOW()," +
				"  PRIMARY KEY (uuid, ip)" +
                ") engine = InnoDB;");
    }

	private static void createTrustedCommandsTable() {
		createTable("CREATE TABLE IF NOT EXISTS sc_trusted_commands (" +
				"  command VARCHAR(255)," +
				"  senderType VARCHAR(50)," +
				"  `context` VARCHAR(50)," +
                "  PRIMARY KEY (command, senderType, context)" +
                ") engine = InnoDB;");
	}


	private static void createTable(String statement) {
        try (Connection connection = DbAccess.getDataSource().getConnection()) {
            try (PreparedStatement preparedStatemente = connection.prepareStatement(statement)) {
                preparedStatemente.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
