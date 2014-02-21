package de.htw.ds.board.chess;

import de.htw.ds.board.Board;
import de.sb.javase.TypeMetadata;


@TypeMetadata(copyright = "2013-2014 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface ChessBoard extends Board<ChessPieceType> {

	/**
	 * Chess boards must have at least 3 ranks to allow for one rank distance between two
	 * single-rank armies.
	 */
	static final byte MIN_RANK_COUNT = 3;

	/**
	 * Chess boards must have at least 3 files to avoid negative castling distances. Note that kings
	 * castle on the spot on boards with 3 and 4 files.
	 */
	static final byte MIN_FILE_COUNT = 3;

	/**
	 * The bit-mask for the right to castle to the left of the white king.
	 */
	static final byte MASK_CASTLE_WHITE_LEFT = 0b00000001;

	/**
	 * The bit-mask for the right to castle to the right of the white king.
	 */
	static final byte MASK_CASTLE_WHITE_RIGHT = 0b00000010;

	/**
	 * The bit-mask for the right to castle to the left of the black king.
	 */
	static final byte MASK_CASTLE_BLACK_LEFT = 0b00000100;

	/**
	 * The bit-mask for the right to castle to the right of the black king.
	 */
	static final byte MASK_CASTLE_BLACK_RIGHT = 0b00001000;

	/**
	 * The default board setups for chess boards with eight ranks three to ten files.
	 */
	static final String[] DEFAULT_EIGHT_RANK_SETUPS = {
		"rkr/ppp/3/3/3/3/PPP/RKR w KQkq - 0 1",
		"rekr/pppp/4/4/4/4/PPPP/REKR w KQkq - 0 1",
		"rckcr/ppppp/5/5/5/5/PPPPP/RCKCR w KQkq - 0 1",
		"raqkar/pppppp/6/6/6/6/PPPPPP/RAQKAR w KQkq - 0 1",
		"rnqkanr/ppppppp/7/7/7/7/PPPPPPP/RNQKANR w KQkq - 0 1",
		"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
		"rnbqkbncr/ppppppppp/9/9/9/9/PPPPPPPPP/RNBQKBNCR w KQkq - 0 1",
		"rnabqkbanr/pppppppppp/10/10/10/10/PPPPPPPPPP/RNABQKBANR w KQkq - 0 1"
	};


	/**
	 * Returns the castling abilities.
	 * @return the four castling abilities as a bit field
	 */
	public byte getCastlingAbilities ();


	/**
	 * Returns the passing pawn position.
	 * @return the position of a pawn of the currently passive side that just moved two ranks
	 *         forward, and can now be captured en passant by a pawn of the active side
	 */
	public short getPassingPawnPosition ();
}