package de.htw.ds.board;

import de.sb.javase.TypeMetadata;


/**
 * Declares interface common to any kind of board piece, bundling information about color and type.
 * Note that implementors of this class may or may not be positional, i.e. aware of their position
 * on their board. This interface itself makes no presumptions onto this design choice, but leaves
 * this to the two sub- interfaces contained herein. Implementors therefore should either implement
 * one or the other of those two, in order to reflect the design choice taken.
 * @param <T> the piece type
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface Piece<T extends PieceType> {

	/**
	 * Returns the ordinal which uniquely identifies the piece within it's universe. It is used to
	 * address the piece within caches, for example in enums. Note that this method's name has been
	 * chosen to conform to {@linkplain java.lang.Enum}, as enums are possible implementors of this
	 * interface. The downside is that name does not conform to the JavaBeans specification, and is
	 * therefore hard to reflect as an accessor.
	 * @return the ordinal
	 */
	int ordinal ();


	/**
	 * Returns the name which uniquely identifies this piece within it's universe. Note that this
	 * method's name has been chosen to conform to {@linkplain java.lang.Enum}, as enums are
	 * possible implementors of this interface. The downside is that name does not conform to the
	 * JavaBeans specification, and is therefore hard to reflect as an accessor.
	 * @return the name
	 */
	String name ();


	/**
	 * Returns the character alias which uniquely identifies this piece's type and color.
	 * @return the character alias
	 */
	char getAlias ();


	/**
	 * Returns whether or not this piece is white.
	 * @return {@code true} if this piece is white, {@code false} otherwise
	 */
	boolean isWhite ();


	/**
	 * Returns the piece type.
	 * @return the type
	 */
	T getType ();


	/**
	 * Returns the piece rating in cents. With universal pieces, the rating will be adjusted for
	 * piece color only, i.e. positive for white pieces, and negative for black ones. With
	 * positional pieces, the rating will be adjusted for both piece color and piece position.
	 * @return the rating
	 */
	int getRating ();
}