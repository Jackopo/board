package de.htw.ds.board.chess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPBinding;
import de.htw.ds.board.MovePrediction;
import de.sb.javase.io.SocketAddress;

@WebService(endpointInterface="de.htw.ds.board.chess.ChessService", serviceName="ChessService")
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
	
	public URI getServiceURI () {
		return this.serviceURI;
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

	
	public void putMovePrediction( String xfen, short searchDepth, MovePrediction movePrediction)  {
		
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
				
			}
		}

	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) throws SQLException, URISyntaxException, IOException {
		
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final String serviceName = args[1];


		try (ChessServer server = new ChessServer(servicePort, serviceName)) {
			System.out.println("Dynamic (bottom-up) JAX-WS shop server running, enter \"quit\" to stop.");
			System.out.format("Service URI is \"%s\".\n", server.getServiceURI());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		}
	}

}
