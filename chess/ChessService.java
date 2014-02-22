package de.htw.ds.board.chess;

import javax.jws.WebParam;
import javax.jws.WebService;
import de.htw.ds.board.MovePrediction;


@WebService
public interface ChessService {
	
	MovePrediction[] getMovePredictions(
			@WebParam(name= "xfen") String xfen,
			@WebParam(name="searchDepth") short searchDepth);
	
	void putMovePrediction (
			@WebParam(name= "xfen") String xfen,
			@WebParam(name="searchDepth") short searchDepth,
			@WebParam(name="movePrediction") MovePrediction movePrediction);

}
