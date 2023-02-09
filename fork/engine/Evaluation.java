package fork.engine;

public class Evaluation 
{
    public static final int PAWN_VALUE   = 100;
    public static final int KNIGHT_VALUE = 300;
    public static final int BISHOP_VALUE = 320;
    public static final int ROOK_VALUE   = 500;
    public static final int QUEEN_VALUE  = 950;

    private static final int[][] PSQT = new int[][] {
        // Pawn PSQT
        {
            50, 50, 50, 50, 50, 50, 50, 50,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
           -5, -5,  0,  25, 25, 0, -5, -5,
           -5, -5,  5,  10, 10, 5, -5, -5,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
        },

        // Knight PSQT
        {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  15, 15, 15, 15, 0,  0,
            0,  0,  15, 0,  0,  15, 0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
        },

        // Bishop PSQT
        {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  15, 10, 10, 15, 0,  0,
            0,  0,  15, 10, 10, 15, 0,  0,
            0,  15, 0,  0,  0,  0,  15, 0,
            0,  0,  0,  0,  0,  0,  0,  0,
        },

        // Rook PSQT
        {
            25, 25, 15, 5,  5,  15, 25, 25,
            25, 25, 15, 5,  5,  15, 25, 25,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  50, 50, 0,  0,  0,
            0,  0,  0,  50, 50, 0,  0,  0,
        },

        // Queen PSQT
        {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  20, 10, 10, 20, 0,  0,
            0,  0,  20, 15, 15, 20, 0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
        },

        // King PSQT
        {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0, -25,-25, 0,  0,  0,
            0,  0,  0, -25,-25, 0,  0,  0,
            50, 50, 0,  0,  0,  0,  50, 50,
        },
    };

    private static final int[][] flipSq = new int[][]{
        {
            0,  1,  2,  3,  4,  5,  6,  7,
            8,  9,  10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23,
            24, 25, 26, 27, 28, 29, 30, 31,
            32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47,
            48, 49, 50, 51, 52, 53, 54, 55,
            56, 57, 58, 59, 60, 61, 62, 63,
        },

        {
            56, 57, 58, 59, 60, 61, 62, 63,
            48, 49, 50, 51, 52, 53, 54, 55,
            40, 41, 42, 43, 44, 45, 46, 47,
            32, 33, 34, 35, 36, 37, 38, 39,
            24, 25, 26, 27, 28, 29, 30, 31,
            16, 17, 18, 19, 20, 21, 22, 23,
            8,  9,  10, 11, 12, 13, 14, 15,
            0,  1,  2,  3,  4,  5,  6,  7,
        }
    };

    public static int evaluate(Position pos) 
    {
        int scores[] = new int[2];
        long allPieces = pos.sides[Position.WHITE] | pos.sides[Position.BLACK];

        while (allPieces != 0)
        {
            int sq = Bitboard.findMSBPos(allPieces);
            allPieces = Bitboard.clearBit(allPieces, sq);

            int pieceType = pos.getPieceType(sq);
            int pieceColor = pos.getPieceColor(sq);

            switch (pieceType) 
            {
                case Position.PAWN: 
                    scorePawn(scores, sq, pieceColor);
                    break;
                case Position.KNIGHT: 
                    scoreKnight(scores, sq, pieceColor);
                    break;
                case Position.BISHOP: 
                    scoreBishop(scores, sq, pieceColor);
                    break;
                case Position.ROOK: 
                    scoreRook(scores, sq, pieceColor);
                    break;
                case Position.QUEEN: 
                    scoreQueen(scores, sq, pieceColor);
                    break;
                case Position.KING: 
                    scoreKing(scores, sq, pieceColor);
                    break;
            }
        }

        return scores[pos.stm] - scores[pos.stm ^ 1];
    }

    private static void scorePawn(int[] scores, int sq, int pieceColor)
    {
        scores[pieceColor] += PAWN_VALUE;
        scores[pieceColor] += PSQT[Position.PAWN][flipSq[pieceColor][sq]];
    }

    private static void scoreKnight(int[] scores, int sq, int pieceColor)
    {
        scores[pieceColor] += KNIGHT_VALUE;
        scores[pieceColor] += PSQT[Position.KNIGHT][flipSq[pieceColor][sq]];
    }

    private static void scoreBishop(int[] scores, int sq, int pieceColor)
    {
        scores[pieceColor] += BISHOP_VALUE;
        scores[pieceColor] += PSQT[Position.BISHOP][flipSq[pieceColor][sq]];
    }

    private static void scoreRook(int[] scores, int sq, int pieceColor)
    {
        scores[pieceColor] += ROOK_VALUE;
        scores[pieceColor] += PSQT[Position.BISHOP][flipSq[pieceColor][sq]];
    }

    private static void scoreQueen(int[] scores, int sq, int pieceColor)
    {
        scores[pieceColor] += QUEEN_VALUE;
        scores[pieceColor] += PSQT[Position.QUEEN][flipSq[pieceColor][sq]];
    }
    
    private static void scoreKing(int[] scores, int sq, int pieceColor)
    {
        scores[pieceColor] += PSQT[Position.KING][flipSq[pieceColor][sq]];
    }
}
