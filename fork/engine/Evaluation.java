package fork.engine;

public class Evaluation 
{
    public static final int PAWN_VALUE   = 100;
    public static final int KNIGHT_VALUE = 300;
    public static final int BISHOP_VALUE = 320;
    public static final int ROOK_VALUE   = 500;
    public static final int QUEEN_VALUE  = 950;

    public static int evaluate(Position pos) 
    {
        int scores[] = new int[3];
        byte usColor = pos.stm;
        byte enemyColor = Position.flipColor(usColor);
        
        scores[usColor] += Long.bitCount((pos.pieces[Position.PAWN] & pos.sides[usColor])) * PAWN_VALUE;
        scores[usColor] += Long.bitCount((pos.pieces[Position.KNIGHT] & pos.sides[usColor])) * KNIGHT_VALUE;
        scores[usColor] += Long.bitCount((pos.pieces[Position.BISHOP] & pos.sides[usColor])) * BISHOP_VALUE;
        scores[usColor] += Long.bitCount((pos.pieces[Position.ROOK] & pos.sides[usColor])) * ROOK_VALUE;
        scores[usColor] += Long.bitCount((pos.pieces[Position.QUEEN] & pos.sides[usColor])) * QUEEN_VALUE;

        scores[enemyColor] += Long.bitCount((pos.pieces[Position.PAWN] & pos.sides[enemyColor])) * PAWN_VALUE;
        scores[enemyColor] += Long.bitCount((pos.pieces[Position.KNIGHT] & pos.sides[enemyColor])) * KNIGHT_VALUE;
        scores[enemyColor] += Long.bitCount((pos.pieces[Position.BISHOP] & pos.sides[enemyColor])) * BISHOP_VALUE;
        scores[enemyColor] += Long.bitCount((pos.pieces[Position.ROOK] & pos.sides[enemyColor])) * ROOK_VALUE;
        scores[enemyColor] += Long.bitCount((pos.pieces[Position.QUEEN] & pos.sides[enemyColor])) * QUEEN_VALUE;

        return scores[usColor] - scores[enemyColor];
    }
}
