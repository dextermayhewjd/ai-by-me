package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.stream.Collector;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece;
public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "Jesus "; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		Move chosen =  moves.get(new Random().nextInt(moves.size()));

		// use visitor to see whether it is mrx turn to move
		Boolean MrxQues = chosen.visit(new Move.Visitor<Boolean>() {
			@Override
			public Boolean visit(Move.SingleMove move) {
				if (move.commencedBy()==Piece.MrX.MRX){
					return Boolean.TRUE;
				}
				else {
					return Boolean.FALSE;
				}
			}

			@Override
			public Boolean visit(Move.DoubleMove move) {
				return Boolean.TRUE;
			}
		});

		if (!MrxQues){
			return chosen;
		}else {
		List<Piece.Detective> detectivePieceList = new ArrayList<>();
		for (Piece.Detective detective: Piece.Detective.values()){
			if (board.getPlayers().contains(detective)){
				detectivePieceList.add(detective);
			}
		}

		//create a map which holds detectives and their locations
		// as a second parameter of ImmutableBoard
		Map<Piece.Detective,Integer> detectivesLocationMap = new HashMap<>();
				for (Piece.Detective d :detectivePieceList){
					detectivesLocationMap.put(d,board.getDetectiveLocation(d).orElseThrow());
				}
				ImmutableMap<Piece.Detective,Integer> ImuDetectivesLocationMap = ImmutableMap.copyOf(detectivesLocationMap);


		//create a map consists of Piece and its Tickets
		//key is the Piece and value is another Map of piece's tickets
		//tickets map hold the ticket type as key and number as its value
		Map<Piece,ImmutableMap<ScotlandYard.Ticket,Integer>> pieceTicketsMap = new HashMap<>();
		for (Piece everyPiece : board.getPlayers()){
			Map<ScotlandYard.Ticket,Integer> ownTicketsMap = new HashMap<>();
			for (ScotlandYard.Ticket t : ScotlandYard.Ticket.values()){
				ownTicketsMap.put(t,board.getPlayerTickets(everyPiece).orElseThrow().getCount(t));
			}
			ImmutableMap<ScotlandYard.Ticket,Integer> ImuOwnTicketMap = ImmutableMap.copyOf(ownTicketsMap);
			pieceTicketsMap.put(everyPiece,ImuOwnTicketMap);
		}
		ImmutableMap<Piece,ImmutableMap<ScotlandYard.Ticket,Integer>> ImuPieceTicketsMaP = ImmutableMap.copyOf(pieceTicketsMap);

		List<Player> detectivePlayersList = new ArrayList<>();
		for (Piece.Detective p : detectivePieceList){
			Player detective = new Player(p,ImuPieceTicketsMaP.get(p),board.getDetectiveLocation(p).get());
			detectivePlayersList.add(detective);
		}

		Piece mrx = Piece.MrX.MRX;
		Player Mrx = new Player(mrx,ImuPieceTicketsMaP.get(mrx),chosen.source());

		ImmutableBoard immutableBoard = new ImmutableBoard(board.getSetup(),ImuDetectivesLocationMap,ImuPieceTicketsMaP,board.getMrXTravelLog(),board.getWinner(),board.getAvailableMoves());

		GameState gameState = new CopyOfCwModel(immutableBoard,ImmutableSet.of(Mrx.piece()),Mrx,detectivePlayersList,0);
//				Node rootOfTree = new Node(gameState);
//		int depth = 1 + detectivePlayersList.size();
//				double score = minimax(rootOfTree,Mrx.location(),detectivePieceList,depth,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
//		chosen = findMrxMove(rootOfTree);


		double highestScore = 0;
		int moveIndex = 0;
		for (int i = 0;i <= moves.size()-1;i++){
			GameState possibleGameState = ((CopyOfCwModel) gameState).advance(moves.get(i));
			double currentScore = computeScore(possibleGameState, ((CopyOfCwModel) possibleGameState).getMrxLocation(),detectivePieceList);
			if (currentScore>highestScore){
				highestScore = currentScore;
				moveIndex = i;
			}
		}

		return moves.get(moveIndex);}
	}

	// use dijkstra's algorithm to compute score
	double computeScore(GameState gameState, int source, List<Piece.Detective> detectives){
		Set<Integer> nodes = gameState.getSetup().graph.nodes();
		int totalDistance = 0;
		for (Piece.Detective d : detectives){
			int destination = gameState.getDetectiveLocation(d).get();
			ArrayList<Integer> unComputedNodes = new ArrayList<>();
			for (Integer node : nodes){
				unComputedNodes.add(node);
			}


			// 	create a list of distances arraylist and set the distance to source to 0
			Integer maxNodeIndex = Collections.max(nodes);
			Integer [] distances = new Integer[maxNodeIndex + 1];
			for(int i = 0;i<=maxNodeIndex;i++){
				distances[i] = Integer.MAX_VALUE;
			}
			distances[source]=0;
			Integer currentNode = source;

			while (!unComputedNodes.isEmpty()&& currentNode!=destination){
				currentNode = findCurrentNode(distances);

				Set<Integer> adjNodes = gameState.getSetup().graph.adjacentNodes(currentNode);

				for (Integer adj : adjNodes){
					Integer disPlusOne = Integer.valueOf(distances[currentNode]+1);

					if (disPlusOne <distances[adj]&&isUnComputed(unComputedNodes,adj)) {
						distances[adj]=disPlusOne;
					}
				}
				unComputedNodes.remove(currentNode);
				distances[currentNode]=Integer.MAX_VALUE;
			}

			totalDistance +=distances[destination];
		}
		double score = totalDistance;
		return score;
	}
		//Find the node index with the shortest distance to the source node
		Integer findCurrentNode(Integer[] distance){
		Integer index = 0;
		for (int i = 0; i<distance.length;i++){
			if (distance[i] < distance[index]) {
			index = i;
			}
		}
		return index;
		}

		boolean isUnComputed(List<Integer> unCalculatedNodes, Integer n){
		for (Integer node :unCalculatedNodes){
			if (node.equals(n)){
				return true;
			}
		}
			return false;
		}




