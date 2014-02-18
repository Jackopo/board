package de.htw.ds.board;

import de.sb.javase.TypeMetadata;


/**
 * Universal pieces are abstract definitions of pieces, i.e. they maintain no relation to board
 * position, and the same piece can be used multiple times on the same board. The downside to this
 * is that such pieces cannot pre-calculate geometrically valid move positions, but only relative
 * motion directions.
 * @param <T> the piece type
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface UniversalPiece<T extends PieceType> extends Piece<T> {

	/**
	 * Returns the move vectors, whose entries consist of rank and file increments relative to a
	 * piece's current position. Note that move vectors do not contain any information about move
	 * restrictions or special move modes. They solely describe the direction of single motion. Note
	 * that each signed 8bit rank and file distance pair is packed into a 16bit value (rank
	 * high-byte, file low-byte) to speed up processing, and avoid problems with the cloning of
	 * multi-dimensional arrays. These values can be unpacked using
	 * {@code rankIncrement = (byte) (direction >> 8)} and {@code fileIncrement = (byte) direction}.
	 * @return the (packed) move vectors
	 */
	short[] getMoveVectors ();
}