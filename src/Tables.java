public class Tables {
    public static final long[] MASK_DIAGONAL = 
    {
        0x80L,
        0x8040L,
        0x804020L,
        0x80402010L,
        0x8040201008L,
        0x804020100804L,
        0x80402010080402L,
        0x8040201008040201L,
        0x4020100804020100L,
        0x2010080402010000L,
        0x1008040201000000L,
        0x804020100000000L,
        0x402010000000000L,
        0x201000000000000L,
        0x100000000000000L,
    };

    public static final long[] MASK_ANTIDIAGONAL = 
    {
        0x1L,
        0x102L,
        0x10204L,
        0x1020408L,
        0x102040810L,
        0x10204081020L,
        0x1020408102040L,
        0x102040810204080L,
        0x204081020408000L,
        0x408102040800000L,
        0x810204080000000L,
        0x1020408000000000L,
        0x2040800000000000L,
        0x4080000000000000L,
        0x8000000000000000L,
    };

    public static final long[] MASK_FILE;
    public static final long[] MASK_RANK;
    public static final long[] CLEAR_FILE;
    public static final long[] CLEAR_RANK;

    public static final long[]   KING_MOVES;
    public static final long[]   KNIGHT_MOVES;
    public static final long[][] PAWN_PUSHES;
    public static final long[][] PAWN_ATTACKS;

    public static final byte RANK_1 = 0, FILE_A = 0;
    public static final byte RANK_2 = 1, FILE_B = 1;
    public static final byte RANK_3 = 2, FILE_C = 2;
    public static final byte RANK_4 = 3, FILE_D = 3;
    public static final byte RANK_5 = 4, FILE_E = 4;
    public static final byte RANK_6 = 5, FILE_F = 5;
    public static final byte RANK_7 = 6, FILE_G = 6;
    public static final byte RANK_8 = 7, FILE_H = 7;

    public static final byte NORTH = 8, SOUTH = 8;
    public static final byte EAST  = 1, WEST  = 1;
    
    static 
    {
        MASK_FILE  = new long[8];
        MASK_RANK  = new long[8];
        CLEAR_FILE = new long[8];
        CLEAR_RANK = new long[8];

        KING_MOVES   = new long[64];
        KNIGHT_MOVES = new long[64];
        PAWN_PUSHES  = new long[3][64];
        PAWN_ATTACKS = new long[3][64];

        for (int i = 0; i < 8; i++) 
        {
            long emptyBB = Bitboard.EMPTY_BB;
            long fullBB = Bitboard.FULL_BB;

            for (int j = i; j <= 63; j += 8) 
            {
                emptyBB = Bitboard.setBit(emptyBB, (byte)j);
                fullBB = Bitboard.clearBit(fullBB, (byte)j);
            }

            MASK_FILE[i] = emptyBB;
            CLEAR_FILE[i] = fullBB;
        }

        for (int i = 0; i <= 56; i += 8) 
        {
            long emptyBB = Bitboard.EMPTY_BB;
            long fullBB = Bitboard.FULL_BB;

            for (int j = i; j < i + 8; j++) 
            {
                emptyBB = Bitboard.setBit(emptyBB, (byte)j);
                fullBB = Bitboard.clearBit(fullBB, (byte)j);
            }

            MASK_RANK[i / 8] = emptyBB;
            CLEAR_RANK[i / 8] = fullBB;
        }

        for (int sq = 0; sq < 64; sq++) 
        {
            long sqBB = Bitboard.MSB >>> sq;
            long sqBBClippedHFile  = sqBB & CLEAR_FILE[FILE_H];
            long sqBBClippedAFile  = sqBB & CLEAR_FILE[FILE_A];
            long sqBBClippedGHFile = sqBB & CLEAR_FILE[FILE_G] & CLEAR_FILE[FILE_H];
            long sqBBClippedABFile  = sqBB & CLEAR_FILE[FILE_A] & CLEAR_FILE[FILE_B];
            
            // Generate king moves lookup table.

            long top      = sqBB >>> NORTH;
            long topRight = sqBBClippedHFile >>> NORTH >>> EAST;
            long topLeft  = sqBBClippedAFile >>> NORTH << WEST;

            long right = sqBBClippedHFile >>> EAST;
            long left  = sqBBClippedAFile <<   WEST;

            long bottom      = sqBB << SOUTH;
            long bottomRight = sqBBClippedHFile << SOUTH >>> EAST;
            long bottomLeft  = sqBBClippedAFile << SOUTH << WEST;

            long kingMoves = top | topRight | topLeft | right | left | bottom | bottomRight | bottomLeft;
            KING_MOVES[sq] = kingMoves;

            // Generate knight moves lookup table.

            long northNorthEast = sqBBClippedHFile >>> NORTH >>> NORTH >>> EAST;
            long northEastEast  = sqBBClippedGHFile >>> NORTH >>> EAST >>> EAST;

            long southEastEast  = sqBBClippedGHFile << SOUTH >>> EAST >>> EAST;
            long southSouthEast = sqBBClippedHFile << SOUTH << SOUTH >>> EAST;

            long southSouthWest = sqBBClippedAFile << SOUTH << SOUTH << WEST;
            long southWestWest  = sqBBClippedABFile << SOUTH << WEST << WEST;

            long northNorthWest = sqBBClippedAFile >>> NORTH >>> NORTH << WEST;
            long northWestWest  = sqBBClippedABFile >>> NORTH << WEST << WEST;

            long knightMoves = northNorthEast | northEastEast | southEastEast | southSouthEast |
                southSouthWest | southWestWest | northNorthWest | northWestWest;
            KNIGHT_MOVES[sq] = knightMoves;

            // Generate pawn pushes lookup table.

            long whitePawnPush = sqBB >>> NORTH;
            long blackPawnPush = sqBB << SOUTH;

            PAWN_PUSHES[Position.WHITE][sq] = whitePawnPush;
            PAWN_PUSHES[Position.BLACK][sq] = blackPawnPush;

            // Generate pawn attacks lookup table.

            long whitePawnRightAttack = sqBBClippedHFile >>> NORTH >>> EAST;
            long whitePawnLeftAttack  = sqBBClippedAFile >>> NORTH << WEST;

            long blackPawnRightAttack = sqBBClippedHFile << SOUTH >>> EAST;
            long blackPawnLeftAttack  = sqBBClippedAFile << SOUTH << WEST;

            PAWN_ATTACKS[Position.WHITE][sq] = whitePawnRightAttack | whitePawnLeftAttack;
            PAWN_ATTACKS[Position.BLACK][sq] = blackPawnRightAttack | blackPawnLeftAttack;
        }
    }
}