//	double computeScore(GameState gameState, int source, List<Piece.Detective> detectives){
//		Set<Integer> nodesInteger = gameState.getSetup().graph.nodes();
//
//		List<DisBoolNode> disBoolNodes = new ArrayList<>();
//
//		//create a list of destination of detectives' location
//		List<Integer> destination = new ArrayList<>();
//		for (Piece.Detective detective :detectives){
//			destination.add(gameState.getDetectiveLocation(detective).get());
//		}
//
//		// add a node that is just a place holder of which nodeIndex is 0 but not connected with any node
//
//		for (Integer node :nodesInteger){
//			DisBoolNode disBoolNode = new DisBoolNode(node);
//			disBoolNodes.add(disBoolNode);
//		}
//		DisBoolNode placeHolderNode = new DisBoolNode(0);
//		disBoolNodes.add(placeHolderNode);
//		Collections.sort(disBoolNodes);
//
//		List<DisBoolNode> unVisitedNodes = new ArrayList<>(disBoolNodes);
//		unVisitedNodes.remove(placeHolderNode);
//
//
//		//set the distance to each node to infinity
//		for (DisBoolNode everyNode : disBoolNodes){
//			everyNode.setDistance(Integer.MAX_VALUE);
//		}
//
//		disBoolNodes.get(source).setDistance(0);
//
//		while (!unVisitedNodes.isEmpty()){
//			Integer currentNode;
//			currentNode = findTheShortestNode(disBoolNodes,unVisitedNodes);
//			Set<Integer> relatedNodes = gameState.getSetup().graph.adjacentNodes(currentNode);
//			for (Integer relatedNode :relatedNodes){
//				// if this node's distance from source is larger than currentnode  + 1
//				// update the
//				Integer changedDistance = Integer.valueOf(disBoolNodes.get(currentNode).getDistance()+1);
//				if (disBoolNodes.get(relatedNode).getDistance()>changedDistance){
//					disBoolNodes.get(relatedNode).setDistance(changedDistance);
//				}
//			}
//			unVisitedNodes = toListWithoutNodeIndexEqual(unVisitedNodes,currentNode);
//			disBoolNodes.get(currentNode).ChangeCheckState();
//
//		}
//
//		double DisFromMrxToAllDetective = 0;
//		// go through all the detectives ' location
//		// if any one of the distance from source to it is 0
//		// break from the current loop and set the score to zero
//		for (Integer dest : destination){
//			if (disBoolNodes.get(dest).getDistance()==0){
//				DisFromMrxToAllDetective = 0;
//				break;
//			}
//			DisFromMrxToAllDetective += disBoolNodes.get(dest).getDistance();
//		}
//
//		return DisFromMrxToAllDetective;
//	}
//	// need to find the current node which has the shortest distance from the source node
//	Integer findTheShortestNode(List<DisBoolNode> Nodes , List<DisBoolNode> unvisitedNodes){
//		int currentNode = unvisitedNodes.get(0).nodeIndex;
//		for (int i = 1; i < Nodes.size(); i++) {
//			if (Nodes.get(i).getCheckState() == false) {
//				if (Nodes.get(i).getDistance()<Nodes.get(currentNode).getDistance()) {
//					currentNode = i ;
//				}
//			}
//		}
//		return Integer.valueOf(currentNode);
//	}

