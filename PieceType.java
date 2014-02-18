package de.htw.ds.board;

import de.sb.javase.TypeMetadata;


/**
 * Declares interface common to any kind of board based piece type, addressing base valuation and
 * textual representation.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface PieceType {

	/**
	 * Returns this type's identifying ordinal, a positive number.
	 * @return the ordinal
	 * @see Enum#ordinal()
	 */
	int ordinal ();


	/**
	 * Returns this type's identifying name, an uppercase text.
	 * @return the ordinal
	 * @see Enum#name()
	 */
	String name ();


	/**
	 * Returns this type's unique character alias.
	 * @return the character alias
	 */
	char getAlias ();


	/**
	 * Returns this type's base rating in cents. Note that the rating is expected to be positive.
	 * @return the rating
	 */
	int getRating ();


	/**
	 * Returns the single move vectors, with each entry consisting of rank and file increments
	 * relative to a piece's current location. Note that such move vectors do not contain any
	 * information about move restrictions or special moves. They solely describe the direction of
	 * single motion. Note that each 8bit rank and file distance pair is packed into a 16bit value
	 * (rank high-byte, file low-byte) to speed up processing.
	 * @return the (packed and relative) single move directions
	 */
	short[] getSingleMoveVectors ();


	/**
	 * Returns the continuous move vectors, with each entry indicating repeatable rank and file
	 * increments relative to a piece's current location. Note that such move vectors do not contain
	 * any information about move restrictions or special moves. They solely describe the direction
	 * of continuous motion. Note that each 8bit rank and file distance pair is packed into a 16bit
	 * value (rank high-byte, file low-byte) to speed up processing.
	 * @return the (packed and relative) continuous move directions
	 */
	short[] getContinuousMoveVectors ();
}