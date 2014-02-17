package de.htw.ds.board;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import de.sb.javase.TypeMetadata;


/**
 * Instances of this class represent the results of minimax board analysis. They contain the
 * predicted moves considering best play by both sides, and the board rating after performing said
 * sequence.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class MovePrediction {

	private final int rating;
	private final List<short[]> moves;


	/**
	 * Creates a new instance with the given projected rating.
	 * @param rating the predicted board rating in cents
	 */
	public MovePrediction (final int rating) {
		super();

		this.rating = rating;
		this.moves = new LinkedList<>();
	}


	/**
	 * Returns the predicted board rating.
	 * @return the predicted board rating, in cents
	 */
	public int getRating () {
		return this.rating;
	}


	/**
	 * Returns the predicted moves.
	 * @return the predicted moves, with each move represented by an array of positions in visiting
	 *         order, starting with the source position
	 */
	public List<short[]> getMoves () {
		return this.moves;
	}


	/**
	 * Returns the number of moves that are not {@code null}.
	 * @return the move count
	 */
	public int getMoveCount () {
		final int sequenceLength = this.moves.size();
		for (int index = 0; index < sequenceLength; ++index) {
			if (this.moves.get(index) == null) return index;
		}
		return sequenceLength;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return String.format("%s rates %+.2f", Arrays.deepToString(this.moves.toArray()), 0.01 * this.rating);
	}
}