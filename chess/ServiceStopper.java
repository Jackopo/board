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

	public static void main(final String[] args) throws IOException {
		
		final InetSocketAddress serviceAddress = new SocketAddress(args[0]).toInetSocketAddress();
		
		final Socket connection = new Socket(serviceAddress.getHostName(), serviceAddress.getPort());
		try {
			final DataInputStream dataSource = new DataInputStream(connection.getInputStream());
			final DataOutputStream dataSink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

			dataSink.writeChars(PROTOCOL_IDENTIFIER);
			dataSink.flush();
			
			for (final char character : PROTOCOL_IDENTIFIER.toCharArray()) {
				if (dataSource.readChar() != character) throw new ProtocolException();
			}


			dataSink.writeUTF(args[1]);
			dataSink.flush();
			
			String response = dataSource.readUTF(); 

			if(response.equals("ok")){
				System.out.println("Server shuts down! The response was " +response );
			}else if(response.equals("fail")){
				System.out.println("Server didn't shut down! The response was "+response);
			}
		} catch (final Exception exception) {
			try {
				connection.close();
			} catch (final Exception nestedException) {
				exception.addSuppressed(nestedException);
			}
			throw exception;
		} finally {
			try {
				connection.close();
			} catch (final Exception exception) {
				exception.addSuppressed(exception);
			}
		}
	
	}
}