package de.htw.ds.board.chess;

import de.htw.ds.board.PieceType;
import de.sb.javase.TypeMetadata;


/**
 * Defines the piece types allowed for a chess game. The static valuation of pieces is based loosely
 * on {@code Hans Berliner, "The System: A World Champion's Approach to Chess", 1999}.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public enum ChessPieceType implements PieceType {
	PAWN ('P', 100, new byte[][] { { 1, -1 }, { 1, 0 }, { 1, 1 } }, new byte[0][]),
	KING ('K', 10000, new byte[][] { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 } }, new byte[0][]),
	KNIGHT ('N', 320, new byte[][] { { 1, -2 }, { 2, -1 }, { 2, 1 }, { 1, 2 }, { -1, 2 }, { -2, 1 }, { -2, -1 }, { -1, -2 } }, new byte[0][]),
	BISHOP ('B', 330, new byte[0][], new byte[][] { { 1, -1 }, { 1, 1 }, { -1, 1 }, { -1, -1 } }),
	ROOK ('R', 510, new byte[0][], new byte[][] { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } }),
	QUEEN ('Q', 880, new byte[0][], new byte[][] { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 }, { 1, -1 }, { 1, 1 }, { -1, 1 }, { -1, -1 } }),
	ARCHBISHOP ('A', 750, KNIGHT.singleMoveVectors, BISHOP.continuousMoveVectors),
	CHANCELLOR ('C', 800, KNIGHT.singleMoveVectors, ROOK.continuousMoveVectors),
	EMPRESS ('E', 1000, KNIGHT.singleMoveVectors, QUEEN.continuousMoveVectors);

	private final char alias;
	private final int rating;
	private final short[] singleMoveVectors;
	private final short[] continuousMoveVectors;


	/**
	 * Creates a new instance.
	 * @param alias the character alias
	 * @param rating the base rating in cents
	 * @param singleMoveDirections the (unpacked) single move directions
	 * @param continuousMoveDirections the (unpacked) continuous move directions
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given rating is negative
	 */
	private ChessPieceType (final char alias, final int rating, final byte[][] singleMoveDirections, final byte[][] continuousMoveDirections) {
		this(alias, rating, pack(singleMoveDirections), pack(continuousMoveDirections));
	}


	/**
	 * Creates a new instance.
	 * @param alias the character alias
	 * @param rating the base rating in cents
	 * @param singleMoveDirections the (packed) single move directions
	 * @param continuousMoveDirections the (packed) continuous move directions
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given rating is negative
	 */
	private ChessPieceType (final char alias, final int rating, final short[] singleMoveDirections, final short[] continuousMoveDirections) {
		if (singleMoveDirections == null | continuousMoveDirections == null) throw new NullPointerException();
		if (rating <= 0) throw new IllegalArgumentException();

		this.alias = alias;
		this.rating = rating;
		this.singleMoveVectors = singleMoveDirections;
		this.continuousMoveVectors = continuousMoveDirections;
	}


	/**
	 * {@inheritDoc}
	 */
	public char getAlias () {
		return this.alias;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRating () {
		return this.rating;
	}


	/**
	 * {@inheritDoc}
	 */
	public short[] getSingleMoveVectors () {
		return this.singleMoveVectors.clone();
	}


	/**
	 * {@inheritDoc}
	 */
	public short[] getContinuousMoveVectors () {
		return this.continuousMoveVectors.clone();
	}


	/**
	 * Returns a 16bit based equivalent for the given 8bit based motion motionDirections, packing
	 * rank and file into single values (rank high-byte, file low-byte).
	 * @param motionDirections the 8bit based motion motionDirections
	 * @return the 16bit based motion motionDirections
	 */
	static private short[] pack (final byte[][] motionDirections) {
		final short[] result = new short[motionDirections.length];
		for (int index = 0; index < result.length; ++index) {
			result[index] = (short) ((motionDirections[index][0] << 8) | (motionDirections[index][1] & 0xFF));
		}
		return result;
	}


	/**
	 * Returns a chess type for the given character alias.
	 * @param alias the character alias
	 * @return the associated type
	 * @throws IllegalArgumentException if the given alias is illegal
	 */
	static public ChessPieceType valueOf (final char alias) {
		switch (alias) {
			case 'P':
				return ChessPieceType.PAWN;
			case 'N':
				return ChessPieceType.KNIGHT;
			case 'B':
				return ChessPieceType.BISHOP;
			case 'R':
				return ChessPieceType.ROOK;
			case 'A':
				return ChessPieceType.ARCHBISHOP;
			case 'C':
				return ChessPieceType.CHANCELLOR;
			case 'Q':
				return ChessPieceType.QUEEN;
			case 'E':
				return ChessPieceType.EMPRESS;
			case 'K':
				return ChessPieceType.KING;
			default:
				throw new IllegalArgumentException();
		}
	}
}