package fork.engine;

public class Position {
    public final static byte NO_TYPE = 0;
    public final static byte PAWN    = 1;
    public final static byte KNIGHT  = 2;
    public final static byte BISHOP  = 3;
    public final static byte ROOK    = 4;
    public final static byte QUEEN   = 5;
    public final static byte KING    = 6;

    public final static byte NO_COLOR = 0;
    public final static byte WHITE    = 1;
    public final static byte BLACK    = 2;
    
    public final static byte WHITE_KS_RIGHT = 0x8;
    public final static byte WHITE_QS_RIGHT = 0x4;
    public final static byte BLACK_KS_RIGHT = 0x2;
    public final static byte BLACK_QS_RIGHT = 0x1;

    public final static byte[] SPOILERS = {
        0xb, 0xf, 0xf, 0xf, 0x3, 0xf, 0xf, 0x7,
        0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf,
        0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf,
        0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf,
        0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf,
        0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf,
        0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf, 0xf,
        0xe, 0xf, 0xf, 0xf, 0xc, 0xf, 0xf, 0xd
    };

    public final static String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public final static String KIWIPETE_FEN = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    public long[] pieces;
    public long[] sides;
    public byte rights, stm , epSq, rule50;

    public Position() 
    {
        pieces = new long[7];
        sides = new long[3];
        rights = stm = rule50 = epSq = 0;
    }

    public Position(String fen) 
    {
        loadFEN(fen);
    }

    public void loadFEN(String fen) {
        pieces = new long[7];
        sides = new long[3];
        rights = stm = rule50 = epSq = 0;

        String[] fields = fen.split("\\s");
        String piecePlacement    = fields[0];
        String activeColor       = fields[1];
        String castlingRights    = fields[2];
        String activeEPSq        = fields[3];
        String halfMoveClock     = fields[4];

        for (byte index = 0 , sq = 56; index < piecePlacement.length(); index++) 
        {
            char chr = piecePlacement.charAt(index);
            switch (chr) 
            {
                case 'p': putPiece(PAWN, BLACK, sq); sq++; break;
                case 'n': putPiece(KNIGHT, BLACK, sq); sq++; break;
                case 'b': putPiece(BISHOP, BLACK, sq); sq++; break;
                case 'r': putPiece(ROOK, BLACK, sq); sq++; break;
                case 'q': putPiece(QUEEN, BLACK, sq); sq++; break;
                case 'k': putPiece(KING, BLACK, sq); sq++; break;
                case 'P': putPiece(PAWN, WHITE, sq); sq++; break;
                case 'N': putPiece(KNIGHT, WHITE, sq); sq++; break;
                case 'B': putPiece(BISHOP, WHITE, sq); sq++; break;
                case 'R': putPiece(ROOK, WHITE, sq); sq++; break;
                case 'Q': putPiece(QUEEN, WHITE, sq); sq++; break;
                case 'K': putPiece(KING, WHITE, sq); sq++; break;

                case '/': sq -= 16; break;
                case '1', '2', '3', '4', '5', '6', '7', '8': sq += chr - '0'; break;
            }
        }

        stm = activeColor.equals("w") ? WHITE : BLACK;
        epSq = activeEPSq.equals("-") ? Square.NO_SQ : Square.coordToSq(activeEPSq);
        rule50 = Byte.parseByte(halfMoveClock);

        for (int i = 0; i < castlingRights.length(); i++) 
        {
            switch (castlingRights.charAt(i)) 
            {
                case 'K': rights |= WHITE_KS_RIGHT; break;
                case 'Q': rights |= WHITE_QS_RIGHT; break;
                case 'k': rights |= BLACK_KS_RIGHT; break;
                case 'q': rights |= BLACK_QS_RIGHT; break;
            }
        }
    }

    public Position copy() 
    {
        Position newPos = new Position();

        System.arraycopy(this.pieces, 0, newPos.pieces, 0, this.pieces.length);
        System.arraycopy(this.sides, 0, newPos.sides, 0, this.sides.length);

        newPos.rights = this.rights;
        newPos.stm    = this.stm;
        newPos.epSq   = this.epSq;
        newPos.rule50 = this.rule50;
        
        return newPos;
    }

