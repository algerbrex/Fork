package fork.engine;

public class MoveGen
{
    private static final long F1_G1 = 0x600000000000000L, B1_C1_D1 = 0x7000000000000000L,
	F8_G8 = 0x6L, B8_C8_D8 = 0x70L;

    public static MoveList genAllMoves(Position pos) 
    {
        MoveList moves = new MoveList();
        long usBB = pos.sides[pos.stm];
        long enemyBB = pos.sides[Position.flipColor(pos.stm)];

        genKnightMoves(pos.pieces[Position.KNIGHT] & usBB, enemyBB, usBB, moves);
        genKingMoves(pos.pieces[Position.KING] & usBB, enemyBB, usBB, moves);

        genRookMoves(pos.pieces[Position.ROOK] & usBB, enemyBB, usBB, moves);
        genBishopMoves(pos.pieces[Position.BISHOP] & usBB, enemyBB, usBB, moves);
        genQueenMoves(pos.pieces[Position.QUEEN] & usBB, enemyBB, usBB, moves);
        
        genPawnMoves(
            pos.pieces[Position.PAWN] & usBB,
            enemyBB, usBB, pos.stm, pos.epSq, moves
        );

        genCastlingMoves(pos, moves);

        return moves;
    }

    public static boolean sqIsAttacked(Position pos, byte usColor, byte sq) 
    {
        long enemyBB = pos.sides[Position.flipColor(usColor)];
        long usBB = pos.sides[usColor];

        long enemyKnights = pos.pieces[Position.KNIGHT] & enemyBB;
        long enemyKing    = pos.pieces[Position.KING] & enemyBB;
        long enemyPawns   = pos.pieces[Position.PAWN] & enemyBB;

        if ((Tables.KNIGHT_MOVES[sq] & enemyKnights) != 0)
            return true;
        if ((Tables.KING_MOVES[sq] & enemyKing) != 0)
            return true;
        if ((Tables.PAWN_ATTACKS[usColor][sq] & enemyPawns) != 0)
            return true;

        long enemyBishops = pos.pieces[Position.BISHOP] & enemyBB;
        long enemyRooks   = pos.pieces[Position.ROOK] & enemyBB;
        long enemyQueens  = pos.pieces[Position.QUEEN] & enemyBB;

        long intercardinalRays = genBishopMovesBB(sq, enemyBB|usBB);
        long cardinalRays  = genRookMovesBB(sq, enemyBB|usBB);

        if ((intercardinalRays & (enemyBishops|enemyQueens)) != 0)
            return true;
        if ((cardinalRays & (enemyRooks|enemyQueens)) != 0)
            return true;
        return false;
    }

    public static long perft(Position pos, byte depth) 
    {
        if (depth == 0)
            return 1L;
        
        MoveList moves = genAllMoves(pos);
        long nodes = 0L;

        byte kingSq = Bitboard.findMSBPos(pos.pieces[Position.KING] & pos.sides[pos.stm]);
        boolean inCheck = MoveGen.sqIsAttacked(pos, pos.stm, kingSq);
        long pinned = pos.getPinnedPieces(pos.stm);

        for (int i = 0; i < moves.count; i++) 
        {
            int move = moves.moves[i];
            Position newPos = pos.copy();

            if (newPos.makeMove(move, inCheck, kingSq, pinned)) 
            {
                nodes += perft(newPos, (byte)(depth-1));
            }
        }
        return nodes;
    }

    private static void genKnightMoves(long knightBB, long enemyBB, long usBB, MoveList moves) 
    {
        while (knightBB != 0) 
        {
            byte from = Bitboard.findMSBPos(knightBB);
            knightBB = Bitboard.clearBit(knightBB, from);
            genMovesFromBB(Tables.KNIGHT_MOVES[from] & ~usBB, enemyBB, from, moves);
        }
    }

    private static void genKingMoves(long kingBB, long enemyBB, long usBB, MoveList moves) 
    {
        while (kingBB != 0) 
        {
            byte from = Bitboard.findMSBPos(kingBB);
            kingBB = Bitboard.clearBit(kingBB, from);
            genMovesFromBB(Tables.KING_MOVES[from] & ~usBB, enemyBB, from, moves);
        }
    }

    private static void genRookMoves(long rookBB, long enemyBB, long usBB, MoveList moves) 
    {
        while (rookBB != 0) 
        {
            byte from = Bitboard.findMSBPos(rookBB);
            rookBB = Bitboard.clearBit(rookBB, from);
            genMovesFromBB(genRookMovesBB(from, enemyBB|usBB) & ~usBB, enemyBB, from, moves);
        }
    }

    private static void genBishopMoves(long bishopBB, long enemyBB, long usBB, MoveList moves) 
    {
        while (bishopBB != 0) 
        {
            byte from = Bitboard.findMSBPos(bishopBB);
            bishopBB = Bitboard.clearBit(bishopBB, from);
            genMovesFromBB(genBishopMovesBB(from, enemyBB|usBB) & ~usBB, enemyBB, from, moves);
        }
    }

    private static void genQueenMoves(long queenBB, long enemyBB, long usBB, MoveList moves) 
    {
        while (queenBB != 0) 
        {
            byte from = Bitboard.findMSBPos(queenBB);
            queenBB = Bitboard.clearBit(queenBB, from);
            genMovesFromBB(
                (genRookMovesBB(from, enemyBB|usBB) | genBishopMovesBB(from, enemyBB|usBB)) & ~usBB, 
                enemyBB, from, moves
            );
        }
    }

