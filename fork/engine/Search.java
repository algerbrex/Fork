package fork.engine;

public class Search implements Runnable
{
    public final static int MAX_PLY              = 100;
    public final static int INFINITY             = 10000;
    private final static int CHECKMATE_THRESHOLD = 9000;
    private final static int DRAW                = 0;
    private final static int MAX_PV_LENGTH       = 50;
    private final static int MAX_GAME_PLY        = 700;

    public Position pos;
    public Timer timer;
    private long totalNodes;
    private long currSearchNodeCnt;
    private int[][] MVV_LVA = new int[][] {
        {30, 31, 32, 33, 34, 0, 0},   // attacking pawn
        {25, 26, 27, 28, 29, 0, 0},   // attacking knight
        {20, 21, 22, 23, 24, 0, 0},   // attacking bishop
        {15, 16, 17, 18, 19, 0, 0},   // attacking rook
        {10, 11, 12, 13, 14, 0, 0},   // attacking queen
        {5,  6,  7,  8,  9 , 0, 0},   // attacking king
    };

    private int zobristHistoryPly;
    private long[] zobristHistory;

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
        setup(Position.START_FEN);
    }

    public Search(String fen) 
    {
        setup(fen);
    }

    public void setup(String fen)
    {
        pos = new Position(fen);
        timer = new Timer();

        zobristHistoryPly = 0;
        zobristHistory = new long[MAX_GAME_PLY];
        zobristHistory[zobristHistoryPly] = pos.hash;
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

    public void addHistory(long hash)
    {
        zobristHistoryPly++;
        zobristHistory[zobristHistoryPly] = hash;
    }

    public void removeHistory()
    {
        zobristHistoryPly--;
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

        byte kingSq = Bitboard.findMSBPos(pos.pieces[Position.KING] & pos.sides[pos.stm]);
        boolean inCheck = MoveGen.sqIsAttacked(pos, pos.stm, kingSq);

        if (inCheck)
            depth++;

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

        long pinned = pos.getPinnedPieces(pos.stm);
        boolean isRoot = ply == 0;

        boolean possibleMateInOne = inCheck && ply == 1;
        if (!isRoot && ((pos.rule50 == 100 && !possibleMateInOne) || posIsDrawByRepition(pos)))
            return DRAW;

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

            addHistory(newPos.hash);
            numLegalMoves++;

            int score = -negamax(newPos, depth - 1, ply + 1, -beta, -alpha, childPV);

            removeHistory();
            
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

        MoveList moves = MoveGen.genAttacks(pos);
        PVLine childPV = new PVLine();

        scoreMoves(pos, moves.moves, moves.count);

        for (int i = 0; i < moves.count; i++)
        {
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

    private boolean posIsDrawByRepition(Position pos)
    {
        for (int i = 0; i < zobristHistoryPly; i++)
            if (zobristHistory[i] == pos.hash)
                return true;

        return false;
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
