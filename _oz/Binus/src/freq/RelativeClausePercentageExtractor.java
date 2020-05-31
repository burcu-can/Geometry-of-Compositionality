package freq;

import Jackido.collection.JMap;
import Jackido.io.JPrint;
import common.StemProcessor;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class RelativeClausePercentageExtractor {
    private static HashMap<String, Integer> map1, map2;
    private static LinkedHashSet<String> setArgVerb, setArgVerb_;
    private static NumberFormat formatter;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java RelativeClausePercentageExtractor <corpus file> <arg-verb file>");
            System.exit(1);
        }

        formatter = new DecimalFormat("#0.0000");
        map1 = new HashMap<>();
        map2 = new HashMap<>();
        String corpusFile = args[0];
        String inputFile = args[1];
        readInputFile(inputFile);

        long ln = new File(corpusFile).length();
        long quanta = ln / 100;
        if (quanta == 0) quanta = 1;
        long cc = 0;
        int p = 0;
        System.out.println("\nRelative clause extracting...");
        try {
            JPrint.printNS(100, "|");
            BufferedReader br = new BufferedReader(new FileReader(corpusFile));
            String line;
            while ((line = br.readLine()) != null) {
                // line: <word_form1><space><stem1><space><POS1><space><morpheme_sequence1><space><tag_sequence1><space><space>
                // <word_form2><space><stem2><space><POS2><space><morpheme_sequence2><space><tag_sequence2>
                String[] items = line.split("  ");

                for (int i = 0; i < items.length - 1; i++) {
                    String item1 = items[i];
                    String item2 = items[i + 1];
                    String[] item1Arr = item1.split(" ");
                    String[] item2Arr = item2.split(" ");

                    if (item1Arr.length == 1 || item2Arr.length == 1) continue;

                    String stem1 = item1Arr[1];
                    String stem2 = item2Arr[1];
                    List mList1 = Arrays.asList(item1Arr[3].split("/"));

                    if (setArgVerb_.contains(stem2 + "\t" + stem1)) {
                        JMap.addMap1(map1, stem2 + "\t" + stem1);
                        if ((mList1.contains("YAcAK") || mList1.contains("DHK") || (mList1.contains("UL") && mList1.contains("YAn")))) {
                            JMap.addMap1(map2, stem2 + "\t" + stem1);
                        }
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

        String resultFile = corpusFile + ".rel";
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile), "UTF-8"));

        Iterator iter = setArgVerb.iterator();
        while (iter.hasNext()) {
            String s = (String) iter.next();
            String s_ = StemProcessor.clear(s.split("\t")[0]) + "\t" + StemProcessor.clear(s.split("\t")[1]);
            if (map1.get(s_) == null || map2.get(s_) == null) {
                out.write(s + "\t0\t0\t0" + "\n");
            } else {
                out.write(s + "\t" + formatter.format((double) map2.get(s_) / map1.get(s_)) + "\t" + map2.get(s_) + "\t" + map1.get(s_) + "\n");
            }
        }
        out.close();
    }

    private static void readInputFile(String file) throws IOException {
        setArgVerb = new LinkedHashSet<>();
        setArgVerb_ = new LinkedHashSet<>();
        BufferedReader br = new BufferedReader(new FileReader(new File(file)));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            setArgVerb.add(line);
            setArgVerb_.add(StemProcessor.clear(parts[0]) + "\t" + StemProcessor.clear(parts[1]));
        }
    }
}