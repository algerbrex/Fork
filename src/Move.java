public class Move {
    public final static byte QUIET     = 0;
    public final static byte ATTACK    = 1;
    public final static byte CASTLE    = 2;
    public final static byte PROMOTION = 3;

    public final static byte KNIGHT_PROMO = 0;
    public final static byte BISHOP_PROMO = 1;
    public final static byte ROOK_PROMO   = 2;
    public final static byte QUEEN_PROMO  = 3;

    public final static byte ATTACK_EP = 1;
    public final static byte NO_FLAG   = 0;

    public static int makeMove(byte from, byte to, byte moveType, byte flag) 
    {
        return (int)(from) << 26 | (int)(to) << 20 | (int)(moveType) << 18 | (int)(flag) << 16;
    }

    public static byte getFromSq(int move) 
    {
        return (byte)((move & 0xfc000000) >>> 26);
    } 

    public static byte getToSq(int move) 
    {
        return (byte)((move & 0x3f00000) >> 20);
    } 

    public static byte getMoveType(int move) 
    {
        return (byte)((move & 0xc0000) >> 18);
    } 

    public static byte getFlag(int move) 
    {
        return (byte)((move & 0x30000) >> 16);
    } 

    public static short getScore(int move) 
    {
        return (short)(move & 0xffff);
    } 

    public static int addScore(int move, short score) 
    {
        return (move & 0xffff0000) | (int)score;
    }

    public static boolean equals(int move_1, int move_2) 
    {
        return (move_1 & 0xffff0000) == (move_2 & 0xffff0000);
    }

    public static String toString(int move) 
    {
        byte from     = getFromSq(move);
        byte to       = getToSq(move);
        byte moveType = getMoveType(move);
        byte flag     = getFlag(move);

        String promoType = "";
        if (moveType == PROMOTION) 
            promoType = switch(flag) 
            {
                case KNIGHT_PROMO -> "n";
                case BISHOP_PROMO -> "b";
                case ROOK_PROMO   -> "r";
                case QUEEN_PROMO  -> "q";
                default -> "";
            };
        
        return Square.sqToCoord(from) + Square.sqToCoord(to) + promoType;
    }
}
