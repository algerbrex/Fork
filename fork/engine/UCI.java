package fork.engine;

import java.util.Scanner;
import java.util.Arrays;

public class UCI 
{
    private static final String ENGINE_NAME         = "Fork 0.1";
    private static final String ENGINE_AUTHOR       = "Christian Dean";
    private static final String ENGINE_AUTHOR_EMAIL = "deanmchris@gmail.com";


    private Search search;

    public UCI() 
    {
        search = new Search();
    }

    private void reset() 
    {
        search = new Search();
    }

    private String trimPrefix(String str, String prefix) 
    {
        if (str.startsWith(prefix))
            return str.substring(prefix.length());
        return str;
    }

    private void UCICommand() 
    {
        System.out.printf("\nid name %s\n", ENGINE_NAME);
        System.out.printf("id author %s\n", ENGINE_AUTHOR);

        System.out.print("\nAvailable UCI commands:\n");
        System.out.print("    * uci\n    * isready\n    * ucinewgame");

        System.out.print("\n    * position");
        System.out.print("\n\t* fen <FEN>");
        System.out.print("\n\t* startpos");
    
        System.out.print("\n    * go");
        System.out.print("\n\t* wtime <MILLISECONDS>\n\t* btime <MILLISECONDS>");
        System.out.print("\n\t* winc <MILLISECONDS>\n\t* binc <MILLISECONDS>");
        System.out.print("\n\t* movestogo <INTEGER>\n\t* depth <INTEGER>\n\t* nodes <INTEGER>\n\t* movetime <MILLISECONDS>");
        System.out.print("\n\t* infinite");
    
        System.out.print("\n    * stop\n    * quit\n\n");
        System.out.printf("uciok\n\n");
    }

    private void positionCommand(String command)
    {
        command = trimPrefix(command, "position ");

        String fenString = "";

        if (command.startsWith("startpos")) 
        {
            command = trimPrefix(command, "startpos ");
            fenString = Position.START_FEN;
        } else if (command.startsWith("fen")) 
        {
            command = trimPrefix(command, "fen ");
            String[] args = command.split("\\s");
            fenString = String.join(" ", Arrays.copyOfRange(args, 0, 6));
            command = String.join(" ", Arrays.copyOfRange(args, 6, args.length));
        }

        search.pos.loadFEN(fenString);

        if (command.startsWith("moves")) 
        {
            command = trimPrefix(command, "moves");
            if (!command.isEmpty()) {
                command = command.trim();
                for (String moveAsString : command.split("\\s")) {
                    int move = Move.moveFromCoord(search.pos, moveAsString);

                    // We pass in false and Square.NO_SQ here because
                    // we assume the moves given through this UCI command
                    // are legal, so we're not going to waste time testing
                    // them even if they put the king in check or are king
                    // moves.
                    search.pos.makeMove(move, false, Square.NO_SQ, 0L);
                }
            }
        }
    }

    public void goCommand(String command)
    {
        command = trimPrefix(command, "go ");
        String[] args = command.split("\\s");

        String colorPrefix = search.pos.stm == Position.WHITE ? "w" : "b";

        long timeLeft     = Timer.INFINITE_TIME; 
        long increment    = Timer.NO_VALUE;
        long moveTime     = Timer.NO_VALUE;
        long maxNodeCount = Long.MAX_VALUE;
        int movesToGo     = Timer.NO_VALUE;
        int maxDepth      = Search.MAX_PLY;

        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];

            if (arg.startsWith(colorPrefix))
            {
                if (arg.endsWith("time"))
                    timeLeft = Long.parseLong(args[i + 1]);
                else if (arg.endsWith("inc"))
                    increment = Long.parseLong(args[i + 1]);
            } 
            else if (arg.equals("movestogo"))
                movesToGo = Integer.parseInt(args[i + 1]);
            else if (arg.equals("depth"))
                maxDepth = Integer.parseInt(args[i + 1]);
            else if (arg.equals("nodes"))
                maxNodeCount = Integer.parseInt(args[i + 1]);
            else if (arg.equals("movetime"))
                moveTime = Integer.parseInt(args[i + 1]);
        }

        search.timer.setup(timeLeft, increment, moveTime, maxNodeCount, maxDepth, movesToGo);

        Thread thread = new Thread(search);
        thread.start();
    }
    
    public void loop() 
    {
        System.out.println("Author: " + ENGINE_AUTHOR);
        System.out.println("Engine: " + ENGINE_NAME);
        System.out.println("Email: " + ENGINE_AUTHOR_EMAIL);

        Scanner scn = new Scanner(System.in);

        UCICommand();
        reset();
        search.pos.loadFEN(Position.START_FEN);

        boolean running = true;

        while (running)
        {
            String command = scn.nextLine();

            if (command.equals("uci"))
                UCICommand();
            else if (command.equals("isready"))
                System.out.println("readyok\n");
            else if (command.startsWith("ucinewgame"))
                reset();
            else if (command.startsWith("position"))
                positionCommand(command);
            else if (command.startsWith("go"))
                goCommand(command);
            else if (command.startsWith("stop"))
                search.stopSearch();
            else if (command.equals("quit"))
            {
                search.stopSearch();
                break;
            }
        }

        scn.close();
    }
}
