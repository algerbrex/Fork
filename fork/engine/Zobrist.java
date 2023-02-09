package fork.engine;

import java.util.concurrent.ThreadLocalRandom;
import java.lang.Long;

public class Zobrist {
    public static final int ZOBRIST_SEED_VALUE = 1;
    public static final int NO_EP_FILE         = 8;

    private static final long[] PIECE_SQ_RAND_64;
    private static final long[] EP_FILE_RAND_64;
    private static final long[] CASTLING_RIGHTS_RAND_64;
    private static final long   SIDE_TO_MOVE_RAND_64;

    private static final long[] POSSIBLE_EP_FILES = {
        8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8,
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8,
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8,
        8
    };

    static
    {
        PIECE_SQ_RAND_64        = new long[768];
        EP_FILE_RAND_64         = new long[9];
        CASTLING_RIGHTS_RAND_64 = new long[16];

        for (int i = 0; i < 768; i++)
            PIECE_SQ_RAND_64[i] = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

        for (int i = 0; i < 8; i++)
            EP_FILE_RAND_64[i] = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

        for (int i = 0; i < 16; i++)
            CASTLING_RIGHTS_RAND_64[i] = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

        SIDE_TO_MOVE_RAND_64    = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    }

    public static long pieceNumber(int pieceType, int pieceColor, int sq)
    {
        return PIECE_SQ_RAND_64[(pieceType * 2 + pieceColor) * 64 + sq];
    }

    public static long epNumber(int epSq)
    {
        return EP_FILE_RAND_64[(int)POSSIBLE_EP_FILES[epSq]];
    }

    public static long castlingNumber(long castlingRights)
    {
        return CASTLING_RIGHTS_RAND_64[(int)castlingRights];
    }

    public static long sideToMoveNumber()
    {
        return SIDE_TO_MOVE_RAND_64;
    }

    public static long genHash(Position pos)
    {
        long hash = 0L;

        for (int i = 0; i < 64; i++)
        {
            int pieceType = pos.getPieceType(i);
            int pieceColor = pos.getPieceColor(i);

            if (pieceType != Position.NO_TYPE)
                hash ^= Zobrist.pieceNumber(pieceType, pieceColor, (byte)i);
        }

        hash ^= Zobrist.epNumber(pos.epSq);
        hash ^= Zobrist.castlingNumber(pos.rights);

        if (pos.stm == Position.WHITE)
            hash ^= Zobrist.sideToMoveNumber();

        return hash;
    }
}
