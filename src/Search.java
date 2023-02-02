public class Search 
{
    private final static int MAX_PLY       = 100;
    private final static int MAX_PV_LENGTH = 50;
    public int INFINITY      = 10000;

    private Position pos;
    private long totalNodes;

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
    }

    public Search(String fen) 
    {
        pos = new Position(fen);
    }

    public int search(int searchDepth) 
    {
        PVLine pv = new PVLine();
        int bestMove = Move.NULL_MOVE;
        long totalTime = 0L;

        for (int depth = 1; depth <= searchDepth; depth++)
        {
            pv.clear();

            // Put timer logic here to get best move if we run out of time.

            long startTime = System.currentTimeMillis();
            int score = negamax(pos, depth, 0, pv);
            long endTime = System.currentTimeMillis();

            totalTime += (endTime - startTime);
            bestMove = pv.getBestMove();

            long nps = (totalNodes * 1000) / totalTime;

            System.out.printf(
                "info depth %d score %s nodes %d nps %d time %d pv %s\n",
                depth, score, totalNodes, nps, totalTime, pv
            );
        }

        return bestMove;
    }

    public int negamax(Position pos, int depth, int ply, PVLine pv)
    {
        totalNodes++;

        if (depth == 0 || ply == MAX_PLY)
            return Evaluation.evaluate(pos);

        byte kingSq = Bitboard.findMSBPos(pos.pieces[Position.KING] & pos.sides[pos.stm]);
        boolean inCheck = MoveGen.sqIsAttacked(pos, pos.stm, kingSq);

        MoveList moves = MoveGen.genAllMoves(pos);

        PVLine childPV = new PVLine();
        int bestScore = -INFINITY;
        int numLegalMoves = 0;

        for (int i = 0; i < moves.count; i++)
        {
            int move = moves.moves[i];
            Position newPos = pos.copy();

            if (!newPos.makeMove(move, inCheck, kingSq))
                continue;

            numLegalMoves++;

            int score = -negamax(newPos, depth - 1, ply + 1, childPV);

            if (score > bestScore)
            {
                bestScore = score;
                pv.updatePV(move, childPV);
            }

            childPV.clear();
        }

        if (inCheck && numLegalMoves == 0)
            return -INFINITY + ply;

        return bestScore;
    }
}
