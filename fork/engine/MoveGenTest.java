package fork.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.lang.Integer;
import java.lang.Long;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;

public class MoveGenTest 
{
    private static class TestData
    {
        public String fen;
        public byte depth;
        public long expectedNodeCount;

        public TestData(String fen, byte depth, long expectedNodeCount)
        {
            this.fen = fen;
            this.depth = depth;
            this.expectedNodeCount = expectedNodeCount;
        }
    }

    public static ArrayList<TestData> load_test_data() throws FileNotFoundException
    {
            
        Path path = Paths.get(System.getProperty("user.dir"), "perft_suite", "perft_suite.epd");
        File file = new File(path.toString());
        Scanner scn = new Scanner(file);
        ArrayList<TestData> testDatas = new ArrayList<TestData>();

        while (scn.hasNextLine())
        {
            String line = scn.nextLine();
            String[] fields = line.split(";");

            String fen = fields[0].trim();

            for (int i = 1; i < fields.length; i ++)
            {
                String field = fields[i].trim();
                byte depth = (byte)Integer.parseInt(field.substring(1, 2));
                long expectedNodeCount = Long.parseLong(field.split("\\s")[1]);
                testDatas.add(new TestData(fen, depth, expectedNodeCount));
            }

        }

        scn.close();
        return testDatas;
    }

    public static void run_test() throws FileNotFoundException
    {
        Position pos = new Position();
        ArrayList<TestData> testDatas = load_test_data();
        
        System.out.println("Begin perft testing.");
        System.out.println("Format of output: (<depth>) <fen> => <expected number>/<calculated number> [passed/failed]");
        System.out.println("------------------------------------------------------------------------------\n");

        int numberOfTests = 0, numberOfFailedTests = 0;
        long startTime = System.currentTimeMillis();

        for (TestData testData : testDatas)
        {
            numberOfTests++;
            pos.loadFEN(testData.fen);
            long nodes = MoveGen.perft(pos, testData.depth);

            if (nodes == testData.expectedNodeCount)
                System.out.printf("(%d) %s => %d/%d [passed]\n", testData.depth, testData.fen, testData.expectedNodeCount, nodes);
            else
            {
                System.out.printf("(%d) %s => %d/%d [failed]\n", testData.depth, testData.fen, testData.expectedNodeCount, nodes);
                numberOfFailedTests++;
            }
        }

        System.out.printf("Testing completed in %d millseconds", System.currentTimeMillis() - startTime);
        System.out.printf("%d tests were run, and %d were incorrect\n", numberOfTests, numberOfFailedTests);
    }
}
