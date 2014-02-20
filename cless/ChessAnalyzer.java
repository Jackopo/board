package de.htw.ds.board.cless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import de.htw.ds.board.Analyzer;
import de.htw.ds.board.Board;
import de.htw.ds.board.MovePrediction;
import de.htw.ds.board.Piece;
import de.sb.javase.TypeMetadata;


/**
 * Instances of this class serve as analyzers for chess games. Any subclass of this class must be
 * stateless in the sense that it's operations must not change it's state, as a given analyzer
 * instance may be used by multiple board instances at the same time. Additionally, any subclass is
 * required to provide a public default constructor to allow instance creation using the Java
 * reflection API.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class ChessAnalyzer implements Analyzer<ChessPieceType> {

	/**
	 * {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	public MovePrediction predictMoves (final Board<ChessPieceType> board, final int depth) {
		return predictMovesSingleThreaded(board, depth);
	}


	/**
	 * Recursively analyzes the valid moves and counter moves up until the given search depth
	 * beginning with the given board's active color, and implementing the minimax game theory
	 * principle. The result contains the next {@code depth} (half) moves predicted given optimum
	 * play from both sides, and the board rating after performing said moves. Note that this
	 * implementation is single-threaded.
	 * @param board the board
	 * @param depth the search depth in half moves
	 * @return the prediction for the next {@code depth} (half) moves, and the board rating after
	 *         performing said moves
	 * @throws NullPointerException if the given board is {@code null}
	 * @throws IllegalArgumentException if the given depth is negative
	 */
	static protected final MovePrediction predictMovesSingleThreaded (final Board<ChessPieceType> board, final int depth) {
		if (depth <= 0) throw new IllegalArgumentException();

		final boolean whiteActive = board.isWhiteActive();
		final List<MovePrediction> alternatives = new ArrayList<>();

		final Collection<short[]> moves = board.getActiveMoves();
		for (final short[] move : moves) {
			// create and calculated the  MovePrediction
			final MovePrediction movePrediction;
			final Piece<ChessPieceType> capturePiece = board.getPiece(move[1]);
			if (capturePiece != null && capturePiece.isWhite() != whiteActive && capturePiece.getType() == ChessPieceType.KING) {
				movePrediction = new MovePrediction(whiteActive ? +Board.WIN : -Board.WIN);
				movePrediction.getMoves().add(move);
			} else {
				final Board<ChessPieceType> boardClone = board.clone();
				boardClone.performMove(move);

				if (depth > 1) {
					// perform single threaded recursive analysis, but filter move sequences resulting in own king's capture
					movePrediction = predictMovesSingleThreaded(boardClone, depth - 1);
					final short[] counterMove = movePrediction.getMoves().get(0);
					if (counterMove != null) {
						final Piece<ChessPieceType> recapturePiece = boardClone.getPiece(counterMove[1]);
						if (recapturePiece != null && recapturePiece.isWhite() == whiteActive && recapturePiece.getType() == ChessPieceType.KING) continue;
					}
					movePrediction.getMoves().add(0, move);
				} else {
					movePrediction = new MovePrediction(boardClone.getRating());
					movePrediction.getMoves().add(move);
				}
			}
			// the just now created MovePrediction is compared with the best alternative
			final int comparison = compareMovePredictions(whiteActive, movePrediction, alternatives.isEmpty() ? null : alternatives.get(0));
			if (comparison > 0) alternatives.clear();
			if (comparison >= 0) alternatives.add(movePrediction);
		}

		if (alternatives.isEmpty()) { // distinguish check mate and stale mate
			final short[] activeKingPositions = board.getPositions(true, whiteActive, ChessPieceType.KING);
			final boolean kingCheckedOrMissing = activeKingPositions.length == 0 ? true : board.isPositionThreatened(activeKingPositions[0]);
			final int rating = kingCheckedOrMissing ? (whiteActive ? -Board.WIN : +Board.WIN) : Board.DRAW;

			final MovePrediction movePrediction = new MovePrediction(rating);
			for (int loop = depth; loop > 0; --loop)
				movePrediction.getMoves().add(null);
			return movePrediction;
		}
		return alternatives.get(ThreadLocalRandom.current().nextInt(alternatives.size()));
	}


	/**
	 * Compares two move predictions to decide which one is more desirable from the given
	 * perspective. Returns {@code 1} if the left alternative is better, {@code -1} if the right
	 * alternative is better, and {@code 0} if both are equally preferable.
	 * @param white {@code true} for white perspective, {@code false} for black perspective
	 * @param leftAlternative the left alternative
	 * @param rightAlternative the right alternative
	 * @return {@code 1} if the left alternative is better, {@code -1} if the right alternative is
	 *         better, and {@code 0} if both are equally preferable
	 * @throws NullPointerException if the left alternative is {@code null}
	 */
	static protected int compareMovePredictions (final boolean white, final MovePrediction leftAlternative, final MovePrediction rightAlternative) {
		if (rightAlternative == null) return 1;

		final int leftRating = leftAlternative.getRating();
		final int rightRating = rightAlternative.getRating();
		if (leftRating > rightRating) return white ? 1 : -1;
		if (leftRating < rightRating) return white ? -1 : 1;
		if (leftRating != +Board.WIN & leftRating != -Board.WIN) return 0;

		// avoid own mate, seek opposition mate
		final int winRating = white ? +Board.WIN : -Board.WIN;
		final int compare = Integer.compare(rightAlternative.getMoveCount(), leftAlternative.getMoveCount());
		return leftRating == winRating ? compare : -compare;
	}
}