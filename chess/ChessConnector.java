package de.htw.ds.board.chess;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import de.htw.ds.shop.ShopConnector;
import de.sb.javase.sql.JdbcConnectionMonitor;

public class ChessConnector implements AutoCloseable {
	
	static private final String SQL_INSERT_MOVE = "INSERT INTO chess.openingMove VALUES (0, ?, ?, ?, ?, ?)";
	static private final String SQL_SELECT_MOVES = "SELECT * FROM chess.openingMove where position=? AND source=?  AND sink=? AND rating=? and searchDepth=?";
	private final Connection connection;
	static private final DataSource DATA_SOURCE;
	static {
		try {
			final Properties properties = new Properties();
			try (InputStream byteSource = ChessConnector.class.getResourceAsStream("chess-mysql.properties")) {
				properties.load(byteSource);
			}

			final Class<?> dataSourceClass = Class.forName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource", true, Thread.currentThread().getContextClassLoader());
			final DataSource dataSource = (DataSource) dataSourceClass.newInstance();
			dataSourceClass.getMethod("setURL", String.class).invoke(dataSource, properties.get("connectionUrl"));
			dataSourceClass.getMethod("setCharacterEncoding", String.class).invoke(dataSource, properties.get("characterEncoding"));
			dataSourceClass.getMethod("setUser", String.class).invoke(dataSource, properties.get("alias"));
			dataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, properties.get("password"));
			DATA_SOURCE = dataSource;
		} catch (final Exception exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}
	
	
	public ChessConnector() throws SQLException {
		this.connection = DATA_SOURCE.getConnection();

		final Runnable connectionMonitor = new JdbcConnectionMonitor(this.connection, "select null", 60000);
		final Thread thread = new Thread(connectionMonitor, "jdbc-connection-monitor");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void close() throws Exception {
		this.connection.close();

	}

	public boolean createDatabse() {
		
		return false;
	}

}
