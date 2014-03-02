package de.htw.ds.board.chess;

import java.io.InputStream;


import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.sql.Connection;
import java.sql.PreparedStatement;

import de.htw.ds.board.MovePrediction;



import de.sb.javase.sql.JdbcConnectionMonitor;

public class ChessConnector implements AutoCloseable {

	static private final String SQL_INSERT_MOVE = "INSERT INTO OpeningMove (position, source, sink, rating, searchDepth) VALUES ( ?, ?, ?, ?, ?)";
	static private final String SQL_SELECT_BETTER_OR_EQUAL_MOVES = "SELECT * FROM OpeningMove WHERE position=? AND searchDepth >= ? ORDER BY identity";
	static private final String SQL_SELECT_WORST_MOVES = "SELECT * FROM OpeningMove WHERE position=? AND searchDepth < ?";
	static private final String SQL_DELETE_MOVE = "DELETE FROM OpeningMove WHERE identity=?";
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

	/**
	 * Returns the JDBC connection to allow external synchronization and transaction handling.
	 * @return the JDBC connection
	 */
	public Connection getConnection () {
		return this.connection;
	}

	@Override
	public void close() throws SQLException {
		this.connection.close();
	}

	public MovePrediction[] getMovePredictions (String xfen, short searchDepth) throws SQLException {
		if ((xfen == null) || (searchDepth < 1 )) {
			return null;
		}

		List<MovePrediction> movePredictions = new ArrayList<MovePrediction>();

		synchronized(this.getConnection())  {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_BETTER_OR_EQUAL_MOVES)) {
				statement.setString(1, xfen);
				statement.setInt(2, searchDepth);

				try (ResultSet resultSet = statement.executeQuery()) {
					List<short[]> moves = new ArrayList<short[]>();
					while(resultSet.next()) {						
						moves.add(new short[]{resultSet.getShort("source"), resultSet.getShort("sink")});
					}
					 
					if (moves.size() > 0) {
						MovePrediction movePrediction = new MovePrediction(resultSet.getInt("rating"));
						movePrediction.getMoves().addAll(moves);
						movePredictions.add(movePrediction);					
						
					}
				}

			}
		}		
		if (movePredictions.size() == 0) {
			MovePrediction[] mp = {};
			return mp;
		} else {

			MovePrediction[] mp = movePredictions.toArray(new MovePrediction[movePredictions.size()]);
			return mp;
		}

	}


	public void putMovePrediction( String xfen, short searchDepth, MovePrediction movePrediction) {
		// null or value-ranges not good
		if ((xfen == null) || (searchDepth < 1 ) || (movePrediction == null)) {
			return;
		}
		
		
		synchronized(this.connection)  {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_WORST_MOVES)) {
				statement.setString(1, xfen);
				statement.setInt(2, searchDepth);

				try (ResultSet resultSet = statement.executeQuery()) {
					while(resultSet.next()) {
						deleteMovePrediction(resultSet.getLong("identity"));						
					}					
				}					
			} catch (SQLException e) {
			
				e.printStackTrace();
			}
		}

		for (short[] move : movePrediction.getMoves()) {
			synchronized (this.connection) {
				try (PreparedStatement statement = this.connection.prepareStatement(SQL_INSERT_MOVE)) {
					
					statement.setString(1, xfen);
					statement.setShort(2, move[0]);
					statement.setShort(3, move[1]);
					statement.setInt(4, movePrediction.getRating());
					statement.setInt(5, searchDepth);
					if(statement.executeUpdate() != 1)  throw new IllegalStateException();				
				} catch (SQLException e) {
					e.printStackTrace();
				} catch(IllegalStateException e) {
					System.out.println("failed to insert move!");
				}
			}
		}

	}




	public long deleteMovePrediction(long identity)  {
				
		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_DELETE_MOVE)) {
				statement.setLong(1, identity);
				
				if (statement.executeUpdate() != 1) throw new IllegalStateException();
			} catch ( SQLException e) {
				e.printStackTrace(); 
			} catch(IllegalStateException e) {
				System.out.println("moveprediction removal failed.");
			}
			
		}

		return identity;
	}


}
