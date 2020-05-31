package freq;

import Jackido.io.JPrint;
import common.StemProcessor;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class SubcatDistributionExtractor {
    private static final int NUMCASE = 5;
    private static LinkedHashSet<String> setArgVerb;
    private static HashMap<String, ArrayList<String>> mapVerbArg;
    private static HashMap<String, int[]> mapVerbSubcat, mapArgVerbSubcat;
    private static NumberFormat formatter;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java SubcatDistributionExtractor <corpus file> <arg-verb file>");
            System.exit(1);
        }

        formatter = new DecimalFormat("#0.0000");
        String corpusFile = args[0];
        String inputFile = args[1];
        mapVerbSubcat = new HashMap<>();
        mapArgVerbSubcat = new HashMap<>();
        readInputFile(inputFile);

        long ln = new File(corpusFile).length();
        long quanta = ln / 100;
        if (quanta == 0) quanta = 1;
        long cc = 0;
        int p = 0;
        System.out.println("\nSubcategorization distribution extracting...");
        try {
            JPrint.printNS(100, "|");
            BufferedReader br = new BufferedReader(new FileReader(corpusFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] stems = StemProcessor.getLabeledStems(line);

                for (int i = 1; i < stems.length - 1; i++) {
                    String s = stems[i];

                    if (mapVerbArg.keySet().contains(s)) {
                        process(stems, s, i);
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

        String resultFile = corpusFile + ".sub";
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile), "UTF-8"));

        Iterator iter = setArgVerb.iterator();
        while (iter.hasNext()) {
            String s = (String) iter.next();
            String[] parts = s.split("\t");
            int[] arr1 = mapArgVerbSubcat.get(s);
            int[] arr2 = mapVerbSubcat.get(parts[1]);
            if (arr1 == null || arr2 == null) {
                out.write(s + "\t<null>\t<null>\t<null>\t<null>\t<null>" + "\n");
            } else {
                out.write(s + "\t" + getDist(arr1, arr2) + "\n");
            }
        }
        out.close();
    }

    private static String getDist(int[] arr1, int[] arr2) {
        long sum1 = 0, sum2 = 0;
        for (int i = 0; i < NUMCASE; i++) {
            sum1 += arr1[i];
            sum2 += arr2[i];
        }
        String result = "";
        for (int i = 0; i < NUMCASE; i++) {
            result += formatter.format((double) arr1[i] / sum1) + "\t";
        }
        for (int i = 0; i < NUMCASE; i++) {
            result += formatter.format((double) arr1[i] / sum1 - (double) arr2[i] / sum2) + "\t";
        }

        return result;
    }

    private static void process(String[] stems, String verb, int index) {
        ArrayList<String> argList = mapVerbArg.get(verb);
        String left = stems[index - 1];
        int begin = 0;
        for (int i = 0; i < index - 1; i++) {
            String stem = stems[i];
            if (stem.endsWith(":v")) begin = i;
        }
        for (int i = begin; i < index; i++) {
            String stem = stems[i];
            int subcatIndex = -1;
            try {
                subcatIndex = Integer.parseInt(stem.substring(stem.length() - 1));
            } catch (Exception e) {
                continue;
            }
            if (subcatIndex >= 1 && subcatIndex <= 5) {
                if (argList.contains(left) && i < index - 1) {
                    int[] arr = mapArgVerbSubcat.get(left + "\t" + verb);
                    if (arr == null) arr = new int[NUMCASE];
                    arr[subcatIndex - 1]++;
                    mapArgVerbSubcat.put(left + "\t" + verb, arr);
                } else {
                    int[] arr = mapVerbSubcat.get(verb);
                    if (arr == null) arr = new int[NUMCASE];
                    arr[subcatIndex - 1]++;
                    mapVerbSubcat.put(verb, arr);
                }
            }
        }
    }

    private static void readInputFile(String file) throws IOException {
        setArgVerb = new LinkedHashSet<>();
        mapVerbArg = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(new File(file)));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            setArgVerb.add(line);
            ArrayList<String> tempList = mapVerbArg.get(parts[1]);
            if (tempList == null) tempList = new ArrayList<>();
            tempList.add(parts[0]);
            mapVerbArg.put(parts[1], tempList);
        }
    }
}