    public boolean makeMove(int move, boolean inCheck, byte currStmKingSq) 
    {
        byte from     = Move.getFromSq(move);
        byte to       = Move.getToSq(move);
        byte moveType = Move.getMoveType(move);
        byte flag     = Move.getFlag(move);

        byte movedType  = getPieceType(from);
        byte movedColor = getPieceColor(from);
        
        rule50++;
        epSq = Square.NO_SQ;

        long pinned = getPinnedPieces(stm);

        switch (moveType)
        {
            case Move.QUIET:
                clearPiece(movedType, movedColor, from);
                putPiece(movedType, movedColor, to);

                if (movedType == PAWN) 
                {
                    rule50 = 0;
                    if (Math.abs(from-to) == 16) {
                        epSq = (byte)(stm == WHITE ? to - 8 : to + 8);
                    }
                }
                break;
            case Move.ATTACK:
                {
                    if (flag == Move.ATTACK_EP) 
                    {
                        byte captureSq = (byte)(stm == WHITE ? to - 8 : to + 8);
                        byte capturedType  = getPieceType(captureSq);
                        byte capturedColor = getPieceColor(captureSq);

                        clearPiece(capturedType, capturedColor, captureSq);
                        clearPiece(movedType, movedColor, from);
                        putPiece(movedType, movedColor, to);
                    } 
                    else 
                    {
                        byte capturedType  = getPieceType(to);
                        byte capturedColor = getPieceColor(to);
                        clearPiece(capturedType, capturedColor, to);
                        clearPiece(movedType, movedColor, from);
                        putPiece(movedType, movedColor, to);
                    }
                }

                rule50 = 0;
                break;
            case Move.PROMOTION:
                {
                    byte capturedType  = getPieceType(to);
                    byte capturedColor = getPieceColor(to);

                    clearPiece(movedType, movedColor, from);

                    if (capturedType != NO_TYPE) 
                        clearPiece(capturedType, capturedColor, to);

                    putPiece((byte)(flag + 2), movedColor, to);
                }

                rule50 = 0;
                break;
            case Move.CASTLE:
                clearPiece(movedType, movedColor, from);
                putPiece(movedType, movedColor, to);

                byte rookFrom = 0, rookTo = 0;
                byte firstSqCrossed = 0, secondSqCrossed = 0;
                switch(to) 
                {
                    case Square.G1: 
                        rookFrom = Square.H1; rookTo = Square.F1;
                        firstSqCrossed = Square.F1; secondSqCrossed = Square.G1;
                        break;
                    case Square.C1: 
                        rookFrom = Square.A1; rookTo = Square.D1;
                        firstSqCrossed = Square.D1; secondSqCrossed = Square.C1;
                        break;
                    case Square.G8: 
                        rookFrom = Square.H8; rookTo = Square.F8;
                        firstSqCrossed = Square.F8; secondSqCrossed = Square.G8;
                        break;
                    case Square.C8: 
                        rookFrom = Square.A8; rookTo = Square.D8;
                        firstSqCrossed = Square.D8; secondSqCrossed = Square.C8;
                        break;
                }

                if (MoveGen.sqIsAttacked(this, movedColor, from)             || 
                    MoveGen.sqIsAttacked(this, movedColor, firstSqCrossed)   || 
                    MoveGen.sqIsAttacked(this, movedColor, secondSqCrossed))
                {
                    return false;
                }

                clearPiece(ROOK, movedColor, rookFrom);
                putPiece(ROOK, movedColor, rookTo);
                break;
        }

        rights = (byte)(rights & SPOILERS[from] & SPOILERS[to]);
        stm = flipColor(stm);

        if (inCheck ||
            from == currStmKingSq ||
            Bitboard.bitSet(pinned, from) || 
            (moveType == Move.ATTACK && flag == Move.ATTACK_EP)) 
        {
            byte kingSq = Bitboard.findMSBPos(pieces[KING] & sides[flipColor(stm)]);
            return !MoveGen.sqIsAttacked(this, flipColor(stm), kingSq);
        }

        return true;
    }

