package fork.engine;

public class MoveList 
{
    private static final int MAX_MOVES = 255;

    public int[] moves;
    public int count;

    public MoveList() 
    {
        moves = new int[MAX_MOVES];
        count = 0;
    }

    public void addMove(int move) 
    {
        moves[count] = move;
        count++;
    }
}
