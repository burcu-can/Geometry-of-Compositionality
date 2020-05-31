package freq;

import Jackido.io.JFile;
import Jackido.collection.JMap;
import Jackido.math.JMath;
import Jackido.io.JPrint;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class Monitor {
    private static NumberFormat formatter;

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            System.out.println("Usage: java Monitor <.duo file> <arg file> <min count> <rank size> <which key? 1:arg / 2:verb>");
            System.exit(1);
        }

        // init
        /////////////////////////////////////////////////////////////
        File file = new File(args[0]);
        ArrayList<String> keyList = JFile.readLines(new File(args[1]));
        int minCount = Integer.valueOf(args[2]);
        int rankSize = Integer.valueOf(args[3]);
        String whichKey = args[4];

        formatter = new DecimalFormat("#0.0000");
        LinkedHashMap<String, HashMap<String, Double>> map = new LinkedHashMap<>();
        long numToken = 0;
        /////////////////////////////////////////////////////////////

        // searching
        /////////////////////////////////////////////////////////////
        int quanta = JFile.countLines(file.getAbsolutePath()) / 100;
        if (quanta == 0) quanta = 1;
        System.out.println("\nScanning...");
        try {
            JPrint.printNS(100, "|");
            int cc = 0;
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (i == 0) {
                    numToken = Long.parseLong(line);
                    i++;
                    continue;
                }
                String[] parts = line.split("\t");
                String token1 = parts[0];
                String token2 = parts[1];
                int fAB = Integer.valueOf(parts[2]);
                int fA = Integer.valueOf(parts[3]);
                int fB = Integer.valueOf(parts[4]);

                double jointProb = (double) fAB / numToken;
                double margProbA = (double) fA / numToken;
                double margProbB = (double) fB / numToken;
                double t = (jointProb - margProbA * margProbB) / Math.sqrt(jointProb / numToken);
                double pmi = JMath.log2(jointProb / (margProbA * margProbB));
                double o11 = fAB;
                double o22 = numToken - fA - fB + (2 * fAB);
                double o12 = fB - fAB;
                double o21 = fA - fAB;
                double chi = (numToken * Math.pow(o11 * o22 - o12 * o21, 2)) / ((o11 + o12) * (o11 + o21) * (o12 + o22) * (o21 + o22));

                String tkn = "";
                if (whichKey.equals("1")) {
                    tkn = token1;
                } else if (whichKey.equals("2")) {
                    tkn = token2;
                }

                if (keyList.contains(tkn) && fAB >= minCount && t > 2.576 && token2.endsWith(":v")) {
                    String s = fA + "\t" + fB + "\t" + fAB + "\t" + formatter.format(t) + "\t" + formatter.format(pmi) + "\t" + formatter.format(chi) + "\t" + token1 + "\t" + token2;
                    HashMap<String, Double> innerMap = map.get(tkn);
                    if (innerMap == null) {
                        innerMap = new HashMap<>();
                    }
                    innerMap.put(s, chi);
                    map.put(tkn, innerMap);
                }

                cc++;
                JPrint.printPlus(cc, quanta);
                i++;
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
        System.out.println();
        /////////////////////////////////////////////////////////////

        // result
        /////////////////////////////////////////////////////////////
        String resultFile = file + ".result";
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile), "UTF-8"));
        System.out.println("\n" + resultFile);
        out.write("f_a\tf_b\tf_ab\tt\tpmi\tchi\ta\tb\n");
        for (String token1 : keyList) {
            LinkedHashMap sortedMap = null;
            try {
                sortedMap = JMap.sortByValues(map.get(token1), false);

                Set keySet2 = sortedMap.keySet();
                Iterator iter2 = keySet2.iterator();
                int i = 0;
                while (iter2.hasNext()) {
                    String s = (String) iter2.next();
                    out.write(s + "\n");
                    i++;
                    if (i == rankSize) break;
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        out.close();
        /////////////////////////////////////////////////////////////
    }
}