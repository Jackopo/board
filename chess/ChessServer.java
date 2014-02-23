package de.htw.ds.board.chess;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPBinding;
import de.htw.ds.board.MovePrediction;
import de.sb.javase.io.SocketAddress;

@WebService (endpointInterface="de.htw.bs.board.chess.ChessService", serviceName="ChessService")
public class ChessServer implements ChessService, AutoCloseable {
	
	private final URI serviceURI;
	private final Endpoint endpoint;
	private final ChessConnector jdbcConnector;
	
	
	public ChessServer(final int servicePort, final String serviceName) throws SQLException {
		super();
		
		
		if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		this.jdbcConnector = new ChessConnector();
		this.jdbcConnector.getConnection().setAutoCommit(false);

		
		// important
		this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
		this.endpoint.publish(this.serviceURI.toASCIIString());
		
	}

	/**
	 * Closes the receiver, thereby stopping it's JDBC connector and JAX-WS endpoint.
	 * @throws SQLException 
	 */

	public void close () throws SQLException {
		try {
			this.endpoint.stop();
		} catch (final Throwable exception) {};
		this.jdbcConnector.close();
	}

	@Override
	public MovePrediction[] getMovePredictions(String xfen,short searchDepth) throws SQLException {
		
		
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				final MovePrediction[] movePredictions = this.jdbcConnector.getMovePredictions(xfen,searchDepth);
				this.jdbcConnector.getConnection().commit();
				return movePredictions;
			} catch (final Exception exception) {
				try {
					this.jdbcConnector.getConnection().rollback();
				} catch (final Exception nestedException) {
					exception.addSuppressed(nestedException);
				}
				throw exception;
			}
		}
		
	}

	
	public void putMovePrediction( String xfen, short searchDepth, MovePrediction movePrediction) throws SQLException {
		
		synchronized (this.jdbcConnector.getConnection()) {
			try {
				this.jdbcConnector.putMovePrediction(xfen,searchDepth, movePrediction);
				this.jdbcConnector.getConnection().commit();

			} catch (final Exception exception) {
				try {
					this.jdbcConnector.getConnection().rollback();
				} catch (final Exception nestedException) {
					exception.addSuppressed(nestedException);
				}
				throw exception;
			}
		}

	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