//	public List<DisBoolNode> toListWithoutNodeIndexEqual(List<DisBoolNode> Nodes,Integer integer){
//		List<DisBoolNode> beforeRemove = new ArrayList<>(Nodes);
//		for (DisBoolNode d : Nodes){
//			if (d.getNodeIndex()==integer){
//				beforeRemove.remove(d);
//			}
//		}
//		return beforeRemove;
//	}
//
//	DisBoolNode getNodeWithIndexNumber(List<DisBoolNode> Nodes, Integer integer){
//		Integer nodeOriginNumber = -1;
//		for (int i = 0; i<Nodes.size();i++){
//			if (Nodes.get(i).getNodeIndex()==integer){
//				nodeOriginNumber = i ;
//			}
//		}
//		return Nodes.get(nodeOriginNumber);
//	}




//	// need to find the current node which has the shortest distance from the source node
//		Integer findTheShortestNode(List<Integer> distance ) {
//			Integer minimumDistanceIndex = 0;
//			for (int i = 0;i < distance.size(); i++){
//				if (distance.get(i)<distance.get(minimumDistanceIndex)){
//					minimumDistanceIndex
//				}
//			}
//	}
//
//		List<DisBoolNode> toListWithoutNodeIndexEqual(List<DisBoolNode> Nodes,Integer currentNode){
//		List<DisBoolNode> beforeRemove = new ArrayList<>(Nodes);
//		for (DisBoolNode d : Nodes){
//				if (d.getNodeIndex()==currentNode){
//					beforeRemove.remove(d);
//				}
//			}
//			return beforeRemove;
//		}
//
//		public DisBoolNode getNodeWithIndexNumber(List<DisBoolNode> Nodes, Integer integer){
//			Integer nodeOriginNumber = -1;
//		for (int i = 0; i<Nodes.size();i++){
//				if (Nodes.get(i).getNodeIndex()==integer){
//					nodeOriginNumber = i ;
//				}
//			}
//		return Nodes.get(nodeOriginNumber);
//		}
//
		double minimax(Node currentGsNode, int mrxLocation, List<Piece.Detective>detectives,int remainingDepth,double alpha,double beta){
		GameState state = currentGsNode.getGameState();

		if (remainingDepth == 0){
			double score = computeScore(state,mrxLocation,detectives);
			currentGsNode.setScore(score);
			return score;
		}else {
			var moves = state.getAvailableMoves().asList();
			if (!moves.isEmpty()) {
				Move whoseMove = moves.get(new Random().nextInt(moves.size()));

				// in miniMax    if move commenced by mrx   Max turn
				if (whoseMove.commencedBy().isMrX()) {
					// initialize Mrx's score to infinity and resign the highest score of its childGSNode
					double maxEval = Double.NEGATIVE_INFINITY;

					for (Move move : moves) {
						Node childGSNode = new Node(state.advance(move));
						childGSNode.setMove(move);
						currentGsNode.addSubNode(childGSNode);

						int newDestination = move.visit(new Move.Visitor<Integer>() {
							@Override
							public Integer visit(Move.SingleMove move) {
								return move.destination;
							}

							@Override
							public Integer visit(Move.DoubleMove move) {
								return move.destination2;
							}
						});

						mrxLocation = newDestination;

						double childNodeScore = minimax(childGSNode, mrxLocation, detectives, remainingDepth - 1, alpha, beta);
						//update the maxEval because mrx would always choose the higher one
						//since it is mrx 's turn
						maxEval = Math.max(maxEval, childNodeScore);
						currentGsNode.setScore(maxEval);
						alpha = Math.max(alpha, childNodeScore);
						if (beta <= alpha) {
							break;
						}
					}
					return maxEval;
				} else {
					// detectives' turn in minimax algorithm
					// should be mini turn
					double minEval = Double.POSITIVE_INFINITY;
					List<Piece.Detective> holdDetective = new ArrayList<>();
					List<List<Piece.Detective>> allPossibleDetectiveOrder = new ArrayList<>();
					List<List<Piece.Detective>> detectivesMoveOrderList = permutationOfDetectives(detectives,holdDetective,allPossibleDetectiveOrder);
					for (List<Piece.Detective> l : detectivesMoveOrderList){
						for (int i = 0; i<l.size();i++){
							Node childGsNode = new Node(state);
							List<Move> movesForCurrentDetective = new ArrayList<>();
							for (Move move : moves) {
								if (move.commencedBy() == l.get(i)) {
									movesForCurrentDetective.add(move);
								}
								for (Move m : movesForCurrentDetective){
									Node childGSNode = new Node(state.advance(m));
							}
						}
					}
					Piece currentDetective = whoseMove.commencedBy();

					// because get available moves for detectives would show move for every detective
					// need to filter that

//
//					for (Move move : movesForCurrentDetective) {
//						Node childGSNode = new Node(state.advance(move));
//						childGSNode.setMove(move);
//						currentGsNode.addSubNode(childGSNode);

						double childNodeScore = minimax(childGSNode, mrxLocation, detectives, remainingDepth - 1, alpha, beta);
						//update the minEval because detective would always choose the lower one
						//since it is detective 's turn
						minEval = Math.min(minEval, childNodeScore);

						beta = Math.min(beta, childNodeScore);
						if (beta <= alpha) {
							break;
						}
					}
					return minEval;
				}

			} else {
				double score = computeScore(state,mrxLocation,detectives);
				currentGsNode.setScore(score);
				return score;
				}
			}
		}

		public List<List<Piece.Detective>> permutationOfDetectives (List<Piece.Detective> detectivesPieceList,List<Piece.Detective> holdDetective,List<List<Piece.Detective>> allPossibleDetectiveOrder){
		if (detectivesPieceList.isEmpty()){
			allPossibleDetectiveOrder.add(holdDetective);
		}
			for (int i = 0; i<detectivesPieceList.size();i++){
				List<Piece.Detective> ListWithMinusOneDetective = new ArrayList<>(detectivesPieceList);
				ListWithMinusOneDetective.remove(detectivesPieceList.get(i));
				holdDetective.add(detectivesPieceList.get(i));
				permutationOfDetectives(ListWithMinusOneDetective,holdDetective,allPossibleDetectiveOrder);
			}

		return allPossibleDetectiveOrder;
		}












