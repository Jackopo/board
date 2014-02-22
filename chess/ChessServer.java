package de.htw.ds.board.chess;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPBinding;

import de.htw.ds.board.MovePrediction;
import de.htw.ds.shop.ShopConnector;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.sql.JdbcConnectionMonitor;

@WebService (endpointInterface="de.htw.bs.board.chess.ChessService", serviceName="ChessService")
public class ChessServer implements ChessService, AutoCloseable {
	
	private final URI serviceURI;
	private final Endpoint endpoint;
	
	
	public ChessServer(final int servicePort, final String serviceName) {
		super();
		/*
		 *   check if there is a database
		 *   if not create one and create this table:
		 *   CREATE TABLE OpeningMove (
		 		identity BIGINT AUTO_INCREMENT,
				position VARCHAR(128) NOT NULL,
				source SMALLINT NOT NULL,
				sink SMALLINT NOT NULL,
				rating BIGINT NOT NULL,
				searchDepth TINYINT NOT NULL,
				PRIMARY KEY (identity),
				UNIQUE KEY (position, source, sink)
				) ENGINE=InnoDB;

		 *   
		 */
		
		if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		//this.jdbcConnector = new ShopConnector();
		//this.jdbcConnector.getConnection().setAutoCommit(false);

		
		// important
		this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
		this.endpoint.publish(this.serviceURI.toASCIIString());
		
	}

	@Override
	public void close() throws Exception {
		
	}

	@Override
	public MovePrediction[] getMovePredictions(String xfen,short searchDepth) {
		MovePrediction[] movePredictions;
		
		
		return movePredictions;
	}

	@Override
	public void putMovePrediction( String xfen, short searchDepth, MovePrediction movePrediction) {
		// TODO Auto-generated method stub

	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
