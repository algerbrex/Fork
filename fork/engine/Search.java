package fork.engine;

public class Search implements Runnable
{
    public final static int MAX_PLY              = 100;
    public final static int INFINITY             = 10000;
    private final static int CHECKMATE_THRESHOLD = 9000;
    private final static int DRAW                = 0;
    private final static int MAX_PV_LENGTH       = 50;

    public Position pos;
    public Timer timer;
    private long totalNodes;
    private long currSearchNodeCnt;
    private int[][] MVV_LVA = new int[][] {
        {0, 0, 0, 0, 0, 0},
        {0, 30, 31, 32, 33, 34},   // attacking pawn
        {0, 25, 26, 27, 28, 29},   // attacking knight
        {0, 20, 21, 22, 23, 24},   // attacking bishop
        {0, 15, 16, 17, 18, 19},   // attacking rook
        {0, 10, 11, 12, 13, 14},   // attacking queen
        {0, 5,  6,  7,  8,  9 },   // attacking king
    };

    private class PVLine 
    {
        public int[] moves;

        public PVLine() 
        {
            moves = new int[MAX_PV_LENGTH];
        }

        public int getBestMove() {
            return moves[0];
        }

        public void clear() 
        {
            moves = new int[MAX_PV_LENGTH];
        }

        public void updatePV(int bestMove, PVLine childPV)
        {
            moves[0] = bestMove;
            System.arraycopy(childPV.moves, 0, moves, 1, childPV.moves.length - 1);
        }

        public String toString() 
        {
            StringBuilder pv = new StringBuilder();
            for (int i = 0; i < MAX_PV_LENGTH; i++)
            {
                int move = moves[i];
                if (move == Move.NULL_MOVE)
                    break;
                pv.append(Move.toString(move));
                pv.append(" ");
            }

            return pv.toString();
        }
    }

    public Search() 
    {
        pos = new Position(Position.START_FEN);
        timer = new Timer();
    }

    public Search(String fen) 
    {
        pos = new Position(fen);
        timer = new Timer();
    }

    @Override
    public void run()
    {
        search();
    }

    public void stopSearch()
    {
        timer.forceStop();
    }

    public void search() 
    {
        PVLine pv = new PVLine();
        int bestMove = Move.NULL_MOVE;
        long totalTime = 0L;
        totalNodes = 0L;

        timer.start();

        for (int depth = 1; depth <= MAX_PLY && depth <= timer.maxDepth; depth++)
        {
            pv.clear();
            currSearchNodeCnt = 0L;

            // Put timer logic here to get best move if we run out of time.

            long startTime = System.currentTimeMillis();
            int score = negamax(pos, depth, 0, -INFINITY, INFINITY, pv);
            long endTime = System.currentTimeMillis();

            if (timer.isStopped())
            {
                if (bestMove == Move.NULL_MOVE && depth == 1)
                    bestMove = pv.getBestMove();
                break;
            }

            totalTime += (endTime - startTime) + 1;
            bestMove = pv.getBestMove();

            long nps = (totalNodes * 1000) / totalTime;
            totalNodes += currSearchNodeCnt;

            System.out.printf(
                "info depth %d score %s nodes %d nps %d time %d pv %s\n",
                depth, getMateOrCPScore(score), totalNodes, nps, totalTime, pv
            );
        }

        System.out.println("bestmove " + Move.toString(bestMove));
    }

    public int negamax(Position pos, int depth, int ply, int alpha, int beta, PVLine pv)
    {
        currSearchNodeCnt++;

        if (ply == MAX_PLY) 
            return Evaluation.evaluate(pos);

        if (depth == 0)
        {
            currSearchNodeCnt--;
            return quiescenceSearch(pos, alpha, beta, pv);
        }

        if (totalNodes + currSearchNodeCnt >= timer.maxNodeCount)
            timer.forceStop();

        if ((currSearchNodeCnt & 2047) == 0)
            timer.checkIfTimeIsUp();

        if (timer.isStopped())
            return 0;

        byte kingSq = Bitboard.findMSBPos(pos.pieces[Position.KING] & pos.sides[pos.stm]);
        boolean inCheck = MoveGen.sqIsAttacked(pos, pos.stm, kingSq);
        long pinned = pos.getPinnedPieces(pos.stm);

        MoveList moves = MoveGen.genAllMoves(pos);

        PVLine childPV = new PVLine();
        int bestScore = -INFINITY;
        int numLegalMoves = 0;

        scoreMoves(pos, moves.moves, moves.count);

        for (int i = 0; i < moves.count; i++)
        {

            swapBestMoveToIdx(moves.moves, moves.count, i);

            int move = moves.moves[i];
            Position newPos = pos.copy();

            if (!newPos.makeMove(move, inCheck, kingSq, pinned))
                continue;

            numLegalMoves++;

            int score = -negamax(newPos, depth - 1, ply + 1, -beta, -alpha, childPV);
            
            if (score > bestScore)
                bestScore = score;

            if (bestScore >= beta)
                break;

            if (bestScore > alpha)
            {
                alpha = bestScore;
                pv.updatePV(move, childPV);
            }

            childPV.clear();
        }

        if (numLegalMoves == 0)
        {
            if (inCheck)
                return -INFINITY + ply;
            return DRAW;
        }
        
        return bestScore;
    }

