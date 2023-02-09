package fork.engine;

public class Move {
    public final static int QUIET     = 0;
    public final static int ATTACK    = 1;
    public final static int CASTLE    = 2;
    public final static int PROMOTION = 3;

    public final static int KNIGHT_PROMO = 0;
    public final static int BISHOP_PROMO = 1;
    public final static int ROOK_PROMO   = 2;
    public final static int QUEEN_PROMO  = 3;

    public final static int ATTACK_EP = 1;
    public final static int NO_FLAG   = 0;

    public final static int NULL_MOVE = 0;

    public static int makeMove(int from, int to, int moveType, int flag) 
    {
        return from << 26 | to << 20 | moveType << 18 | flag << 16;
    }

    public static int getFromSq(int move) 
    {
        return (move & 0xfc000000) >>> 26;
    } 

    public static int getToSq(int move) 
    {
        return (move & 0x3f00000) >> 20;
    } 

    public static int getMoveType(int move) 
    {
        return (move & 0xc0000) >> 18;
    } 

    public static int getFlag(int move) 
    {
        return(move & 0x30000) >> 16;
    } 

    public static int getScore(int move) 
    {
        return move & 0xffff;
    } 

    public static int addScore(int move, int score) 
    {
        return (move & 0xffff0000) | score;
    }

    public static boolean equals(int move_1, int move_2) 
    {
        return (move_1 & 0xffff0000) == (move_2 & 0xffff0000);
    }

    public static int moveFromCoord(Position pos, String coord)
    {
        int from = Square.coordToSq(coord.substring(0, 2));
        int to   = Square.coordToSq(coord.substring(2, 4));
        int movedType = pos.getPieceType(from);

        int moveType = 0;
        int flag = Move.NO_FLAG;

        if (coord.length() == 5)
        {
            moveType = Move.PROMOTION;
            if (coord.charAt(coord.length() - 1) == 'n')
                flag = Move.KNIGHT_PROMO;
            else if (coord.charAt(coord.length() - 1) == 'b')
                flag = Move.BISHOP_PROMO;
            else if (coord.charAt(coord.length() - 1) == 'r')
                flag = Move.ROOK_PROMO;
            else if (coord.charAt(coord.length() - 1) == 'q')
                flag = Move.QUEEN_PROMO;
        } 
        else if (coord.equals("e1g1") && movedType == Position.KING)
            moveType = Move.CASTLE;
        else if (coord.equals("e1c1") && movedType == Position.KING)
            moveType = Move.CASTLE;
        else if (coord.equals("e8g8") && movedType == Position.KING)
            moveType = Move.CASTLE; 
        else if (coord.equals("e8c8") && movedType == Position.KING)
            moveType = Move.CASTLE;
        else if (to == pos.epSq && movedType == Position.PAWN)
        {
            moveType = Move.ATTACK;
            flag = Move.ATTACK_EP;
        }
        else
            moveType = pos.getPieceType(to) == Position.NO_TYPE ? Move.QUIET : Move.ATTACK;

        return makeMove(from, to, moveType, flag);
    }

    public static String toString(int move) 
    {
        int from     = getFromSq(move);
        int to       = getToSq(move);
        int moveType = getMoveType(move);
        int flag     = getFlag(move);

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
