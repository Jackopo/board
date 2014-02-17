package de.htw.ds.board;

import javax.swing.event.ChangeEvent;
import de.sb.javase.TypeMetadata;


/**
 * These change events are thrown by board panels once a move has been performed.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class MoveEvent extends ChangeEvent {
	private static final long serialVersionUID = 1L;

	private final PieceType pieceType;
	private final short[] move;
	private final boolean capture;
	private final boolean gameOver;
	private final int rating;


	/**
	 * Creates a new instance
	 * @param source the source
	 * @param PieceType the piece type than moved
	 * @param move the move
	 * @param capture whether or not a piece was captured
	 * @param gameOver whether or not the game has ended
	 * @param rating the projected rating
	 */
	public MoveEvent (final Object source, final PieceType PieceType, final short[] move, final boolean capture, final boolean gameOver, final int rating) {
		super(source);

		this.pieceType = PieceType;
		this.move = move;
		this.capture = capture;
		this.gameOver = gameOver;
		this.rating = rating;
	}


	/**
	 * Returns the piece type that moved.
	 * @return the piece type
	 */
	public PieceType getType () {
		return this.pieceType;
	}


	/**
	 * Returns the move.
	 * @return the move as a sequence of visited positions, including the start position
	 */
	public short[] getMove () {
		return this.move;
	}


	/**
	 * Returns whether or not a piece was captured.
	 * @return {@code true} if a piece was captured, {@code false} otherwise
	 */
	public boolean getCapture () {
		return this.capture;
	}


	/**
	 * Returns whether or not the game is over.
	 * @return {@code true} if the game is over, {@code false} otherwise
	 */
	public boolean getGameOver () {
		return this.gameOver;
	}


	/**
	 * Returns the projected rating.
	 * @return the projected rating
	 */
	public int getRating () {
		return this.rating;
	}
}