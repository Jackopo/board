package de.htw.ds.board;

import java.util.Collection;
import de.sb.javase.TypeMetadata;


/**
 * Abstract interface for checker-board games played by two opponents, on boards of varying size.
 * Note that this interface does not make any assumptions on the game type except that it has to be
 * a game played between two sides, and on a board whose rank and file count does not exceed
 * {@code 127}.
 * @param <T> the type of the board's pieces
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface Board<T extends PieceType> extends Cloneable {

	/**
	 * Returns the rating that represents a finished game, won by white; the corresponding finished
	 * game, won by black, is represented with the negative of this value.
	 */
	static public int WIN = Integer.MAX_VALUE;

	/**
	 * Returns the rating that represents a finished drawn game.
	 */
	static public int DRAW = Integer.MIN_VALUE;


	/**
	 * Returns a clone of this board.
	 * @return the clone
	 */
	Board<T> clone ();


	/**
	 * Returns the number of ranks on the board.
	 * @return the rank count
	 */
	byte getRankCount ();


	/**
	 * Returns the number of files on the board.
	 * @return the file count
	 */
	byte getFileCount ();


	/**
	 * Returns the number of (existing) pieces on the board.
	 * @return the piece count
	 */
	short getPieceCount ();


	/**
	 * Returns the total number of (half) moves performed.
	 * @return the move clock
	 */
	short getMoveClock ();


	/**
	 * Returns the number of (half) moves performed since the last irreversible move, i.e. the
	 * latest one where the former board state cannot be recovered by simply moving back.
	 * @return the reversible move clock
	 */
	byte getReversibleMoveClock ();


	/**
	 * Returns whether or not white is active
	 * @return {@code true} if white is active, {@code false} otherwise
	 */
	boolean isWhiteActive ();


	/**
	 * Returns all valid moves of the active side. Each move is an array of positions in visiting
	 * order, including the source position
	 * @return the moves
	 */
	Collection<short[]> getActiveMoves ();


	/**
	 * Returns the piece at the given position, or {@code null} if the given position is empty.
	 * @param position the board position
	 * @return the piece, or {@code null}
	 * @throws IllegalArgumentException if the given position is out of range
	 */
	Piece<T> getPiece (short position);


	/**
	 * If anyMode is {@code false}, returns all positions where matching pieces are located, or an
	 * empty array if there is no matching piece. Otherwise returns the first matching position, or
	 * an empty array if there is none.
	 * @param any whether or not to return any or all matching positions
	 * @param white {@code true} for white pieces, {@code false} for black pieces, or {@code null}
	 *        for any color
	 * @param type the type of the pieces, or {@code null} for any type
	 * @return the matching board positions
	 */
	short[] getPositions (boolean any, Boolean white, T type);


	/**
	 * Returns the current board rating in cents, with matching sides having a rating of zero by
	 * definition. The following values have a special meaning:
	 * <ul>
	 * <li>+{@linkplain #WIN} indicates white has won</li>
	 * <li>{@linkplain #DRAW} indicates the game ended in a draw</li>
	 * <li>-{@linkplain #WIN} indicates black has won</li>
	 * </ul>
	 * @return the current board rating
	 */
	int getRating ();


	/**
	 * Returns whether or not the passive side can capture a (possibly temporal) piece at the given
	 * position with it's next move. Note that this method may be based on a reverse motion check,
	 * in opposition to {@linkplain #getActiveMoves()}, which may provide improved performance in
	 * analysis operations.
	 * @param position the position
	 * @return whether or not the given position can be captured by the passive side within it's
	 *         following counter move
	 * @throws IllegalArgumentException if the given position is out of range
	 */
	boolean isPositionThreatened (short position);


	/**
	 * Moves a piece from the first to the last of the given move's positions, visiting the
	 * in-between positions on the way (if present), and modifying the board in the process. Note
	 * that this method does not check the validity of the given move, only that there is an active
	 * piece to be moved.
	 * @param move the move as an array of positions in visiting order, including the source
	 *        position
	 * @throws NullPointerException if the given move is {@code null}
	 * @throws IllegalArgumentException if the move consists of less than two positions or more than
	 *         permitted, or if any position is out of range, or if the first position is empty, or
	 *         if the piece to be moved is not active
	 */
	void performMove (short... move);


	/**
	 * Resets this board's content to the given X-FEN like text representation, or to the typical
	 * piece layout for it's dimensions if none is given.
	 * @param textRepresentation an X-FEN text representation, or {@code null} for default
	 * @throws IllegalStateException if the given text represents a board with incompatible
	 *         dimensions, or if no text is given and there is no typical piece layout for this
	 *         board's current dimensions
	 */
	void reset (String textRepresentation);


	/**
	 * Returns a game neutral character based matrix representation of this board's pieces.
	 * @return the character matrix
	 */
	char[][] toCharacters ();


	/**
	 * Returns an X-FEN like representation of this board's current state.
	 * @return the X-FEN board representation
	 */
	String toString ();
}