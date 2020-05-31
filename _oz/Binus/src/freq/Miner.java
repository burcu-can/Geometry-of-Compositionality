package freq;

import Jackido.collection.JMap;
import Jackido.io.JPrint;
import common.StemProcessor;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Miner {
    private static HashMap<String, Integer> map;

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java Miner <corpus file> <filter> <min count>");
            System.exit(1);
        }

        // init
        /////////////////////////////////////////////////////////////
        map = new HashMap<>();
        File fileToRead = new File(args[0]);
        String filter = args[1];
        int minCount = Integer.parseInt(args[2]);

        long numToken = 0;
        /////////////////////////////////////////////////////////////

        /////////////////////////////////////////////////////////////
        long ln = fileToRead.length();
        long quanta = ln / 100;
        if (quanta == 0) quanta = 1;
        long cc = 0;
        int p = 0;
        System.out.println("\nMining...");
        JPrint.printNS(100, "|");
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileToRead));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("  ");
                numToken += tokens.length;

                for (int i = 0; i < tokens.length; i++) {
                    String s = StemProcessor.getLabeledStem(tokens[i]);
                    if (!filter.contains(s.substring(s.length() - 1))) {
                        JMap.add(map, s);
                    }
                }

                for (int i = 0; i < tokens.length - 1; i++) {
                    String s1 = StemProcessor.getLabeledStem(tokens[i]);
                    String s2 = StemProcessor.getLabeledStem(tokens[i + 1]);
                    if (map.get(s1) != null && map.get(s2) != null && s2.endsWith(":v")) {
                        JMap.add(map, s1 + "\t" + s2);
                    }
                }

                cc += line.length() + 1;
                if (JPrint.printPlus(cc, quanta, p)) {
                    p++;
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
        System.out.println();
        /////////////////////////////////////////////////////////////

        // result
        /////////////////////////////////////////////////////////////
        System.out.println("\nWriting...");
        String resultFile = fileToRead + ".duo_" + minCount + "_" + filter;
        try {
            int quanta2 = map.size() / 100;
            if (quanta2 == 0) quanta2 = 1;
            JPrint.printNS(100, "|");
            int cc2 = 0;
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile), "UTF-8"));
            out.write(numToken + "\n");
            Set keySet = map.keySet();
            Iterator iter = keySet.iterator();
            while (iter.hasNext()) {
                String s = (String) iter.next();
                if (!s.contains("\t")) continue;
                String[] arr = s.split("\t");
                String left = arr[0];
                String right = arr[1];
                int freqBoth = map.get(s);
                int freqLeft = map.get(left);
                int freqRight = map.get(right);

                if (freqLeft >= minCount && freqRight >= minCount) {
                    out.write(left + "\t" + right + "\t" + freqBoth + "\t" + freqLeft + "\t" + freqRight + "\n");
                }
                cc2++;
                JPrint.printPlus(cc2, quanta2);
            }
            out.close();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
        System.out.println("\n");
        System.out.println(resultFile);
        /////////////////////////////////////////////////////////////
    }
}