    public int quiescenceSearch(Position pos, int alpha, int beta, PVLine pv)
    {
        currSearchNodeCnt++;

        if (totalNodes + currSearchNodeCnt >= timer.maxNodeCount)
            timer.forceStop();

        if ((currSearchNodeCnt & 2047) == 0)
            timer.checkIfTimeIsUp();

        if (timer.isStopped())
            return 0;

        int bestScore = Evaluation.evaluate(pos);

        if (bestScore >= beta)
            return bestScore;

        if (alpha < bestScore)
            alpha = bestScore;

        byte kingSq = Bitboard.findMSBPos(pos.pieces[Position.KING] & pos.sides[pos.stm]);
        boolean inCheck = MoveGen.sqIsAttacked(pos, pos.stm, kingSq);
        long pinned = pos.getPinnedPieces(pos.stm);

        MoveList moves = MoveGen.genAllMoves(pos);
        PVLine childPV = new PVLine();

        scoreMoves(pos, moves.moves, moves.count);

        for (int i = 0; i < moves.count; i++)
        {
            if (Move.getMoveType(moves.moves[i]) != Move.ATTACK)
                continue; 

            swapBestMoveToIdx(moves.moves, moves.count, i);

            int move = moves.moves[i];
            Position newPos = pos.copy();

            if (!newPos.makeMove(move, inCheck, kingSq, pinned))
                continue;

            int score = -quiescenceSearch(newPos, -alpha, -beta, childPV);
            
            if (score > bestScore)
                bestScore = score;

            if (bestScore >= beta)
                break;

            if (bestScore > alpha)
            {
                alpha = bestScore;
                pv.updatePV(move, childPV);
            }

            childPV.clear();
        }

        return bestScore;
    }

    private void scoreMoves(Position pos, int[] moves, int numMoves)
    {
        for (int i = 0; i < numMoves; i++)
        {
            int move = moves[i];
            
            if (Move.getMoveType(moves[i]) == Move.ATTACK)
            {
                int from = Move.getFromSq(move), to = Move.getToSq(move);
                byte attackerType = pos.getPieceType(from), attackedType = pos.getPieceType(to);
                moves[i] = Move.addScore(move, MVV_LVA[attackerType][attackedType]);
            }
        }
    }

    private void swapBestMoveToIdx(int[] moves, int numMoves, int index)
    {
        int bestMoveScore = Move.getScore(moves[index]);
        int bestMoveIndex = index;

        for (int i = index + 1; i < numMoves; i++)
        {
            int moveScore = Move.getScore(moves[i]);
            if (moveScore > bestMoveScore) 
            {
                bestMoveScore = moveScore;
                bestMoveIndex = i;
            }
        }

        if (bestMoveIndex != index)
        {
            int bestMove = moves[bestMoveIndex];
            moves[bestMoveIndex] = moves[index];
            moves[index] = bestMove;
        }
    }

    // Display the correct format for the search score if it's a centipawn score
    // or a checkmate score.
    private String getMateOrCPScore(int score) 
    {
        if (score > CHECKMATE_THRESHOLD) 
        {
            int pliesToMate = INFINITY - score;
            int mateInN = (pliesToMate / 2) + (pliesToMate % 2);
            return String.format("mate %d", mateInN);
        }

        if (score < -CHECKMATE_THRESHOLD) 
        {
            int pliesToMate = -INFINITY - score;
            int mateInN = (pliesToMate / 2) + (pliesToMate % 2);
            return String.format("mate %d", mateInN);
        }

        return String.format("cp %d", score);
    }
}
