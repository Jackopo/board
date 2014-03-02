package de.htw.ds.board.chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.htw.ds.board.Board;
import de.htw.ds.board.MovePrediction;
import de.htw.ds.board.Piece;
import de.sb.javase.sync.DaemonThreadFactory;

public class ChessAnalyzer2 extends ChessAnalyzer {
	private static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
	private static ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(PROCESSOR_COUNT, new DaemonThreadFactory());
	
	
	public ChessAnalyzer2() {
		super();
	}
	/**
	 *  ------ xfen: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
	 *   
	 *   Single Threaded Implementierung:
	 *   
	 *   Suchetiefe 4: 769ms, 749ms, 800ms, 780ms, 824ms -> 784.4 ms
	 *   
	 *   Suchetiefe 5: 6002ms, 5900ms, 5791ms, 6012ms, 5953ms -> 5931.6 ms
	 *   
	 *   Suchetiefe 6: 129338ms, 129030ms, 128927ms, 127996ms, 127346ms -> 128527 ms
	 *   
	 *   Multi Threaded Implementierung:
	 *   
	 *   Suchetiefe 4: 757ms, 725ms, 760ms, 715ms, 725ms -> 736.4 ms - Gewinn: 6.5%
	 *   
	 *   Suchetiefe 5: 3829ms, 4034ms, 3785ms, 3774ms, 3944ms -> 3867.8 ms - Gewinn: 34.8%
	 *   
	 *   Suchetiefe 6: 72211ms, 71478ms, 71792ms, 72007ms, 70996ms -> 71696.8 ms - Gewinn: 44.2 %
	 *   
	 *   Wir haben schon mit einer Suchtiefe von 4 einen kleine Gewinn und mit grosseren Suchtiefen steigt den Gewinn auf.
	 *   Die Fixkosten sowie Kosten für erzeugen und zerstören von Threads sind beeinflussen deutlich weniger in diesem Implementierung.
	 *   
	 * 
	 * 
	 */
	public MovePrediction predictMoves (final Board<ChessPieceType> board, final int depth) {
		
		return predictMovesMultiThreaded(board, depth);
	}

	protected final MovePrediction predictMovesMultiThreaded (final Board<ChessPieceType> board, final int depth) {
		if (depth <= 0) throw new IllegalArgumentException();


		List<MovePrediction> alternatives = new ArrayList<>();
		final boolean whiteActive = board.isWhiteActive();
		final Set<Future<MovePrediction>> futures = new HashSet<>();

		final Collection<short[]> moves = board.getActiveMoves();
		
		// iteration through the moves collection
		for (final short[] move : moves) {
			
			Callable<MovePrediction> callable =  createCallable(move, board, depth);			
			futures.add(EXECUTOR_SERVICE.submit(callable));			
		
		}
		
		// iteration through the futures collection where the result of the alternatives is 

		for (final Future<MovePrediction> tempFuture : futures) {

			try {
			
				while (true) {
					try {
						MovePrediction movePrediction = tempFuture.get();
						final int comparison = compareMovePredictions(whiteActive, movePrediction, alternatives.isEmpty() ? null : alternatives.get(0));
						if (comparison > 0) alternatives.clear();
						if (comparison >= 0) alternatives.add(movePrediction);

						break;
					} catch (final InterruptedException interrupt) {
						Logger.getGlobal().log(Level.WARNING, interrupt.getMessage(), interrupt);
					}
				}
			} catch (final ExecutionException exception) {
				final Throwable cause = exception.getCause();
				if (cause instanceof Error) throw (Error) cause;
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;
				throw new AssertionError();
			} 
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

	private  Callable<MovePrediction> createCallable(final short[] move, final Board<ChessPieceType> board,final int depth) {

		final boolean whiteActive = board.isWhiteActive();


		final Callable<MovePrediction> callable = new Callable<MovePrediction>()  {
			public MovePrediction call() throws InterruptedException {
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
							if (recapturePiece != null && recapturePiece.isWhite() == whiteActive && recapturePiece.getType() == ChessPieceType.KING) return movePrediction;
						}
						movePrediction.getMoves().add(0, move);
					} else {

						movePrediction = new MovePrediction(boardClone.getRating());
						movePrediction.getMoves().add(move);
					}
				}
				
				return movePrediction;
				
			}
		};

		return callable;


	}


}
