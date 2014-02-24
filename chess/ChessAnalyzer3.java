package de.htw.ds.board.chess;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import javax.xml.ws.Service;

import de.htw.ds.board.Board;
import de.htw.ds.board.MovePrediction;
import de.htw.ds.shop.ShopService;
import de.sb.javase.xml.Namespaces;

public class ChessAnalyzer3 extends ChessAnalyzer {
	private static final String SERVICE_URL = "http://laptop:8001/ChessService";
	public ChessAnalyzer3() {
		super();
	}

	public MovePrediction predictMoves (final Board<ChessPieceType> board, final int depth)   {
		
		MovePrediction result = new MovePrediction(2);
		String xfen = board.toString();

		String[] xfenElements = xfen.split("\\s+");

		final String reducedXfen = xfenElements[0] + " " + xfenElements[1] + " " + xfenElements[2] + " " +  xfenElements[3];
		final String movesClock = xfenElements[5];

		try {
			if (Integer.parseInt(movesClock) >= 20 ) {
				System.out.println("no webservice");
				result =  ChessAnalyzer3.predictMovesSingleThreaded(board, depth);
			} else {
				System.out.println("going to use webservice");
				result = useWebService(reducedXfen, depth, board);
			}
		} catch (MalformedURLException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	static private final ChessService createServiceProxy() throws MalformedURLException {
		final URL wsdlLocator = new URL(SERVICE_URL + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(ChessService.class));
		final ChessService serviceProxy = proxyFactory.getPort(ChessService.class);

		return serviceProxy ;
	}


	private final MovePrediction useWebService(final String reducedXfen,final int depth, final Board<ChessPieceType> board) throws MalformedURLException,SQLException {
		final MovePrediction result;


		final ChessService serviceProxy;

		serviceProxy = createServiceProxy();
		MovePrediction[] movePredictions = serviceProxy.getMovePredictions(reducedXfen, (short) depth);
		System.out.println("stop");		
		if (movePredictions.length == 0) {
			System.out.println("no mp in db");
			MovePrediction mp = ChessAnalyzer.predictMovesSingleThreaded(board, depth);
			System.out.println("mp : " + mp.toString());
			serviceProxy.putMovePrediction(reducedXfen, (short) depth, mp);

			result = mp;
		} else {
			System.out.println("use mp from db");
			result = movePredictions[ThreadLocalRandom.current().nextInt(movePredictions.length)];
			
		}



		return result;


	}
}
