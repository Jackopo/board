package de.htw.ds.board;

import de.sb.javase.TypeMetadata;


/**
 * Positional pieces are concrete definitions of pieces, i.e. they have a relation to a board
 * dimension, and their position within such boards. Such a piece can only be used for a specific
 * position on matching boards. However, the advantage of such pieces is that they can pre-calculate
 * absolute move directions that are geometrically valid, which is a much faster approach compared
 * to using relative move directions.<br />
 * This is especially so since, even with the best available algorithms, sorting out geometrically
 * invalid moves during move analysis involves excessive amounts of if/else decisions, whose
 * randomness renders them practically unpredictable by modern pipeline based CPU architectures,
 * which in turn causing bad performance because of excessive amounts of branch prediction failures.
 * @param <T> the piece type
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface PositionalPiece<T extends PieceType> extends Piece<T> {
	static public final short MASK_POSITION = (short) 0x3FFF;


	/**
	 * Returns the number of board ranks this piece is related to. Positional pieces must only be
	 * used with boards that feature a corresponding number of ranks.
	 * @return the rank count
	 */
	byte getRankCount ();


	/**
	 * Returns the number of board files this piece is related to. Positional pieces must only be
	 * used with boards that feature a corresponding number of files.
	 * @return the file count
	 */
	byte getFileCount ();


	/**
	 * Returns the piece's position on any board with matching dimensions. Note that positions are
	 * zero-based, starting from the bottom-left corner, and incrementing file-wise. In conjunction
	 * with the file count, the position can be used to calculate the rank and file of a piece, by
	 * calculating {@code rank = position / fileCount} and {@code file = position % fileCount}.
	 * @return the position
	 */
	short getPosition ();


	/**
	 * Returns the geometrically valid sink positions, with each entry consisting of positions
	 * stemming from subsequent steps of continuous movement. This implies that non-continuous kinds
	 * of movement will contain exactly one sink position per geometrically valid move direction.
	 * Note that masking the positions with additional information is possible because the maximum
	 * position is {@code 127*127} as ranks and files are positive byte values, which is {@code 14}
	 * bits long. This leaves the leading two bits to carry game specific meta information about
	 * moves, allowing much faster access compared to using composite objects for the same purpose.
	 * However, it implies that the positions need to be unmasked using
	 * {@code position = maskedPositon & MASK_POSITION} before use.
	 * @return the (masked) sink positions
	 */
	short[][] getSinkPositions ();


	/**
	 * Returns the sink position bit-board indicating all positions reachable within a single move.
	 * Basically, the resulting bit board contains all the sink positions returned by
	 * {@linkplain #getSinkPositions()} in the form {@code 1 << sinkPositon}, but without
	 * information about move direction. This renders them useless for move generation, as pieces
	 * may be blocked in certain directions; however, it does allow for a speedy tests if a piece
	 * can move to a given position at all, by simply testing for the presence or absence of the bit
	 * matching the position in question.
	 * @return the sink position bit-board
	 */
	long[] getSinkPositionBitBoard ();
}