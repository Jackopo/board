package de.htw.ds.board.chess;

import java.util.ArrayList;
import java.util.List;
import de.htw.ds.board.PieceType;
import de.htw.ds.board.PositionalPiece;
import de.sb.javase.TypeMetadata;
import de.sb.javase.util.BitArrays;


/**
 * Instances of this class model positional chess pieces. Positional pieces are concrete definitions
 * of pieces, i.e. they have a relation to a board dimension, and their position within such boards.
 * Such a piece can only be reused for a specific position on matching boards. However, the
 * advantage of such pieces is that they can pre-calculate geometrically valid absolute move
 * directions, which is a much faster approach compared to using relative move directions.<br />
 * Note that this class has a natural ordering that is inconsistent with
 * {@linkplain #equals(Object)}, as chess pieces related to differing board dimensions may share the
 * same ordinal. Therefore, equality remains based on identity in this class, not ordinal;
 * practically this should never be a problem, as differing chess board dimensions indicate
 * differing chess variants as well.<br />
 * Finally note that this class is declared final because it's conception assumes a limited amount
 * of well known instances, similarly to an enum; this condition would be violated if subclasses
 * create additional ones.
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessPiece implements PositionalPiece<ChessPieceType>, Comparable<ChessPiece> {
	static private final ChessPiece[][][][] VALUES = new ChessPiece[0x3fff][][][];
	static private final byte LOG2_WORD_SIZE = 6;

	static public final short MOVE_NORMAL = (short) 0x0000;
	static public final short MOVE_CAPTURE_FORBIDDEN = (short) 0x4000;
	static public final short MOVE_CAPTURE_REQUIRED = (short) 0x8000;
	static public final short MOVE_CASTLING = (short) 0xC000;

	private final byte rankCount;
	private final byte fileCount;
	private final boolean white;
	private final ChessPieceType type;
	private final short position;

	private final short[][] sinkPositions;
	private final long[] sinkPositionBitBoard;
	private final int rating;


	/**
	 * Creates a new instance.
	 * @param rankCount the rank count
	 * @param fileCount the file count
	 * @param white {@code true} for white, {@code false} for black
	 * @param type the type
	 * @param position the position
	 * @throws NullPointerException if the given type is {@code null}
	 * @throws IllegalArgumentException if the given rank or file count, or the position, is
	 *         strictly negative
	 */
	private ChessPiece (final byte rankCount, final byte fileCount, final boolean white, final ChessPieceType type, final short position) {
		super();
		if (type == null) throw new NullPointerException();
		if (rankCount < 0 | fileCount < 0 | position < 0) throw new IllegalArgumentException();

		this.rankCount = rankCount;
		this.fileCount = fileCount;
		this.white = white;
		this.type = type;
		this.position = position;
		this.sinkPositions = sinkPositions(rankCount, fileCount, white, type, position);
		this.sinkPositionBitBoard = sinkPositionBitBoard(rankCount, fileCount, this.sinkPositions);
		this.rating = rating(rankCount, fileCount, white, type, position, this.sinkPositionBitBoard);
	}


	/**
	 * {@inheritDoc}
	 */
	public int ordinal () {
		return ((this.white ? 0 : 1) << 18) | (this.type.ordinal() << 14) | (this.position << 0);
	}


	/**
	 * {@inheritDoc}
	 */
	public String name () {
		final byte rank = (byte) (this.position / this.fileCount);
		final byte file = (byte) (this.position % this.fileCount);
		final char fileCharacter = Character.forDigit(file + 10, Character.MAX_RADIX);
		return String.format("%s_%s[%c%d]", this.white ? "WHITE" : "BLACK", this.type, fileCharacter, rank + 1);
	}


	/**
	 * {@inheritDoc}
	 */
	public char getAlias () {
		final char typeAlias = this.type.getAlias();
		return this.white ? Character.toUpperCase(typeAlias) : Character.toLowerCase(typeAlias);
	}


	/**
	 * {@inheritDoc}
	 */
	public byte getRankCount () {
		return this.rankCount;
	}


	/**
	 * {@inheritDoc}
	 */
	public byte getFileCount () {
		return this.fileCount;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isWhite () {
		return this.white;
	}


	/**
	 * {@inheritDoc}
	 */
	public ChessPieceType getType () {
		return this.type;
	}


	/**
	 * {@inheritDoc}
	 */
	public short getPosition () {
		return this.position;
	}


	/**
	 * {@inheritDoc} The two leading masking bits are used as follows:
	 * <ul>
	 * <li>{@linkplain #MOVE_NORMAL}: no special handling required</li>
	 * <li>{@linkplain #MOVE_CAPTURE_FORBIDDEN}: move only permitted if target field is empty</li>
	 * <li>{@linkplain #MOVE_CAPTURE_REQUIRED}: move only permitted if target field contains an
	 * opposite piece</li>
	 * <li>{@linkplain #MOVE_CASTLING}: move must cross empty fields only, and neither escape nor
	 * cross check</li>
	 * </ul>
	 * @return the (masked) absolute move directions
	 */
	public short[][] getSinkPositions () {
		return this.sinkPositions;
	}


	/**
	 * {@inheritDoc}
	 */
	public long[] getSinkPositionBitBoard () {
		return this.sinkPositionBitBoard;
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
	public int compareTo (final ChessPiece chessPiece) {
		return this.ordinal() - chessPiece.ordinal();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return this.name();
	}


	/**
	 * Calculates the geometrically valid sink positions, with each entry consisting of positions
	 * stemming from subsequent steps of continuous movement. This implies that non-continuous kinds
	 * of movement will contain exactly one sink position per geometrically valid move direction.
	 * @param rankCount the number of ranks on a board
	 * @param fileCount the number of files on a board
	 * @param white {@code true} for white, {@code false} for black
	 * @param type the piece type
	 * @param position the piece position
	 * @return the (masked) sink positions
	 * @throws NullPointerException if the given type is {@code null}
	 * @see #getSinkPositions()
	 */
	static private short[][] sinkPositions (final byte rankCount, final byte fileCount, final boolean white, final ChessPieceType type, final short position) {
		assert rankCount > 0 & fileCount > 0 & position >= 0 & position < rankCount * fileCount;

		final List<short[]> result = new ArrayList<>();
		final byte sourceRank = (byte) (position / fileCount);
		final byte sourceFile = (byte) (position % fileCount);
		final byte reverseRank = (byte) (rankCount - sourceRank - 1);
		final byte reverseFile = (byte) (fileCount - sourceFile - 1);

		switch (type) {
			case PAWN: {
				for (final short motionDirection : type.getSingleMoveVectors()) {
					final byte rankDistance = (byte) (motionDirection >> 8);
					final byte fileDistance = (byte) motionDirection;
					final int sinkRank = sourceRank + (white ? rankDistance : -rankDistance);
					final int sinkFile = sourceFile + fileDistance;

					if (sinkRank >= 0 & sinkRank < rankCount & sinkFile >= 0 & sinkFile < fileCount) {
						final short sinkPosition = (short) (sinkRank * fileCount + sinkFile);
						if (fileDistance == 0) {
							if (rankCount >= 4 & (white ? sourceRank : reverseRank) <= 1) {
								// need to add the special fast forward move here because types are unaware of position
								result.add(new short[] { (short) (sinkPosition | MOVE_CAPTURE_FORBIDDEN), (short) (((sinkPosition << 1) - position) | MOVE_CAPTURE_FORBIDDEN) });
							} else {
								result.add(new short[] { (short) (sinkPosition | MOVE_CAPTURE_FORBIDDEN) });
							}
						} else {
							result.add(new short[] { (short) (sinkPosition | MOVE_CAPTURE_REQUIRED) });
						}
					}
				}
				break;
			}

			case KING: {
				// need to add the special castling moves here because types are unaware of board dimension
				if ((white ? sourceRank : reverseRank) == 0 & sourceFile == (fileCount >> 1)) {
					final int castlingDistance = ((fileCount - 1) >> 1) - 1;
					result.add(new short[] { (short) ((position - castlingDistance) | MOVE_CASTLING) });
					result.add(new short[] { (short) ((position + castlingDistance) | MOVE_CASTLING) });
				}
			}

			default: { /* all types except pawns, including king! */
				for (final short moveVector : type.getSingleMoveVectors()) {
					final int sinkRank = sourceRank + (byte) (moveVector >> 8);
					final int sinkFile = sourceFile + (byte) moveVector;

					if (sinkRank >= 0 & sinkRank < rankCount & sinkFile >= 0 & sinkFile < fileCount) {
						result.add(new short[] { (short) (sinkRank * fileCount + sinkFile) });
					}
				}

				for (final short moveVector : type.getContinuousMoveVectors()) {
					final byte rankDistance = (byte) (moveVector >> 8);
					final byte fileDistance = (byte) moveVector;

					if (rankDistance != 0 | fileDistance != 0) {
						final int targetRank = rankDistance == 0 ? Byte.MAX_VALUE : (rankDistance < 0 ? sourceRank : reverseRank);
						final int targetFile = fileDistance == 0 ? Byte.MAX_VALUE : (fileDistance < 0 ? sourceFile : reverseFile);
						final short[] sinkPositionSequence = new short[targetFile < targetRank ? targetFile : targetRank];

						for (int index = 0; index < sinkPositionSequence.length; ++index) {
							final byte sinkRank = (byte) (sourceRank + (index + 1) * rankDistance);
							final byte sinkFile = (byte) (sourceFile + (index + 1) * fileDistance);
							sinkPositionSequence[index] = (short) (sinkRank * fileCount + sinkFile);
						}
						if (sinkPositionSequence.length > 0) result.add(sinkPositionSequence);
					}
				}
				break;
			}
		}

		return result.toArray(new short[0][]);
	}


	/**
	 * Calculates the sink position bit-board indicating all positions reachable within a single
	 * move.
	 * @param rankCount the number of ranks on a board
	 * @param fileCount the number of files on a board
	 * @param absoluteMoveDirections the absolute move directions
	 * @return the sink position bit-board
	 * @throws NullPointerException if the given directions are {@code null}
	 * @see #getSinkPositionBitBoard()
	 */
	static private final long[] sinkPositionBitBoard (final byte rankCount, final byte fileCount, final short[][] absoluteMoveDirections) {
		assert rankCount > 0 & fileCount > 0;

		final short fieldCount = (short) (rankCount * fileCount);
		final long[] sinkPositionBitBoard = new long[((fieldCount - 1) >> LOG2_WORD_SIZE) + 1];

		for (final short[] absoluteMoveDirection : absoluteMoveDirections) {
			for (final short maskedSinkPosition : absoluteMoveDirection) {
				final short sinkPosition = (short) (maskedSinkPosition & MASK_POSITION);
				BitArrays.on(sinkPositionBitBoard, sinkPosition);
			}
		}
		return sinkPositionBitBoard;
	}


	/**
	 * Calculates the piece rating for a piece with the given properties, in cents. The rating is
	 * adjusted for both piece color and piece position.
	 * @param rankCount the number of ranks on a board
	 * @param fileCount the number of files on a board
	 * @param position the piece position
	 * @param white {@code true} for white, {@code false} for black
	 * @param type the piece type
	 * @return the piece rating
	 * @throws NullPointerException if the given type is {@code null}
	 * @see #getRating()
	 */
	static private int rating (final byte rankCount, final byte fileCount, final boolean white, final ChessPieceType type, final short position, final long[] sinkPositionBitBoard) {
		assert rankCount > 0 & fileCount > 0 & position >= 0 & position < rankCount * fileCount;

		final byte sourceRank = (byte) (position / fileCount);
		final byte sourceFile = (byte) (position % fileCount);
		final byte reverseRank = (byte) (rankCount - sourceRank - 1);
		final byte reverseFile = (byte) (fileCount - sourceFile - 1);

		int rating;
		switch (type) {
			case ARCHBISHOP:
			case CHANCELLOR:
				rating = 700;
				break;
			case BISHOP:
			case KNIGHT:
				rating = 300;
				break;
			default:
				rating = type.getRating();
				break;
		}

		switch (type) {
		// prefer centralized pawn advancement by increasing their rating up to 100%
			case PAWN: {
				final byte halfFileCount = (byte) (fileCount >> 1);
				final int advancement = (white ? sourceRank : reverseRank) - 1;
				final int centralization = sourceFile < halfFileCount ? sourceFile : reverseFile;
				final int normalizer = rankCount + halfFileCount - 4;
				rating = rating * (normalizer + advancement + centralization) / normalizer;
				break;
			}

			// adjust rating for number of reachable squares
			case KNIGHT:
			case BISHOP:
			case ARCHBISHOP:
			case CHANCELLOR: {
				final int sinkPositionCount = (int) BitArrays.cardinality(sinkPositionBitBoard, 0, rankCount * fileCount);
				rating = rating + 50 * sinkPositionCount / (rankCount + fileCount - 2);
				break;
			}

			case KING: { // prefer king on base row
				if ((white ? sourceRank : reverseRank) == 0) rating += 50;

				break;
			}

			default: {
				break;
			}
		}
		return white ? rating : -rating;
	}


	/**
	 * Initializes the values for the given rank and file count, if necessary.
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @throws IllegalArgumentException if the given rank or file count is negative
	 */
	static private void initializeValues (final byte rankCount, final byte fileCount) {
		if (rankCount <= 0 | fileCount <= 0) throw new IllegalArgumentException();

		final int packedDimension = (rankCount << 7) | fileCount;
		synchronized (VALUES) {
			if (VALUES[packedDimension] == null) {
				VALUES[packedDimension] = new ChessPiece[2][ChessPieceType.values().length][rankCount * fileCount];

				for (final ChessPieceType type : ChessPieceType.values()) {
					for (int position = rankCount * fileCount - 1; position >= 0; --position) {
						VALUES[packedDimension][0][type.ordinal()][position] = new ChessPiece(rankCount, fileCount, true, type, (short) position);
						VALUES[packedDimension][1][type.ordinal()][position] = new ChessPiece(rankCount, fileCount, false, type, (short) position);
					}
				}
			}
		}
	}


	/**
	 * Returns the piece cache for the given rank and file count, organized by:
	 * <ul>
	 * <li>position</li>
	 * <li>piece color (white 0, black 1)</li>
	 * <li>piece type ordinal</li>
	 * <ul>
	 * Note that the piece cache of the given dimensions is initialized if necessary.
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @return the piece cache
	 * @throws IllegalArgumentException if the given rankCount or fileCount is negative
	 */
	static public ChessPiece[][][] values (final byte rankCount, final byte fileCount) {
		if (rankCount <= 0 | fileCount <= 0) throw new IllegalArgumentException();

		final int packedDimension = (rankCount << 7) | fileCount;
		if (VALUES[packedDimension] == null) initializeValues(rankCount, fileCount);
		return VALUES[packedDimension];
	}


	/**
	 * Returns a piece instance from the piece cache suitable for the given color, type and
	 * position.
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @param white {@code true} for white, {@code false} for black
	 * @param type the piece type
	 * @param position the piece's board position
	 * @return the cached piece
	 * @throws NullPointerException if the given color or type is {@code null}
	 * @throws IllegalArgumentException if the given rankCount or fileCount is negative, or if the
	 *         given position is out of range
	 */
	static public ChessPiece valueOf (final byte rankCount, final byte fileCount, final boolean white, final ChessPieceType type, final short position) {
		if (position < 0 | position >= rankCount * fileCount) throw new IllegalArgumentException();

		return values(rankCount, fileCount)[white ? 0 : 1][type.ordinal()][position];
	}


	/**
	 * Returns a piece instance from the piece cache suitable for the given alias and position.
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @param alias the character alias
	 * @param position the piece's board position
	 * @return the cached piece
	 * @throws IllegalArgumentException if the given rankCount or fileCount is negative, if the
	 *         given position is out of range, or if the given alias is illegal
	 */
	static public ChessPiece valueOf (final byte rankCount, final byte fileCount, final char alias, final short position) {
		final int colorOrdinal = Character.isUpperCase(alias) ? 0 : 1;
		final PieceType type = ChessPieceType.valueOf(Character.toUpperCase(alias));
		return values(rankCount, fileCount)[colorOrdinal][type.ordinal()][position];
	}
}