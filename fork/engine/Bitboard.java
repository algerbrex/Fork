package fork.engine;

import java.lang.Long;

public class Bitboard 
{
    final static long MSB      = 0x8000000000000000L;
    final static long EMPTY_BB = 0x0L;
    final static long FULL_BB  = ~0x0L;
    final static long[] SQUARE_BB = new long[65];

    static {
        for (int i = 0; i < 64; i++)
            SQUARE_BB[i] = MSB >>> i;
    }

    public static long setBit(long bb, int sq) 
    {
        return bb | (MSB >>> sq);
    }

    public static long clearBit(long bb, int sq) 
    {
        return bb & ~(MSB >>> sq);
    }

    public static boolean bitSet(long bb, int sq) 
    {
        return (bb & (MSB >>> sq)) != 0;
    }

    public static int findMSBPos(long bb) 
    {
        return Long.numberOfLeadingZeros(bb);
    }

    public static void printBitboard(long bb) 
    {
        String bbStr = String.format("%64s", Long.toBinaryString(bb)).replace(' ', '0');
        for (int i = 56; i >= 0;  i -= 8) 
        {
            System.out.print((i/8 + 1) + "| ");
            for (int j = i; j < i + 8; j++) 
            {
                char bit = bbStr.charAt(j);
                bit = bit == '0' ? '.' : bit;
                System.out.print(bit + " ");
            }
            System.out.println();
        }

        System.out.println("  ----------------\n   a b c d e f g h");
    }
}
