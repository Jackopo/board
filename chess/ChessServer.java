package de.htw.ds.board.chess;


import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
	


	private  final static String PROTOCOL_IDENTIFIER = "CSP";
	private  static boolean stop = false;


	public ChessServer(final int servicePort, final String serviceName) throws SQLException, IOException {
		super();


		if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		this.jdbcConnector = new ChessConnector();
		/* for the START TRANSACTION command in jdbc */
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


	private static void stopServer( final ServerSocket serverSocket, final int stopPort, final String stopPassword) throws Throwable {
		//final ExecutorService executor = Executors.newFixedThreadPool(1);
		try {
			final Socket connection;

			try {
				connection = serverSocket.accept();
			} catch (SocketException e) {
				e.printStackTrace();
				return;
			}
			try {
				final Runnable runnable = new Runnable() {
					public void run() {
						try {
							
							final DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
							final DataInputStream dataInputStream = new DataInputStream(connection.getInputStream());

							// verify and acknowledge protocol
							for (final char character : PROTOCOL_IDENTIFIER.toCharArray()) {
								if (dataInputStream.readChar() != character) throw new ProtocolException();
							}
							dataOutputStream.writeChars(PROTOCOL_IDENTIFIER);
							dataOutputStream.flush();

							final String password = dataInputStream.readUTF();

							if (stopPassword.equals(password)) {
								stop = true;
								dataOutputStream.writeUTF("ok");
								dataOutputStream.flush();
							}else {
								dataOutputStream.writeUTF("fail");
								dataOutputStream.flush();
							}
							
						} catch (Throwable e) {
							e.printStackTrace();
							try { e.printStackTrace(); } catch (final Throwable nestedException) {}
						} 
					}
				};

				
				//executor.execute(runnable);
				
				Thread t = new Thread(runnable, "stop");
				t.setDaemon(false);
				t.start();
				
				while (t.isAlive());
				

			} catch (final Throwable exception) {
				exception.printStackTrace();
				try { exception.printStackTrace(); } catch (final Throwable nestedException) {
					nestedException.printStackTrace();
				}
				try { connection.close(); } catch (final Throwable nestedException) {
					nestedException.printStackTrace();
				}
				throw exception;
			}
//			executor.shutdown();
//			executor.awaitTermination(10L, TimeUnit.MILLISECONDS);

		} catch (final Throwable exception) {
			exception.printStackTrace();
		}		
	}

	/**
	 * @param args
	 * @throws Throwable 
	 */
	public static void main(String[] args) throws Throwable {

		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final String serviceName = args[1];
		final int stopPort = Integer.parseInt(args[2]);
		final String stopPassword = args[3];

		try (ChessServer server = new ChessServer(servicePort, serviceName)) {

			ServerSocket serverSocket = new ServerSocket(stopPort);
			System.out.println("Dynamic (bottom-up) JAX-WS shop server running");
			System.out.format("Service URI is \"%s\".\n", server.getServiceURI());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);
		
			do {				
				stopServer(serverSocket, stopPort, stopPassword);
			} while (!stop);
			

			if(stop) {
				System.out.println("Houston we are stopping!");
				System.out.format("Server received shutdown request on port %s.\n", stopPort);
				System.out.println("Shutting down....\n");
				server.close();
				System.exit(0);
			} else {
				System.out.println("not good");
			}
		}
	}

}
