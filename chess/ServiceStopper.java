package de.htw.ds.board.chess;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import de.htw.ds.TypeMetadata;
import de.sb.javase.io.SocketAddress;



@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class ServiceStopper {

	private static final String PROTOCOL_IDENTIFIER = "CSP";
	
	private final InetSocketAddress serviceAddress;


	
	public ServiceStopper (final InetSocketAddress serviceAddress) {
		if (serviceAddress == null) throw new NullPointerException();
		this.serviceAddress = serviceAddress;
	}
	
	private void stop(final String password) throws IOException {
		try (Socket connection = this.getConnection())  {
			final DataInputStream dataSource = new DataInputStream(connection.getInputStream());
			final DataOutputStream dataSink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
			
			dataSink.writeUTF(password);
			dataSink.flush();
			
			String response = dataSource.readUTF(); 
			
			switch(response) {
			case "ok":
				System.out.println("Server shuts down! The response was " +response );
				break;
			case "fail":
				System.out.println("Server didn't shut down! The response was "+response);
				break;				
			default:
				System.out.println("Unknown response: " + response);
			}
		} 
	}
	
	private Socket getConnection () throws IOException {
		final Socket connection = new Socket(this.serviceAddress.getHostName(), this.serviceAddress.getPort());
		try {
			final DataInputStream dataSource = new DataInputStream(connection.getInputStream());
			final DataOutputStream dataSink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

			dataSink.writeChars(PROTOCOL_IDENTIFIER);
			dataSink.flush();
			for (final char character : PROTOCOL_IDENTIFIER.toCharArray()) {
				if (dataSource.readChar() != character) throw new ProtocolException();
			}
			return connection;
		} catch (final Exception exception) {
			try {
				connection.close();
			} catch (final Exception nestedException) {
				exception.addSuppressed(nestedException);
			}
			throw exception;
		}
	}
	
	public static void main(final String[] args) throws IOException {
		
		final InetSocketAddress serviceAddress = new SocketAddress(args[0]).toInetSocketAddress();
		
		final ServiceStopper client = new ServiceStopper(serviceAddress);
		
		final String password = args[1];
				
		try {
			client.stop(password);			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
}