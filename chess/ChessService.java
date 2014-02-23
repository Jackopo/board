package de.htw.ds.board.chess;

import java.sql.SQLException;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;
import de.htw.ds.board.MovePrediction;


@WebService
public interface ChessService {
	
	MovePrediction[] getMovePredictions(
			@WebParam(name= "xfen") String xfen,
			@WebParam(name="searchDepth") short searchDepth) throws SQLException;
	//@Oneway
	void putMovePrediction (
			@WebParam(name= "xfen") String xfen,
			@WebParam(name="searchDepth") short searchDepth,
			@WebParam(name="movePrediction") MovePrediction movePrediction) throws SQLException;

}
