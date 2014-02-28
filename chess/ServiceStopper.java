package de.htw.ds.board.chess;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import de.htw.ds.TypeMetadata;


/**
 * <p>Console based client class using a custom protocol based service.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class ServiceStopper {

	private static final String PROTOCOL_IDENTIFIER = "CSP";

	private final InetSocketAddress serviceAddress;


	/**
	 * Public constructor.
	 * @param stub the chat service stub
	 * @throws NullPointerException if the given stub is null
	 */
	public ServiceStopper(final InetSocketAddress serviceAddress) {
		super();
		if (serviceAddress == null) throw new NullPointerException();
		this.serviceAddress = serviceAddress;
     }


	/**
	 * Adds a chat entry to the chat service.
	 * @throws IOException 
	 */
	public void stopService(String password) throws IOException {
		try (Socket connection = this.getConnection()) {
			final DataInputStream source = new DataInputStream(connection.getInputStream());
			final DataOutputStream sink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

			sink.writeUTF(password);
			sink.flush();
			String response = source.readUTF(); 

			if(response.equals("ok")){
				System.out.println("Chess server is shutting down! (response: "+response+")");
			}else if(response.equals("fail")){
				System.out.println("Chess server rejected shut down request! (response: "+response+")");
			}else{
				System.out.println("Unknown response from chess server! (response: "+response+")");
			}
		} 
	}


	/**
	 * Returns a new TCP connection to the server after verifying the
	 * protocol identifier.
	 * @return the TCP connection
	 * @throws IOException if an I/O related problem occurs
	 */
	private Socket getConnection() throws IOException {
		final Socket connection = new Socket(this.serviceAddress.getHostName(), this.serviceAddress.getPort());
		final DataInputStream source = new DataInputStream(connection.getInputStream());
		final DataOutputStream sink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

		sink.writeChars(PROTOCOL_IDENTIFIER);
		sink.flush();
		for (final char character : PROTOCOL_IDENTIFIER.toCharArray()) {
			if (source.readChar() != character) throw new ProtocolException();
		}
		return connection;
	}
	

	/**
	 * Application entry point. The given runtime parameters must be a socket-address.
	 * @param args the given runtime arguments
	 * @throws IOException if there is an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {
		
		final InetSocketAddress serviceAddress = new de.sb.javase.io.SocketAddress(args[0]).toInetSocketAddress();
		final ServiceStopper client = new ServiceStopper(serviceAddress);
		client.stopService(args[1]);
	
	}
}