    public byte getPieceType(int sq) 
    {
        long sqBB = Bitboard.MSB >>> sq;
        int shift = 63 - sq;

        long kingBB   = (sqBB & pieces[KING]) >>> shift;
        long queenBB  = (sqBB & pieces[QUEEN]) >>> shift;
        long rookBB   = (sqBB & pieces[ROOK]) >>> shift;
        long bishopBB = (sqBB & pieces[BISHOP]) >>> shift;
        long knightBB = (sqBB & pieces[KNIGHT]) >>> shift;
        long pawnBB   = (sqBB & pieces[PAWN]) >>> shift;

        return (byte)(
            kingBB*KING | queenBB*QUEEN | rookBB*ROOK | bishopBB*BISHOP | knightBB*KNIGHT | pawnBB*PAWN
        ); 
    }

    public byte getPieceColor(int sq) 
    {
        long sqBB = Bitboard.MSB >>> sq;
        int shift = 63 - sq;

        long whiteBB  = (sqBB & sides[WHITE]) >>> shift;
        long blackBB  = (sqBB & sides[BLACK]) >>> shift;
  
        return (byte)(whiteBB*WHITE | blackBB*BLACK); 
    }

    private long getPinnedPieces(byte usColor) 
    {
        byte kingSq = Bitboard.findMSBPos(pieces[KING] & sides[usColor]);

        long usBB = sides[usColor];
        long enemyBB = sides[flipColor(usColor)];

        long enemyQueensAndRooks = enemyBB & (pieces[QUEEN] | pieces[ROOK]);
        long enemyQueensAndBishops = enemyBB & (pieces[QUEEN] | pieces[BISHOP]);

        long cardinalRays = MoveGen.genRookMovesBB(kingSq, enemyBB);
        long intercardinalRays = MoveGen.genBishopMovesBB(kingSq, enemyBB);

        long potentialPinners = (enemyQueensAndRooks & cardinalRays) | 
                                (enemyQueensAndBishops & intercardinalRays);
        long pinned = 0L;

        while (potentialPinners != 0)
        {
            byte pinnerSq = Bitboard.findMSBPos(potentialPinners);
            potentialPinners = Bitboard.clearBit(potentialPinners, pinnerSq);

            pinned |= usBB & (Tables.RAYS_BETWEEN[kingSq][pinnerSq]);
        }

        return pinned;
    }

    private void putPiece(byte pieceType, byte pieceColor, byte sq) 
    {
        pieces[pieceType] = Bitboard.setBit(pieces[pieceType], sq);
        sides[pieceColor] = Bitboard.setBit(sides[pieceColor], sq);
    }

    private void clearPiece(byte pieceType, byte pieceColor, byte sq) 
    {
        pieces[pieceType] = Bitboard.clearBit(pieces[pieceType], sq);
        sides[pieceColor] = Bitboard.clearBit(sides[pieceColor], sq);
    }

    public String toString() 
    {
        String boardStr = "";

        for (int i = 56; i >= 0;  i -= 8) 
        {
            boardStr += (i/8 + 1) + "|";
            for (int j = i; j < i + 8; j++) {
                byte pieceType = getPieceType(j);
                byte pieceColor = getPieceColor(j);

                char pieceChar = switch(pieceType) 
                {
                    case PAWN   -> 'i';
                    case KNIGHT -> 'n';
                    case BISHOP -> 'b';
                    case ROOK   -> 'r';
                    case QUEEN  -> 'q';
                    case KING   -> 'k';
                    default     -> '.'; 
                };

                pieceChar = pieceColor == WHITE ? Character.toUpperCase(pieceChar) : pieceChar;
            
                boardStr += " " + pieceChar;
            }
            boardStr += "\n";
        }

        boardStr += "  ----------------\n   a b c d e f g h \n\n";
        boardStr += "turn: " + (stm == WHITE ? "white\n" : "black\n");
    
        boardStr += "castling rights: ";
        if ((rights&WHITE_KS_RIGHT) != 0)
            boardStr += "K";
        if ((rights&WHITE_QS_RIGHT) != 0)
            boardStr += "Q";
        if ((rights&BLACK_KS_RIGHT) != 0)
            boardStr += "k";
        if ((rights&BLACK_QS_RIGHT) != 0)
            boardStr += "q";

        boardStr += "\nen passant: " + (epSq == Square.NO_SQ ? "none" : Square.sqToCoord(epSq));
        boardStr += "\nrule 50: " + rule50;

        return boardStr;
    }

    public static byte flipColor(byte color)
    {
        return (byte)(~color & 0x3);
    }
}
