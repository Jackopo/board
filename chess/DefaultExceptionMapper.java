package de.htw.ds.board.chess;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import de.sb.javase.TypeMetadata;


/**
 * JAX-RS Exception mapper that additionally logs any kinds of marshaling errors.
 */
@Provider
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "1.0.0", authors = "Sascha Baumeister")
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

	/**
	 * {@inheritDoc}
	 */
	public Response toResponse (final Throwable exception) {
		Logger.getGlobal().log(Level.INFO, exception.getMessage(), exception);

		return Response.status(500).build();
	}
}