    private static void genPawnMoves(long pawnBB, long enemyBB, long usBB, byte stm, byte epSq, MoveList moves) 
    {
        while (pawnBB != 0) 
        {
            byte from = Bitboard.findMSBPos(pawnBB);
            pawnBB = Bitboard.clearBit(pawnBB, from);

            long pawnOnePush = Tables.PAWN_PUSHES[stm][from] & ~(usBB | enemyBB);
            long pawnTwoPush = 0L;

            if (stm == Position.WHITE)
                pawnTwoPush = ((pawnOnePush & Tables.MASK_RANK[Tables.RANK_3]) >>> 8) & ~(usBB | enemyBB);
            else 
                pawnTwoPush = ((pawnOnePush & Tables.MASK_RANK[Tables.RANK_6]) << 8) & ~(usBB | enemyBB);

            long pawnPush = pawnOnePush | pawnTwoPush;
            long pawnAttacks = Tables.PAWN_ATTACKS[stm][from] & (enemyBB | ((Bitboard.MSB >>> epSq) & Tables.CLEAR_RANK[Tables.RANK_1]));

            while (pawnPush != 0) 
            {
                byte to = Bitboard.findMSBPos(pawnPush);
                pawnPush = Bitboard.clearBit(pawnPush, to);

                if (isPromoting(stm, to))
                    genPromotionMoves(from, to, moves);
                else
                    moves.addMove(Move.makeMove(from, to, Move.QUIET, Move.NO_FLAG));
            }

            while (pawnAttacks != 0) 
            {
                byte to = Bitboard.findMSBPos(pawnAttacks);
                pawnAttacks = Bitboard.clearBit(pawnAttacks, to);

                if (to == epSq)
                    moves.addMove(Move.makeMove(from, to, Move.ATTACK, Move.ATTACK_EP));
                else
                {
                    if (isPromoting(stm, to))
                        genPromotionMoves(from, to, moves);
                    else
                        moves.addMove(Move.makeMove(from, to, Move.ATTACK, Move.NO_FLAG));
                }
            }
        }   
    }

    private static void genCastlingMoves(Position pos, MoveList moves) 
    {
        long allPieces = pos.sides[pos.stm] | pos.sides[Position.flipColor(pos.stm)];
        if (pos.stm == Position.WHITE) 
        {
            if ((pos.rights & Position.WHITE_KS_RIGHT) != 0 && (allPieces & F1_G1) == 0)
                moves.addMove(Move.makeMove(Square.E1, Square.G1, Move.CASTLE, Move.NO_FLAG));
            if ((pos.rights & Position.WHITE_QS_RIGHT) != 0 && (allPieces & B1_C1_D1) == 0)
                moves.addMove(Move.makeMove(Square.E1, Square.C1, Move.CASTLE, Move.NO_FLAG));
        } 
        else 
        {
            if ((pos.rights & Position.BLACK_KS_RIGHT) != 0 && (allPieces & F8_G8) == 0)
                moves.addMove(Move.makeMove(Square.E8, Square.G8, Move.CASTLE, Move.NO_FLAG));
            if ((pos.rights & Position.BLACK_QS_RIGHT) != 0 && (allPieces & B8_C8_D8) == 0)
                moves.addMove(Move.makeMove(Square.E8, Square.C8, Move.CASTLE, Move.NO_FLAG));
        }
    }

    private static boolean isPromoting(byte stm, byte to) {
        return stm == Position.WHITE ? to >= 56 && to <= 63 : to <= 7;
    }
    
    private static void genPromotionMoves(byte from, byte to, MoveList moves) {
        moves.addMove(Move.makeMove(from, to, Move.PROMOTION, Move.KNIGHT_PROMO));
        moves.addMove(Move.makeMove(from, to, Move.PROMOTION, Move.BISHOP_PROMO));
        moves.addMove(Move.makeMove(from, to, Move.PROMOTION, Move.ROOK_PROMO));
        moves.addMove(Move.makeMove(from, to, Move.PROMOTION, Move.QUEEN_PROMO));
    } 

    private static void genMovesFromBB(long movesBB, long enemyBB, byte from, MoveList moves) 
    {
        while (movesBB != 0) 
        {
            byte to = Bitboard.findMSBPos(movesBB);
            movesBB = Bitboard.clearBit(movesBB, to);

            byte moveType = ((Bitboard.MSB >>> to) & enemyBB) == 0 ? Move.QUIET : Move.ATTACK;
            moves.addMove(Move.makeMove(from, to, moveType, Move.NO_FLAG));
        }
    }

    public static long genRookMovesBB(byte from, long blockers) 
    {
        MagicSqInfo magic = Magic.ROOK_MAGICS[from];
        blockers &= magic.blockerMask;
        long index = (blockers * magic.magicNo) >>> magic.shift;
        return Magic.ROOK_MOVES[from][(int)index];
    }

    public static long genBishopMovesBB(byte from, long blockers) 
    {
        MagicSqInfo magic = Magic.BISHOP_MAGICS[from];
        blockers &= magic.blockerMask;
        long index = (blockers * magic.magicNo) >>> magic.shift;
        return Magic.BISHOP_MOVES[from][(int)index];
    }
}
