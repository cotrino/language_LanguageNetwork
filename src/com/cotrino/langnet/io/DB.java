package com.cotrino.langnet.io;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.sqlite.SQLiteJDBCLoader;

public class DB {

	final static Logger logger = LoggerFactory.getLogger(DB.class);

	protected Connection conn;
	protected Statement stat;

	public DB(String databaseName) {

		File dbFile = new File(databaseName);
		if (!dbFile.exists()) {
			logger.error("Database " + dbFile + " not found! It will be created.");
		}

		try {
			Class.forName("org.sqlite.JDBC");
			String connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
			conn = DriverManager.getConnection(connectionString);
			stat = conn.createStatement();
			/*
			logger.debug(String.format(connectionString
					+ " running in %s mode with driver v%s",
					SQLiteJDBCLoader.isNativeMode() ? "native" : "pure-java",
					SQLiteJDBCLoader.getVersion()));
			*/
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void enableOptimizations() {
		try {
			stat.execute("PRAGMA cache_size = 100000;");
			stat.execute("PRAGMA count_changes = 0;");
			stat.execute("PRAGMA auto_vacuum = none;");
			stat.execute("PRAGMA fullfsync = 0;");
			stat.execute("PRAGMA locking_mode = NORMAL;");
			stat.execute("PRAGMA synchronous = OFF;");
			stat.execute("PRAGMA default_temp_store = MEMORY;");
			stat.execute("PRAGMA temp_store = MEMORY;");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void executeUpdate(String query) throws SQLException {
		stat.executeUpdate(query);
	}

	public ResultSet executeQuery(String query) throws SQLException {
		//logger.debug(query);
		return stat.executeQuery(query);
	}

	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
		}
	}

	public String getString(String query) {
		String result = "";
		try {
			ResultSet rs = executeQuery(query);
			if (rs.next()) {
				result = rs.getString(1);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}

	public String getStringList(String query) {
		String result = "";
		try {
			ResultSet rs = executeQuery(query);
			while (rs.next()) {
				if (!result.equals("")) {
					result += ",";
				}
				result += rs.getString(1);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}

	public long getLong(String query) {
		int result = 0;
		try {
			ResultSet rs = executeQuery(query);
			if (rs.next()) {
				result = rs.getInt(1);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}

	public int getInt(String query) {
		int result = 0;
		try {
			ResultSet rs = executeQuery(query);
			if (rs.next()) {
				result = rs.getInt(1);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}

	public boolean exists(String query) {
		boolean result = false;
		try {
			ResultSet rs = executeQuery(query);
			if (rs.next()) {
				result = true;
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}

	public long insert(String query) {
		try {
			executeUpdate(query);
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
		return getLastInsertedId();
	}

	public void update(String query) {
		try {
			executeUpdate(query);
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public long getLastInsertedId() {
		return getLong("SELECT last_insert_rowid();");
	}

	public void compact() {
		System.out.print("Compacting...");
		String query = "VACUUM;";
		try {
			executeUpdate(query);
			System.out.println("[OK]");
		} catch (SQLException e) {
			System.err.println("Query failed: " + query);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public PreparedStatement massiveInsertPrepare(String query)
			throws SQLException {
		return conn.prepareStatement(query);
	}

	public void massiveInsertExecute(PreparedStatement prep)
			throws SQLException {
		conn.setAutoCommit(false);
		prep.executeBatch();
		conn.setAutoCommit(true);
	}

}