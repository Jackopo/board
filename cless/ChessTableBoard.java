package de.htw.ds.board.cless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import de.htw.ds.board.Board;
import de.htw.ds.board.PositionalPiece;
import de.sb.javase.Reflection;
import de.sb.javase.TypeMetadata;
import de.sb.javase.util.BitArrays;


/**
 * Instances of this class model Chess boards, including an analyzer for computer moves. Note that
 * this class is designed following the "Flyweight" design pattern, as it uses positional chess
 * pieces that maintain a relation to a specific position on boards with specific dimensions. This
 * way, the pieces can pre-cache move information that otherwise would have to be calculated
 * expensively.
 * @see <a href="http://en.wikipedia.org/wiki/Chess">Wikipedia article about Chess</a>
 */
@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessTableBoard implements ChessBoard {

	private final byte rankCount;
	private final byte fileCount;
	private volatile ChessPiece[] pieces;
	private volatile short moveClock;
	private volatile byte reversibleMoveClock;
	private volatile byte castlingAbilities;
	private volatile short passingPawnPosition;
	private volatile short whiteKingPosition;
	private volatile short blackKingPosition;


	/**
	 * Creates an empty chess board with default dimension.
	 */
	public ChessTableBoard () {
		this((byte) 8, (byte) 8);
	}


	/**
	 * Creates an empty chess board with the given dimension.
	 * @param rankCount the number of ranks on the board
	 * @param fileCount the number of files on the board
	 * @throws IllegalArgumentException if the given rank of file count is smaller than {@code 3}
	 */
	public ChessTableBoard (final byte rankCount, final byte fileCount) {
		super();
		if (rankCount < MIN_RANK_COUNT | fileCount < MIN_FILE_COUNT) throw new IllegalArgumentException();

		this.pieces = new ChessPiece[rankCount * fileCount];
		this.rankCount = rankCount;
		this.fileCount = fileCount;
		this.passingPawnPosition = (short) -1;
		this.whiteKingPosition = (short) -1;
		this.blackKingPosition = (short) -1;
	}


	/**
	 * Creates a chess board with the given properties.
	 * @param pieceMatrix the game neutral piece matrix
	 * @param moveClock the move clock, see {@linkplain #getMoveClock}
	 * @param reversibleMoveClock the reversible move clock, see
	 *        {@linkplain #getReversibleMoveClock}
	 * @param castlingAbilities the castling abilities as an array of four boolean values, i.e. the
	 *        ability of white to castle to the left or right, and the ability of black to castle to
	 *        the left or right
	 * @param passingPawnPosition the passing pawn position, i.e. the position of a pawn that can be
	 *        captured en passant, or {@code -1} for none
	 * @throws NullPointerException if the given piece matrix, or any of it's elements, or the given
	 *         casting abilities are {@code null}
	 * @throws IllegalArgumentException if any if the given characters is not a legal chess piece
	 *         representation, or if the given piece matrix, or any of it's elements, has more than
	 *         {@code 127} slots, or if any of the move clocks is negative, or if the castling
	 *         abilities do not have four elements, of if the resulting chess board would be invalid
	 */
	public ChessTableBoard (final char[][] pieceMatrix, final short moveClock, final byte reversibleMoveClock, final boolean[] castlingAbilities, final short passingPawnPosition) {
		this((byte) pieceMatrix.length, (byte) (pieceMatrix.length == 0 ? 0 : pieceMatrix[0].length));

		if (
			pieceMatrix.length > Byte.MAX_VALUE | pieceMatrix[0].length > Byte.MAX_VALUE |
			moveClock < 0 | reversibleMoveClock < 0 |
			passingPawnPosition < -1 | passingPawnPosition >= this.rankCount * this.fileCount |
			castlingAbilities.length != 4
		) throw new IllegalArgumentException();

		this.moveClock = moveClock;
		this.reversibleMoveClock = reversibleMoveClock;
		this.passingPawnPosition = passingPawnPosition;
		if (castlingAbilities[0]) this.castlingAbilities |= MASK_CASTLE_WHITE_LEFT;
		if (castlingAbilities[1]) this.castlingAbilities |= MASK_CASTLE_WHITE_RIGHT;
		if (castlingAbilities[2]) this.castlingAbilities |= MASK_CASTLE_BLACK_LEFT;
		if (castlingAbilities[3]) this.castlingAbilities |= MASK_CASTLE_BLACK_RIGHT;

		short position = 0;
		for (byte rank = 0; rank < this.rankCount; ++rank) {
			if (pieceMatrix[rank].length != this.fileCount) throw new IllegalArgumentException();

			for (byte file = 0; file < this.fileCount; ++file, ++position) {
				final char alias = pieceMatrix[rank][file];
				if (alias != 0) {
					final ChessPiece piece = ChessPiece.valueOf(this.rankCount, this.fileCount, alias, position);
					if (piece.getType() == ChessPieceType.KING) {
						if (this.getKingPosition(piece.isWhite()) != -1) throw new IllegalArgumentException();
						this.setKingPosition(piece.isWhite(), position);
					}
					this.pieces[position] = piece;
				}
			}
		}
		if (!this.isValid()) throw new IllegalArgumentException();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public ChessTableBoard clone () {
		final ChessTableBoard clone;
		try {
			clone = (ChessTableBoard) super.clone();
		} catch (final CloneNotSupportedException exception) {
			throw new AssertionError();
		}
		clone.pieces = this.pieces.clone();

		return clone;
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
	public short getPieceCount () {
		short pieceCount = 0;
		for (int position = this.pieces.length - 1; position >= 0; --position) {
			if (this.pieces[position] != null) pieceCount += 1;
		}
		return pieceCount;
	}


	/**
	 * {@inheritDoc}
	 */
	public byte getCastlingAbilities () {
		return this.castlingAbilities;
	}


	/**
	 * {@inheritDoc}
	 */
	public short getPassingPawnPosition () {
		return this.passingPawnPosition;
	}


	/**
	 * {@inheritDoc}
	 */
	public short getMoveClock () {
		return this.moveClock;
	}


	/**
	 * {@inheritDoc}
	 */
	public byte getReversibleMoveClock () {
		return this.reversibleMoveClock;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isWhiteActive () {
		return (this.moveClock & 1) == 0;
	}


	/**
	 * {@inheritDoc}
	 */
	public List<short[]> getActiveMoves () {
		final List<short[]> moves = new ArrayList<>(256);

		final boolean whiteActive = this.isWhiteActive();
		if (this.getKingPosition(whiteActive) < 0) return moves;

		boolean mustCaptureKing = false;
		for (final ChessPiece piece : this.pieces) {
			if (piece != null && piece.isWhite() == whiteActive)
				mustCaptureKing = this.collectMoves(moves, piece, mustCaptureKing);
		}
		return moves;
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	public ChessPiece getPiece (final short position) {
		try {
			return this.pieces[position];
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalArgumentException();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public short[] getPositions (final boolean any, final Boolean white, final ChessPieceType type) {
		if (type == ChessPieceType.KING) {
			if (white != null) return white
				? (this.whiteKingPosition == -1 ? new short[0] : new short[] { this.whiteKingPosition })
				: (this.blackKingPosition == -1 ? new short[0] : new short[] { this.blackKingPosition });

			if (this.whiteKingPosition != -1 & this.blackKingPosition != -1) return new short[] { this.whiteKingPosition, this.blackKingPosition };
			return new short[] { this.whiteKingPosition != -1 ? this.whiteKingPosition : this.blackKingPosition };
		}

		if (any) {
			for (short position = 0; position < this.pieces.length; ++position) {
				final ChessPiece piece = this.pieces[position];
				if (piece != null && (white == null || piece.isWhite() == white) && (type == null || piece.getType() == type)) return new short[] { position };
			}
			return new short[0];
		}

		final List<Short> positions = new ArrayList<>();
		for (short position = 0; position < this.pieces.length; ++position) {
			final ChessPiece piece = this.pieces[position];
			if (piece != null && (white == null || piece.isWhite() == white) && (type == null || piece.getType() == type)) positions.add(position);
		}
		return (short[]) Reflection.toArray(positions, short.class);
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRating () {
		if (this.whiteKingPosition == -1) return -Board.WIN;
		if (this.blackKingPosition == -1) return +Board.WIN;

		int rating = 0;
		for (int position = this.pieces.length - 1; position >= 0; --position) {
			final ChessPiece piece = this.pieces[position];
			if (piece != null) rating += piece.getRating();
		}
		return rating;
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	public boolean isPositionThreatened (final short position) {
		return this.isPositionThreatened(position, !this.isWhiteActive());
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException if there are not exactly two positions, or if any of the
	 *         positions is out of range, or if the first position is empty, or if the piece to move
	 *         is not active
	 */
	public void performMove (final short... movePositions) {
		final boolean whiteActive = this.isWhiteActive();
		final ChessPiece[][][] pieceValues = ChessPiece.values(this.rankCount, this.fileCount);
		final ChessPiece[][] activePieceValues = pieceValues[whiteActive ? 0 : 1];
		final ChessPiece[][] passivePieceValues = pieceValues[whiteActive ? 1 : 0];

		short passingPawnPosition = this.passingPawnPosition;
		this.passingPawnPosition = -1;
		this.reversibleMoveClock += 1;
		this.moveClock += 1;

		switch (movePositions.length) {
			case 3: {
				final int kingSourcePosition = movePositions[0];
				final int rookSourcePosition = movePositions[1];
				final int kingSinkPosition = movePositions[2];
				final int rookSinkPosition = kingSinkPosition + ((rookSourcePosition - kingSourcePosition) >>> 31 << 1) - 1;
				if (this.pieces[kingSourcePosition] != activePieceValues[ChessPieceType.KING.ordinal()][kingSourcePosition]) throw new IllegalArgumentException();
				if (this.pieces[rookSourcePosition] != activePieceValues[ChessPieceType.ROOK.ordinal()][rookSourcePosition]) throw new IllegalArgumentException();

				this.setKingPosition(whiteActive, (short) kingSinkPosition);
				this.pieces[kingSourcePosition] = null;
				this.pieces[rookSourcePosition] = null;
				this.pieces[kingSinkPosition] = activePieceValues[ChessPieceType.KING.ordinal()][kingSinkPosition];
				this.pieces[rookSinkPosition] = activePieceValues[ChessPieceType.ROOK.ordinal()][rookSinkPosition];
				this.castlingAbilities &= whiteActive
					? ~(MASK_CASTLE_WHITE_LEFT | MASK_CASTLE_WHITE_RIGHT)
					: ~(MASK_CASTLE_BLACK_LEFT | MASK_CASTLE_BLACK_RIGHT);
				this.reversibleMoveClock = 0;
				return;
			}
			case 2: {
				final short sourcePosition = movePositions[0];
				final short sinkPosition = movePositions[1];
				final ChessPiece sourcePiece = this.pieces[sourcePosition];
				final ChessPiece capturePiece = this.pieces[sinkPosition];
				if (sourcePiece == null || sourcePiece.isWhite() != whiteActive) throw new IllegalArgumentException();

				ChessPieceType sourceType = sourcePiece.getType();
				switch (sourceType) {
					case PAWN: {
						if (capturePiece == null) {
							if (sinkPosition == passingPawnPosition) {
								final int capturePosition = sinkPosition + (sinkPosition > sourcePosition ? -this.fileCount : this.fileCount);
								this.pieces[capturePosition] = null;
							} else {
								final int doubleFileCount = this.fileCount << 1;
								final int difference = sourcePosition - sinkPosition;

								if (doubleFileCount == difference | doubleFileCount == -difference) {
									passingPawnPosition = (short) ((sourcePosition + sinkPosition) >> 1);
									final ChessPiece[] passivePawns = passivePieceValues[ChessPieceType.PAWN.ordinal()];

									for (int offset = -1; offset <= 1; offset += 2) {
										final ChessPiece neighbor = passivePawns[sinkPosition + offset];
										if (this.pieces[neighbor.getPosition()] == neighbor && BitArrays.get(neighbor.getSinkPositionBitBoard(), passingPawnPosition)) {
											this.passingPawnPosition = passingPawnPosition;
											break;
										}
									}
								}
							}
						}

						if (sinkPosition < this.fileCount | sinkPosition >= this.pieces.length - this.fileCount) 
							sourceType = ChessPieceType.QUEEN;
						this.reversibleMoveClock = 0;
						break;
					}

					case ROOK: {
						if (whiteActive) {
							if (sourcePosition == 0) this.castlingAbilities &= ~MASK_CASTLE_WHITE_LEFT;
							else if (sourcePosition == this.fileCount - 1) this.castlingAbilities &= ~MASK_CASTLE_WHITE_RIGHT;
						} else {
							if (sourcePosition == this.pieces.length - this.fileCount) this.castlingAbilities &= ~MASK_CASTLE_BLACK_LEFT;
							else if (sourcePosition == this.pieces.length - 1) this.castlingAbilities &= ~MASK_CASTLE_BLACK_RIGHT;
						}
						break;
					}

					case KING: {
						this.castlingAbilities &= whiteActive
							? ~(MASK_CASTLE_WHITE_LEFT | MASK_CASTLE_WHITE_RIGHT)
							: ~(MASK_CASTLE_BLACK_LEFT | MASK_CASTLE_BLACK_RIGHT);
						this.setKingPosition(whiteActive, sinkPosition);
						break;
					}

					default: {
						break;
					}
				}

				if (capturePiece != null) {
					if (capturePiece.isWhite() == whiteActive) throw new IllegalArgumentException();

					switch (capturePiece.getType()) {
						case KING:
							this.castlingAbilities &= whiteActive
								? ~(MASK_CASTLE_BLACK_LEFT | MASK_CASTLE_BLACK_RIGHT)
								: ~(MASK_CASTLE_WHITE_LEFT | MASK_CASTLE_WHITE_RIGHT);
							this.setKingPosition(!whiteActive, (short) -1);
							break;
						case ROOK:
							if (whiteActive) {
								if (sinkPosition == this.pieces.length - this.fileCount) this.castlingAbilities &= ~MASK_CASTLE_BLACK_LEFT;
								else if (sinkPosition == this.pieces.length - 1) this.castlingAbilities &= ~MASK_CASTLE_BLACK_RIGHT;
							} else {
								if (sinkPosition == 0) this.castlingAbilities &= ~MASK_CASTLE_WHITE_LEFT;
								else if (sinkPosition == this.fileCount - 1) this.castlingAbilities &= ~MASK_CASTLE_WHITE_RIGHT;
							}
							break;
						default:
							break;
					}
					this.reversibleMoveClock = 0;
				}

				this.pieces[sourcePosition] = null;
				this.pieces[sinkPosition] = activePieceValues[sourceType.ordinal()][sinkPosition];
				return;
			}
			default:
				throw new IllegalArgumentException();
		}
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException {@inheritDoc}
	 */
	public void reset (String textRepresentation) {
		if (textRepresentation == null) {
			if (this.rankCount != 8 | this.fileCount < MIN_FILE_COUNT | this.fileCount > 10) throw new IllegalStateException();
			textRepresentation = DEFAULT_EIGHT_RANK_SETUPS[this.fileCount - MIN_FILE_COUNT];
		}

		final ChessTableBoard boardTemplate = ChessXfenCodec.decode(ChessTableBoard.class, textRepresentation);
		if (boardTemplate.rankCount != this.rankCount | boardTemplate.fileCount != this.fileCount) throw new IllegalStateException();

		this.moveClock = boardTemplate.moveClock;
		this.reversibleMoveClock = boardTemplate.reversibleMoveClock;
		this.passingPawnPosition = boardTemplate.passingPawnPosition;
		this.castlingAbilities = boardTemplate.castlingAbilities;
		this.whiteKingPosition = boardTemplate.whiteKingPosition;
		this.blackKingPosition = boardTemplate.blackKingPosition;
		System.arraycopy(boardTemplate.pieces, 0, this.pieces, 0, this.pieces.length);
	}


	/**
	 * {@inheritDoc}
	 */
	public char[][] toCharacters () {
		final char[][] result = new char[this.rankCount][this.fileCount];

		short position = 0;
		for (byte rank = 0; rank < this.rankCount; ++rank) {
			for (byte file = 0; file < this.fileCount; ++file, ++position) {
				final ChessPiece piece = this.pieces[position];
				if (piece != null) result[rank][file] = piece.getAlias();
			}
		}

		return result;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return ChessXfenCodec.encode(this);
	}


	/**
	 * Returns whether or not this board is valid. The following rules must be fulfilled for a valid
	 * chess board:
	 * <ul>
	 * <li>There must be no black pawns on the bottom rank, and no white pawns on the top rank (must
	 * have promoted already).</li>
	 * <li>If the passing pawn position is not {@code -1}, then the position must contain a pawn of
	 * the passive side.</li>
	 * <li>If the white side has castling abilities, then it's king must be on the bottom rank,
	 * either right in the middle (odd file count) or right to the middle (even file count).
	 * Additionally, there must be an associated white rook at the leftmost and/or rightmost bottom
	 * cell.</li>
	 * <li>If the black side has castling abilities, then it's king must be on the top rank, either
	 * right in the middle (odd file count) or right to the middle (even file count). Additionally,
	 * there must be an associated black rook at the leftmost and/or rightmost top cell.</li>
	 * <li>There must be a black king and a white king, and the king of the passive side must not be
	 * in check (would be taken next).</li>
	 * </ul>
	 * @return {@code true} if this chess board is valid, {@code false} otherwise
	 */
	private boolean isValid() {
		for (int position = 0; position < this.fileCount; ++position) {
			ChessPiece piece = this.pieces[position];
			if (piece != null && !piece.isWhite() && piece.getType() == ChessPieceType.PAWN) return false;
		}

		for (int position = this.pieces.length - this.fileCount; position < this.pieces.length; ++position) {
			ChessPiece piece = this.pieces[position];
			if (piece != null && piece.isWhite() && piece.getType() == ChessPieceType.PAWN) return false;
		}

		if (this.passingPawnPosition != -1) {
			ChessPiece piece = this.pieces[this.passingPawnPosition];
			if (piece == null || piece.isWhite() == this.isWhiteActive() || piece.getType() != ChessPieceType.PAWN) return false;
		}

		if ((this.castlingAbilities & (MASK_CASTLE_WHITE_LEFT | MASK_CASTLE_WHITE_RIGHT)) != 0) {
			ChessPiece piece = this.pieces[this.fileCount >> 1];
			if (piece == null || !piece.isWhite() || piece.getType() != ChessPieceType.KING) return false;
			if ((this.castlingAbilities & MASK_CASTLE_WHITE_LEFT) != 0) {
				piece = this.pieces[0];
				if (piece == null || !piece.isWhite() || piece.getType() != ChessPieceType.ROOK) return false;
			}
			if ((this.castlingAbilities & MASK_CASTLE_WHITE_RIGHT) != 0) {
				piece = this.pieces[this.fileCount - 1];
				if (piece == null || !piece.isWhite() || piece.getType() != ChessPieceType.ROOK) return false;
			}
		}

		if ((this.castlingAbilities & (MASK_CASTLE_BLACK_LEFT | MASK_CASTLE_BLACK_RIGHT)) != 0) {
			final int offset = this.pieces.length - this.fileCount;
			ChessPiece piece = this.pieces[offset + (this.fileCount >> 1)];
			if (piece == null || piece.isWhite() || piece.getType() != ChessPieceType.KING) return false;
			if ((this.castlingAbilities & MASK_CASTLE_BLACK_LEFT) != 0) {
				piece = this.pieces[offset];
				if (piece == null || piece.isWhite() || piece.getType() != ChessPieceType.ROOK) return false;
			}
			if ((this.castlingAbilities & MASK_CASTLE_BLACK_RIGHT) != 0) {
				piece = this.pieces[offset + this.fileCount - 1];
				if (piece == null || piece.isWhite() || piece.getType() != ChessPieceType.ROOK) return false;
			}
		}

		if (this.whiteKingPosition == -1 | this.blackKingPosition == -1) return false;

		final boolean whiteActive = this.isWhiteActive();
		final short passiveKingPosition = this.getKingPosition(!whiteActive);
		return !this.isPositionThreatened(passiveKingPosition, whiteActive);
	}


	/**
	 * Returns the king position of the given side.
	 * @param white {@code true} for white, {@code false} for black
	 * @return the matching king position
	 */
	private short getKingPosition (final boolean white) {
		return white ? this.whiteKingPosition : this.blackKingPosition;
	}


	/**
	 * Sets the king position of the given side.
	 * @param white {@code true} for white, {@code false} for black
	 * @param position the king position
	 */
	private void setKingPosition (final boolean white, final short position) {
		if (white) {
			this.whiteKingPosition = position;
		} else {
			this.blackKingPosition = position;
		}
	}


	/**
	 * Collects all valid moves of the given piece into the given collection. Returns {@code true}
	 * if further valid moves must capture the opposing king, {@code false} otherwise.
	 * @param moves the move collection
	 * @param activePiece the moving piece
	 * @param mustCaptureKing whether or not the opposing king must to be captured for moves to be
	 *        valid
	 * @return whether or not the opposing king needs to be captured for moves to be valid
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	private boolean collectMoves (final Collection<short[]> moves, final ChessPiece activePiece, boolean mustCaptureKing) {
		final boolean whiteActive = activePiece.isWhite();

		final ChessPiece[] passiveKingValues = ChessPiece.values(this.rankCount, this.fileCount)[whiteActive ? 1 : 0][ChessPieceType.KING.ordinal()];
		if (activePiece.getType() != ChessPieceType.PAWN & this.reversibleMoveClock > 100) return true;

		for (final short[] sinkPositionSequence : activePiece.getSinkPositions()) {
			for (final short maskedSinkPosition : sinkPositionSequence) {
				final short sinkPosition = (short) (maskedSinkPosition & ChessPiece.MASK_POSITION);

				if ((maskedSinkPosition & ~ChessPiece.MASK_POSITION) != ChessPiece.MOVE_CASTLING) {
					final boolean canCapture = (maskedSinkPosition & ChessPiece.MOVE_CAPTURE_FORBIDDEN) == 0;
					final boolean canOccupy = (maskedSinkPosition & ChessPiece.MOVE_CAPTURE_REQUIRED) == 0;
					final ChessPiece sinkPiece = this.pieces[sinkPosition];

					if (sinkPiece == passiveKingValues[sinkPosition]) {
						if (canCapture) {
							if (!mustCaptureKing) moves.clear();
							moves.add(new short[] { activePiece.getPosition(), sinkPosition });
							mustCaptureKing = true;
						}
						break;
					}

					if (sinkPiece != null) {
						if (canCapture & !mustCaptureKing & sinkPiece.isWhite() != whiteActive) {
							moves.add(new short[] { activePiece.getPosition(), sinkPosition });
						}
						break;
					}

					if ((canOccupy | sinkPosition == this.passingPawnPosition) & !mustCaptureKing) {
						moves.add(new short[] { activePiece.getPosition(), sinkPosition });
					}
				} else if (!mustCaptureKing) {
					final short sourcePosition = activePiece.getPosition();
					if (sinkPosition <= sourcePosition) {
						boolean castlingPermitted = (this.castlingAbilities & (MASK_CASTLE_WHITE_LEFT << (whiteActive ? 0 : 2))) != 0;

						for (short position = (short) (sinkPosition + (this.fileCount & 1) - 1); castlingPermitted & position < sourcePosition; ++position) {
							castlingPermitted &= this.pieces[position] == null;
						}
						for (short position = (short) (sinkPosition + 1); castlingPermitted & position <= sourcePosition; ++position) {
							castlingPermitted &= !this.isPositionThreatened(position, !whiteActive);
						}

						final short rookSourcePosition = (short) (sinkPosition - ((this.fileCount + 1) & 1) - 1);
						if (castlingPermitted) moves.add(new short[] { sourcePosition, rookSourcePosition, sinkPosition });
					} else {
						boolean castlingPermitted = (this.castlingAbilities & (MASK_CASTLE_WHITE_RIGHT << (whiteActive ? 0 : 2))) != 0;

						for (short position = sinkPosition; castlingPermitted & position > sourcePosition; --position) {
							castlingPermitted &= this.pieces[position] == null;
						}
						for (short position = (short) (sinkPosition - 1); castlingPermitted & position >= sourcePosition; --position) {
							castlingPermitted &= !this.isPositionThreatened(position, !whiteActive);
						}

						final short rookSourcePosition = (short) (sinkPosition + 1);
						if (castlingPermitted) moves.add(new short[] { sourcePosition, rookSourcePosition, sinkPosition });
					}
				}
			}
		}

		return mustCaptureKing;
	}


	/**
	 * Returns whether or not the given side can capture a (possibly temporal) piece at the given
	 * position with it's next move. Note that in opposition to {@linkplain #getActiveMoves}, this
	 * method is based on a reverse movement check; the idea is that if a given piece is placed on
	 * position A and can capture on position B, then a similar piece placed on position B must be
	 * able to capture on position A. This way, only three movement types need to be checked
	 * (knight, bishop and rook), as all others move in a similar fashion.
	 * @param position the position to be captured
	 * @param white {@code true} for white, {@code false} for black
	 * @return whether or not the given position can be captured by the given side within it's next
	 *         move
	 * @throws IllegalArgumentException if the given position is out of range
	 */
	private boolean isPositionThreatened (final short position, final boolean white) {
		final ChessPiece[][] activePieceValues = ChessPiece.values(this.rankCount, this.fileCount)[white ? 0 : 1];

		// check for pieces that capture like knights, i.e. knight, archbishop, chancellor, empress.
		for (final short[] sinkPositionSequence : activePieceValues[ChessPieceType.KNIGHT.ordinal()][position].getSinkPositions()) {
			final short sinkPosition = (short) (sinkPositionSequence[0] & PositionalPiece.MASK_POSITION);
			final ChessPiece piece = this.pieces[sinkPosition];
			if (piece != null && piece.isWhite() == white) {
				switch (piece.getType()) {
					case KNIGHT:
					case CHANCELLOR:
					case ARCHBISHOP:
					case EMPRESS:
						return true;
					default:
						break;
				}
			}
		}

		// check for pieces that capture like bishops, i.e. bishop, archbishop, queen, empress, and
		// king/pawn (first sink position only). Note that pawn capture is additionally constrained by direction.
		for (final short[] sinkPositionSequence : activePieceValues[ChessPieceType.BISHOP.ordinal()][position].getSinkPositions()) {
			for (int index = 0; index < sinkPositionSequence.length; ++index) {
				final short sinkPosition = (short) (sinkPositionSequence[index] & PositionalPiece.MASK_POSITION);
				final ChessPiece piece = this.pieces[sinkPosition];
				if (piece == null) continue;

				if (piece.isWhite() == white) {
					switch (piece.getType()) {
						case BISHOP:
						case ARCHBISHOP:
						case QUEEN:
						case EMPRESS:
							return true;
						case KING:
							if (index == 0) return true;
							break;
						case PAWN:
							if (index == 0 & (white ^ position < sinkPosition)) return true;
							break;
						default:
							break;
					}
				}
				break;
			}
		}

		// check for pieces that capture like rooks, i.e. rook, chancellor, queen, empress, and
		// king (first sink position only).
		for (final short[] sinkPositionSequence : activePieceValues[ChessPieceType.ROOK.ordinal()][position].getSinkPositions()) {
			for (int index = 0; index < sinkPositionSequence.length; ++index) {
				final short sinkPosition = (short) (sinkPositionSequence[index] & PositionalPiece.MASK_POSITION);
				final ChessPiece piece = this.pieces[sinkPosition];
				if (piece == null) continue;

				if (piece.isWhite() == white) {
					switch (piece.getType()) {
						case ROOK:
						case CHANCELLOR:
						case QUEEN:
						case EMPRESS:
							return true;
						case KING:
							if (index == 0) return true;
							break;
						default:
							break;
					}
				}
				break;
			}
		}

		// check for en passant capture
		return position == this.passingPawnPosition & this.isWhiteActive() == white;
	}
}