//		Move findMrxMove(Node currentGSNode){
//			Move mrxMove = null;
//			double maxScore = 0 ;
//			for (Node N : currentGSNode.getSubNodes()){
//				if (N.getScore()>maxScore){
//					mrxMove = N.getMove();
//				}
//			}
//			return mrxMove;
//	}

//
//
//
//
//
//
//
//
//		// a Node that contains the distance to it and whether it is visited or not
//	private class DisBoolNode implements Comparable<DisBoolNode> {
//		private Integer nodeIndex;
//		private Boolean checkState;
//		private Integer distance;
//
//		private DisBoolNode(Integer nodeIndex){
//			this.checkState = Boolean.FALSE;
//			this.distance = Integer.MAX_VALUE;
//			this.nodeIndex = nodeIndex;
//		}
//			private void ChangeCheckState(){
//				if (this.checkState == Boolean.FALSE){
//					this.checkState = Boolean.TRUE;
//				}else {
//					this.checkState = Boolean.FALSE;
//				}
//			}
//
//			private void setDistance(Integer distance){
//			this.distance = distance;
//			}
//
//			private Boolean getCheckState() {
//				return checkState;
//			}
//			private Integer getDistance(){
//				return distance;
//			}
//			private  Integer getNodeIndex(){
//				return nodeIndex;
//			}
//
//			@Override
//			public int compareTo(DisBoolNode that) {
//			return (this.getNodeIndex()-that.getNodeIndex());
//			}
//		}







	private class Node {
		private List<Node> subNodes = new ArrayList<>();
		private GameState gameState;
		private double score;
		private Move move;

		private Node(GameState gameState) {
			this.gameState = gameState;
		}

		void addSubNode (Node node){
			subNodes.add(node);
		}
		void setScore (double score){
			this.score = score;
		}
		void setMove (Move move){this.move = move;}
		List<Node> getSubNodes(){
			return this.subNodes;
		}
		double getScore(){return  this.score;}
		GameState getGameState(){
			return this.gameState;
		}
		Move getMove(){return this.move;}
	}
	public class CopyOfCwModel implements GameState{

		Board board;
		public GameSetup setup;
		public ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		public Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		// rounds number
		private int roundIndex;

		public CopyOfCwModel(
				 Board board,
				 ImmutableSet<Piece> remaining,

				 Player mrX,
				 List<Player> detectives,
				// rounds number
				 int roundIndex
		){
			this.board = board;
			this.setup = getSetup();
			this.remaining = remaining;
			this.log = board.getMrXTravelLog();
			this.mrX = mrX;
			this.detectives = detectives;
			this.roundIndex = roundIndex;

			List<Player> everyOne = new ArrayList<>();
			everyOne.add(mrX);
			everyOne.addAll(detectives);
			everyone= ImmutableList.copyOf(everyOne);
			this.moves=board.getAvailableMoves();
		}
		public Integer getMrxLocation(){
			return mrX.location();
		}
		public Boolean DoesHeHasTickets(Player player){
			int toolTicket = 0;
			if (player.has(ScotlandYard.Ticket.TAXI)){
				toolTicket += 1;
			}
			if (player.has(ScotlandYard.Ticket.BUS)){
				toolTicket += 1;
			}
			if (player.has(ScotlandYard.Ticket.UNDERGROUND)){
				toolTicket += 1;
			}
			if (toolTicket == 0){
				return false;
			}else{
				return true;
			}
		}

		public Player makePlayer(Move move) {
			Piece PieceOfThisPlayer = move.commencedBy();

			for (Player Detective : detectives) {
				if (PieceOfThisPlayer == Detective.piece()) {
					return Detective;
				}
			}
			return mrX;
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {

			ImmutableSet<Move> moves = ImmutableSet.copyOf(getAvailableMoves());
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			//to know what the piece is
			Piece pieceWhoAdvance = move.commencedBy();

			//to know who the player is
			Player OfThisMove = makePlayer(move);

			//to know the original location
			int originalLocation = move.source();

			//make a list of the tickets consumed in move 可变的用过的ticket list
			ImmutableList<ScotlandYard.Ticket> copyOfTicketsUsed = ImmutableList.copyOf(move.tickets());
			List<ScotlandYard.Ticket> TicketsUsed = new ArrayList<>(copyOfTicketsUsed);


			//make a map of the tickets of the moved player 这个Player所拥有的tickets Map
			ImmutableMap<ScotlandYard.Ticket, Integer> copyOfAdvancePlayerTickets = ImmutableMap.copyOf(OfThisMove.tickets());
			Map<ScotlandYard.Ticket, Integer> OfAdvancePlayerTickets = new HashMap<>(copyOfAdvancePlayerTickets);

			//get the distination interger  这个是关于最后终点的 目前没问题
			int distinctionOfMove = move.visit(new Move.Visitor<Integer>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			});

			// got the mounts of Player Tickets 票可能有问题 此为返回player移动过后的Tickets
			Map<ScotlandYard.Ticket, Integer> AfterOfAdvancePlayerTickets = move.visit(new Move.Visitor<Map<ScotlandYard.Ticket, Integer>>() {
				@Override
				public Map<ScotlandYard.Ticket, Integer> visit(Move.SingleMove move) {
					ScotlandYard.Ticket ticketFirstUsed = TicketsUsed.get(0);
					int numberOfTicketFirstUsed = OfAdvancePlayerTickets.get(ticketFirstUsed) - 1;
					OfAdvancePlayerTickets.replace(ticketFirstUsed, numberOfTicketFirstUsed);
					return OfAdvancePlayerTickets;
				}

				//if statement here because mrx may used two same tickets in a double move
				@Override
				public Map<ScotlandYard.Ticket, Integer> visit(Move.DoubleMove move) {
					ScotlandYard.Ticket ticketFirstUsedInDouble = TicketsUsed.get(0);
					ScotlandYard.Ticket ticketSecondUsedInDouble = TicketsUsed.get(1);
					ScotlandYard.Ticket ticketMaybeDouble = TicketsUsed.get(2);
					if (ticketFirstUsedInDouble.equals(ticketSecondUsedInDouble)) {
						int twoSameTickets = OfAdvancePlayerTickets.get(ticketFirstUsedInDouble) - 2;
						int numberOfTicketDoubleUsedInDouble = OfAdvancePlayerTickets.get(ticketMaybeDouble) - 1;
						OfAdvancePlayerTickets.replace(ticketFirstUsedInDouble, twoSameTickets);
						OfAdvancePlayerTickets.replace(ticketMaybeDouble, numberOfTicketDoubleUsedInDouble);
						return OfAdvancePlayerTickets;
					} else {
						int numberOfTicketFirstUsedInDouble = OfAdvancePlayerTickets.get(ticketFirstUsedInDouble) - 1;
						int numberOfTicketSecondUsedInDouble = OfAdvancePlayerTickets.get(ticketSecondUsedInDouble) - 1;
						int numberOfTicketDoubleUsedInDouble = OfAdvancePlayerTickets.get(ticketMaybeDouble) - 1;
						OfAdvancePlayerTickets.replace(ticketFirstUsedInDouble, numberOfTicketFirstUsedInDouble);
						OfAdvancePlayerTickets.replace(ticketSecondUsedInDouble, numberOfTicketSecondUsedInDouble);
						OfAdvancePlayerTickets.replace(ticketMaybeDouble, numberOfTicketDoubleUsedInDouble);
						return OfAdvancePlayerTickets;
					}
				}
			});

			ImmutableMap<ScotlandYard.Ticket, Integer> ImuAfterOfAdvancePlayerTickets = ImmutableMap.copyOf(AfterOfAdvancePlayerTickets);

			// Player ofThisMoveAfter is a player with tickets used and at a new location
			// 如果这是mrX更新他的状态 如果他是一个detective 更新他的状态
			// create a new list of detectives
			// if this player is a detective then create a new detective and replace it with the new one
			List<Player> newDetectivesWMD = new ArrayList<>(detectives);

			if (OfThisMove.isDetective()) {
				Player newOfThisMove = new Player(pieceWhoAdvance, ImuAfterOfAdvancePlayerTickets, distinctionOfMove);
				newDetectivesWMD.add(newOfThisMove);
				newDetectivesWMD.remove(OfThisMove);
			}


			// delete the Player piece from remaining
			// if this is a detective , create a new remaining list and remove it
			// if this is the last one in the remaining list
			// remove it and add mrx in to it
			//if piece who advance is mrx, add all detectives to the remaining
			if (pieceWhoAdvance.isDetective()) {
				ImmutableSet<Piece> copyOfRemaining = ImmutableSet.copyOf(remaining);
				Set<Piece> newRemaining = new HashSet<>(copyOfRemaining);
				newRemaining.remove(OfThisMove.piece());

				List<Player> movedDetectives = new ArrayList<>(detectives);
				List<Player> notMovedDetectives = new ArrayList<>();
				for (Player detective : detectives) {
					if (remaining.contains(detective.piece())){
						notMovedDetectives.add(detective);
					}
				}
				for (Player detective: notMovedDetectives){
					if (movedDetectives.contains(detective)){
						movedDetectives.remove(detective);
					}
				}

				for (Player notMovedDetective : notMovedDetectives){
					if (!DoesHeHasTickets(notMovedDetective)){
						newRemaining.remove(notMovedDetective.piece());
					}
				}

				if (newRemaining.isEmpty()) {
					newRemaining.add(mrX.piece());
				}
				Player mrXWithMoreTicket = mrX.give(move.tickets());
				ImmutableSet <Piece>ImuNewRemaining = ImmutableSet.copyOf(newRemaining);

				//To create a immutableBoard , second parameter
				// in this case list detectives have changed so use newDetectivesWMD
				Map<Piece.Detective,Integer> detectivesLocation = new HashMap<>();
				for (Player d : newDetectivesWMD){
					detectivesLocation.put((Piece.Detective) d.piece(),getDetectiveLocation(((Piece.Detective) d.piece())).orElseThrow());
				}
				ImmutableMap<Piece.Detective,Integer> ImuDetectiveLocation = ImmutableMap.copyOf(detectivesLocation);

				//To create a immutableBoard , third parameter

				//create a map consists of Piece and its Tickets
				//key is the Piece and value is another Map of piece's tickets
				//tickets map hold the ticket type as key and number as its value
				Map<Piece,ImmutableMap<ScotlandYard.Ticket,Integer>> pieceTicketsMap = new HashMap<>();
				for (Player player : newDetectivesWMD){
					pieceTicketsMap.put(player.piece(),player.tickets());
				}
				pieceTicketsMap.put(Piece.MrX.MRX,mrX.tickets());
				ImmutableMap<Piece,ImmutableMap<ScotlandYard.Ticket,Integer>> ImuPieceTicketsMaP = ImmutableMap.copyOf(pieceTicketsMap);

				ImmutableBoard ImuBoard = new ImmutableBoard(board.getSetup(),ImuDetectiveLocation,ImuPieceTicketsMaP,log,board.getWinner(),board.getAvailableMoves());
				return new CopyOfCwModel(ImuBoard, ImuNewRemaining,  mrXWithMoreTicket, newDetectivesWMD, roundIndex);

			} else {
				Set<Piece> newRemaining = new HashSet<>();
				for (Player everyDetective : detectives) {
					newRemaining.add(everyDetective.piece());
				}

				ImmutableSet<Piece>ImuNewRemaining = ImmutableSet.copyOf(newRemaining);
				Player MrxNew = new Player(pieceWhoAdvance, ImuAfterOfAdvancePlayerTickets, distinctionOfMove);

				//To create a immutableBoard , second parameter
				// in this case detectives have not moved so we use detectives
				Map<Piece.Detective,Integer> detectivesLocation = new HashMap<>();
				for (Player d : detectives){
					detectivesLocation.put((Piece.Detective) d.piece(),getDetectiveLocation(((Piece.Detective) d.piece())).orElseThrow());
				}
				ImmutableMap<Piece.Detective,Integer> ImuDetectiveLocation = ImmutableMap.copyOf(detectivesLocation);


				//To create a immutableBoard , third parameter

				//create a map consists of Piece and its Tickets
				//key is the Piece and value is another Map of piece's tickets
				//tickets map hold the ticket type as key and number as its value
				Map<Piece,ImmutableMap<ScotlandYard.Ticket,Integer>> pieceTicketsMap = new HashMap<>();
				for (Player player : detectives){
					pieceTicketsMap.put(player.piece(),player.tickets());
				}
				pieceTicketsMap.put(Piece.MrX.MRX,MrxNew.tickets());
				ImmutableMap<Piece,ImmutableMap<ScotlandYard.Ticket,Integer>> ImuPieceTicketsMaP = ImmutableMap.copyOf(pieceTicketsMap);

				if (move.visit(new Move.Visitor<Boolean>() {
					@Override
					public Boolean visit(Move.SingleMove move) {
						return true;
					}

					@Override
					public Boolean visit(Move.DoubleMove move) {
						return false;
					}
				})) {
					//		如果这一论是显示的
					// If this round is reveal round
					ImmutableList<LogEntry> copyOfOldLog = ImmutableList.copyOf(log);
					List<LogEntry> newLog = new ArrayList<>(copyOfOldLog);
					if (setup.rounds.get(roundIndex)) {
						LogEntry logEntryOfThisTurn = LogEntry.reveal(TicketsUsed.get(0), distinctionOfMove);
						newLog.add(logEntryOfThisTurn);
					} else {
						LogEntry logEntryOfThisTurn = LogEntry.hidden(TicketsUsed.get(0));
						newLog.add(logEntryOfThisTurn);
					}

					ImmutableList<LogEntry> ImuNewLog = ImmutableList.copyOf(newLog);
					int roundIndexPlusOne = roundIndex + 1;


					ImmutableBoard ImuBoard = new ImmutableBoard(board.getSetup(),ImuDetectiveLocation,ImuPieceTicketsMaP,ImuNewLog,board.getWinner(),board.getAvailableMoves());
					return new CopyOfCwModel(ImuBoard, ImuNewRemaining, MrxNew, detectives, roundIndexPlusOne);

				}

				// if this is a double move's log
				else {

					ImmutableList<LogEntry> copyOfOldLog = ImmutableList.copyOf(log);
					List<LogEntry> newLog = new ArrayList<>(copyOfOldLog);
					int roundIndexForFirstMove = roundIndex;
					int roundIndexForSecondMove = roundIndex + 1;

					if (setup.rounds.get(roundIndexForFirstMove)) {
						LogEntry logEntryOfDoubleFirst = LogEntry.reveal(TicketsUsed.get(0), ((Move.DoubleMove) move).destination1);
						newLog.add(logEntryOfDoubleFirst);
						if (setup.rounds.get(roundIndexForSecondMove)) {
							LogEntry logEntryOfDoubleDouble = LogEntry.reveal(TicketsUsed.get(1), distinctionOfMove);
							newLog.add(logEntryOfDoubleDouble);
						} else {
							LogEntry logEntryOfDoubleDouble = LogEntry.hidden(TicketsUsed.get(1));
						}
					} else {
						// this is for the log of doublemove's single move log
						LogEntry logEntryOfDoubleFirst = LogEntry.hidden(TicketsUsed.get(0));
						newLog.add(logEntryOfDoubleFirst);
						if (setup.rounds.get(roundIndexForSecondMove)) {
							LogEntry logEntryOfDoubleDouble = LogEntry.reveal(TicketsUsed.get(1), distinctionOfMove);
							newLog.add(logEntryOfDoubleDouble);
						} else {
							LogEntry logEntryOfDoubleDouble = LogEntry.hidden(TicketsUsed.get(1));
							newLog.add(logEntryOfDoubleDouble);
						}
					}
					ImmutableList<LogEntry> ImuNewLog = ImmutableList.copyOf(newLog);
					int roundIndexPlusTwo = roundIndex + 2;

					ImmutableBoard ImuBoard = new ImmutableBoard(board.getSetup(),ImuDetectiveLocation,ImuPieceTicketsMaP,ImuNewLog,board.getWinner(),board.getAvailableMoves());
					return new CopyOfCwModel(ImuBoard, ImuNewRemaining,MrxNew, detectives, roundIndexPlusTwo);

				}
			}
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return this.board.getSetup();
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			return board.getPlayers();
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			return board.getDetectiveLocation(detective);
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			return board.getPlayerTickets(piece);
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return board.getMrXTravelLog();
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return board.getWinner();
		}

		private ImmutableSet<Move.SingleMove> makeSingleMoves(
				GameSetup setup,
				List<Player> detectives,
				Player player,
				int source) {
			final var singleMoves = new ArrayList<Move.SingleMove>();
			for (int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the list of moves to return
				int i = 0;
				for (Player detective : detectives) {
					if (destination == detective.location()) {
						i += 1;
					}
				}
				if (i == 0) {
					for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						// TODO find out if the player has the required tickets
						//  if it does, construct SingleMove and add it the list of moves to return
						if (player.has(t.requiredTicket())) {
							singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}

					}
					// TODO consider the rules of secret moves here
					//  add moves to the destination via a secret ticket if there are any left with the player
					// houlaijiade
					if (player.has(ScotlandYard.Ticket.SECRET)) {
						singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
					}
					//houlaijiade
				}
			}
			return ImmutableSet.copyOf(singleMoves);
		}

		private ImmutableSet<Move.DoubleMove> makeDoubleMoves(
				GameSetup setup,
				List<Player> detectives,
				Player player,
				int source) {
			final var DoubleMoves = new ArrayList<Move.DoubleMove>();
			if (player.has(ScotlandYard.Ticket.DOUBLE)) {
//					Set<SingleMove> copyOfSingleMove = ImmutableSet.copyOf(makeSingleMoves(setup, detectives, player, source));
				ImmutableSet<Move.SingleMove> firstMove = ImmutableSet.copyOf(makeSingleMoves(setup, detectives, player, source));
				Set<Move.SingleMove> copyOfSingleMove = new HashSet<>(firstMove);
				for (Move.SingleMove sm : copyOfSingleMove) {

					int ds1 = sm.destination;
					ScotlandYard.Ticket ticket1 = sm.ticket;

					// this map represent the map has already used his ticket

//						Map<Ticket, Integer> ticketMapUsed = ImmutableMap.copyOf(player.tickets());
					ImmutableMap<ScotlandYard.Ticket, Integer> imuTicketMapUsed = ImmutableMap.copyOf(player.tickets());
					Map<ScotlandYard.Ticket, Integer> ticketMapUsed = new HashMap<>(imuTicketMapUsed);
					int ticket1mis1 = ticketMapUsed.get(ticket1) - 1;
					int ticketDouble = ticketMapUsed.get(ScotlandYard.Ticket.DOUBLE) - 1;
					ticketMapUsed.replace(ticket1, ticket1mis1);
					ticketMapUsed.replace(ScotlandYard.Ticket.DOUBLE, ticketDouble);
					ImmutableMap<ScotlandYard.Ticket, Integer> tic1 = ImmutableMap.copyOf(ticketMapUsed);

					//this player assume the player has already use his first ticket in first move

					Player playerUFT = new Player(player.piece(), tic1, ds1);

//					 Set<SingleMove> copySecondMove1 = ImmutableSet.copyOf(makeSingleMoves(setup, detectives, playerUFT, ds1));
					ImmutableSet<Move.SingleMove> ImuCopySecondMove =ImmutableSet.copyOf(makeSingleMoves(setup, detectives, playerUFT, ds1));
					Set<Move.SingleMove> copySecondMove = new HashSet<>(ImuCopySecondMove);
					for (Move.SingleMove secondMove : copySecondMove) {
						DoubleMoves.add(new Move.DoubleMove(player.piece(), source, ticket1, ds1, secondMove.ticket, secondMove.destination));
					}
				}
			}
			return ImmutableSet.copyOf(DoubleMoves);

		}




		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {

			//for detectives

			if (remaining.contains(mrX.piece())) {
				Set<Move> MrxMove = new HashSet<>();
				//THERE MAY BE SOME PROBLEMS
				if (getWinner().isEmpty()) {
					//THE CODE ABOVE
					if (setup.rounds.size() > roundIndex) {
						MrxMove.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
						if (setup.rounds.size() > roundIndex + 1) {
							MrxMove.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
						}
					}
				}
				ImmutableSet<Move> finMrxMove = ImmutableSet.copyOf(MrxMove);
				return finMrxMove;

			} else {
				Set<Move> detectiveMove = new HashSet<>();
				//This code is dangerous
				if (getWinner().isEmpty()) {
					//the code above
					for (Piece detective : remaining) {
						for (Player de : detectives) {
							if (de.piece() == detective) {
								detectiveMove.addAll(makeSingleMoves(setup, detectives, de, de.location()));
							}
						}
					}
				}
				ImmutableSet<Move> finDetectiveMove = ImmutableSet.copyOf(detectiveMove);
				return finDetectiveMove;
			}

			//For Mrx


		}


	}
}
