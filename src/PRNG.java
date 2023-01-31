public class PRNG 
{
    private long state;

    public PRNG() 
    {
        state = 1;
    }

    public PRNG(long seed) 
    {
        state = seed;
    }

    public void seed(long seed)
    {
        state = seed;
    }

    public long randomLong() 
    {
        state ^= state >>> 12;
        state ^= state << 25;
        state ^= state >>> 27;
        return state * 2685821657736338717L;
    }

    public long sparseRandomLong() 
    {
        return randomLong() & randomLong() & randomLong();
    }
}
