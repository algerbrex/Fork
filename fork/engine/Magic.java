package fork.engine;

public class Magic {
    public static final MagicSqInfo[] ROOK_MAGICS;
    public static final MagicSqInfo[] BISHOP_MAGICS;
    public static final long[][] ROOK_MOVES;
    public static final long[][] BISHOP_MOVES;

    public static final long[] MAGIC_SEEDS = {728, 10316, 55013, 32803, 12281, 15100, 16645, 255};

    private static long genRookMovesHQ(byte sq, long occupied, boolean genMask) 
    {
        long sliderBB = Bitboard.MSB >>> sq;    
        long fileMask = Tables.MASK_FILE[Square.fileOf(sq)];
        long rankMask = Tables.MASK_RANK[Square.rankOf(sq)];
    
        long rhs = Long.reverse(Long.reverse((occupied & rankMask)) - (2 * Long.reverse(sliderBB)));
        long lhs = (occupied & rankMask) - 2 * sliderBB;
        long eastWestMoves = (rhs ^ lhs) & rankMask;
    
        rhs = Long.reverse(Long.reverse((occupied & fileMask)) - (2 * Long.reverse(sliderBB)));
        lhs = (occupied & fileMask) - 2 * sliderBB;
        long northSouthMoves = (rhs ^ lhs) & fileMask;
    
        if (genMask)
        {
            northSouthMoves &= Tables.CLEAR_RANK[Tables.RANK_1] & Tables.CLEAR_RANK[Tables.RANK_8];
            eastWestMoves   &= Tables.CLEAR_FILE[Tables.FILE_A] & Tables.CLEAR_FILE[Tables.FILE_H];
        }
    
        return northSouthMoves | eastWestMoves;
    }

    private static long genBishopMovesHQ(byte sq, long occupied, boolean genMask) 
    {
        long sliderBB = Bitboard.MSB >>> sq;    
        long diagonalMask = Tables.MASK_DIAGONAL[Square.fileOf(sq)-Square.rankOf(sq)+7];
        long antidiagonalMask = Tables.MASK_ANTIDIAGONAL[14-(Square.rankOf(sq)+Square.fileOf(sq))];
    
        long rhs = Long.reverse(Long.reverse((occupied & diagonalMask)) - (2 * Long.reverse(sliderBB)));
        long lhs = (occupied & diagonalMask) - 2 * sliderBB;
        long diagonalMoves = (rhs ^ lhs) & diagonalMask;
    
        rhs = Long.reverse(Long.reverse((occupied & antidiagonalMask)) - (2 * Long.reverse(sliderBB)));
        lhs = (occupied & antidiagonalMask) - 2 * sliderBB;
        long antidiagonalMoves = (rhs ^ lhs) & antidiagonalMask;
    
        long edges = Bitboard.FULL_BB;
        if (genMask)
        {
            edges = Tables.CLEAR_FILE[Tables.FILE_A] & 
                    Tables.CLEAR_FILE[Tables.FILE_H] & 
                    Tables.CLEAR_RANK[Tables.RANK_1] & 
                    Tables.CLEAR_RANK[Tables.RANK_8];
        }
        return (diagonalMoves | antidiagonalMoves) & edges;
    }

    private static long[] genSubsets(long bb, int numSubsets) 
    {
        long[] subsets = new long[numSubsets];
        long currSubset = 0L;
        short index = 0;

        do {
            subsets[index] = currSubset;
            currSubset = (currSubset - bb) & bb;
            index++;
        } while (currSubset != 0);

        return subsets;
    }

    static 
    {
        ROOK_MAGICS   = new MagicSqInfo[64];
        BISHOP_MAGICS = new MagicSqInfo[64];
        ROOK_MOVES    = new long[64][4096];
        BISHOP_MOVES  = new long[64][512];

        PRNG prng = new PRNG();

        // Generate rook magic numbers and moves.

        for (int sq = 0; sq < 64; sq++) 
        {
            MagicSqInfo magic = new MagicSqInfo();
            long blockerMask  = genRookMovesHQ((byte)sq, 0L, true);
            int noBitsSetHigh = Long.bitCount(blockerMask);
            byte shift = (byte)(64 - noBitsSetHigh);

            magic.blockerMask = blockerMask;
            magic.shift = shift;

            long[] subsets = genSubsets(blockerMask, 1 << noBitsSetHigh);
            long[] subsetMoves = new long[1 << noBitsSetHigh];

            for (int i = 0; i < (1 << noBitsSetHigh); i++)
            {
                subsetMoves[i] = genRookMovesHQ((byte)sq, subsets[i], false);
            }

            prng.seed(MAGIC_SEEDS[Square.rankOf((byte)sq)]);
            boolean searching = true;

            while (searching)
            {
                long magicNo = prng.sparseRandomLong();
                magic.magicNo = magicNo;
                searching = false;

                for (int i = 0; i < (1 << 12); i++) 
                {
                    ROOK_MOVES[sq][i] = 0L;
                }

                for (int i = 0; i < (1 << noBitsSetHigh); i++)
                {
                    long subset = subsets[i];
                    long moves = subsetMoves[i];
                    long index = (subset * magicNo) >>> shift;

                    if (ROOK_MOVES[sq][(int)index] != 0 && ROOK_MOVES[sq][(int)index] != moves) 
                    {
                        searching = true;
                        break;
                    }

                    ROOK_MOVES[sq][(int)index] = moves;
                }                
            }

            ROOK_MAGICS[sq] = magic;
        } 

        // Generate bishop magic numbers and moves.

        for (int sq = 0; sq < 64; sq++) 
        {
            MagicSqInfo magic = new MagicSqInfo();
            long blockerMask  = genBishopMovesHQ((byte)sq, 0L, true);
            int noBitsSetHigh = Long.bitCount(blockerMask);
            byte shift = (byte)(64 - noBitsSetHigh);

            magic.blockerMask = blockerMask;
            magic.shift = shift;

            long[] subsets = genSubsets(blockerMask, 1 << noBitsSetHigh);
            long[] subsetMoves = new long[1 << noBitsSetHigh];

            for (int i = 0; i < (1 << noBitsSetHigh); i++)
            {
                subsetMoves[i] = genBishopMovesHQ((byte)sq, subsets[i], false);
            }

            prng.seed(MAGIC_SEEDS[Square.rankOf((byte)sq)]);
            boolean searching = true;

            while (searching)
            {
                long magicNo = prng.sparseRandomLong();
                magic.magicNo = magicNo;
                searching = false;

                for (int i = 0; i < (1 << 9); i++) 
                {
                    BISHOP_MOVES[sq][i] = 0L;
                }

                for (int i = 0; i < (1 << noBitsSetHigh); i++)
                {
                    long subset = subsets[i];
                    long moves = subsetMoves[i];
                    long index = (subset * magicNo) >>> shift;

                    if (BISHOP_MOVES[sq][(int)index] != 0 && BISHOP_MOVES[sq][(int)index] != moves) 
                    {
                        searching = true;
                        break;
                    }

                    BISHOP_MOVES[sq][(int)index] = moves;
                }                
            }

            BISHOP_MAGICS[sq] = magic;
        } 
    }
}
