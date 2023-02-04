package fork.engine;

public class Timer 
{
    public static final int NO_VALUE      = 0;
    public static final int INFINITE_TIME = -1;

    public long timeLeft;
    public long increment;
    public long moveTime;
    public long maxNodeCount;
    public int maxDepth;
    public int movesToGo;
    public long timeForMove;

    private boolean stopped;
    private long stopTime;

    public void setup(
        long timeLift, long increment, long moveTime,
        long maxNodeCount, int maxDepth, int movesToGo
    )
    {
        this.timeLeft     = timeLift;
        this.increment    = increment;
        this.moveTime     = moveTime;
        this.maxNodeCount = maxNodeCount;
        this.maxDepth     = maxDepth;
        this.movesToGo    = movesToGo;
    }

    public void start()
    {
        stopped = false;

        if (moveTime != NO_VALUE) 
        {
            stopTime = System.currentTimeMillis() + moveTime;
            timeLeft = NO_VALUE;
            return;
        }

        if (timeLeft == INFINITE_TIME)
            return;


        timeForMove = movesToGo != NO_VALUE ? timeLeft / movesToGo : timeLeft / 40;
        timeForMove += (3 * increment) / 4;

        if (timeForMove >= timeLeft)
        {
            timeForMove = timeLeft - 150;
            if (timeForMove <= 0)
                timeForMove = 100;
        }

        stopTime = System.currentTimeMillis() + timeForMove;
    }

    public void checkIfTimeIsUp()
    {
        if (stopped || timeLeft == INFINITE_TIME)
            return;

        if (System.currentTimeMillis() >= stopTime)
            stopped = true;
    }

    public void forceStop() 
    { 
        stopped = true;
    }

    public boolean isStopped() 
    { 
        return stopped; 
    }
}