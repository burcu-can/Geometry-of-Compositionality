package embed;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import common.StemProcessor;
import Jackido.io.JFile;
import Jackido.io.JPrint;
import Jackido.math.JStatistics;
import Jackido.math.JVector;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class CompositionalityCalculator implements Runnable {
    private static HashMap<String, double[]> mapWordVector;
    private static Map<String, ArrayList<Double>[]> mapPhraseSimilarity;
    private static HashMap<String, double[][]> mapPhraseRepresentative;
    private static int vectorSize, window, minContextSize, quanta;
    private static double pcaVarThreshold;
    private static LinkedHashSet<String> setInput;
    private static File corpusFile;
    private static String filters;
    private static boolean isKeywordIncluded;
    private static boolean isSentencePrinted;
    private static Writer outSent;
    private static NumberFormat formatter = new DecimalFormat("#0.0000");

    private int start, end;

    public static void main(String[] args) throws IOException, InterruptedException {
        // argument checking
        if (args.length != 12) {
            warning();
        }

        // variable declarations
        File wordVectorFile = null, inputFile = null;
        String info = null, outputFolder = null;
        int minSampleSize = 0, numThread = 0;

        // assigning arguments to variables
        try {
            wordVectorFile = new File(args[0]);
            corpusFile = new File(args[1]);
            inputFile = new File(args[2]);
            outputFolder = args[3];
            minSampleSize = Integer.parseInt(args[4]);
            minContextSize = Integer.parseInt(args[5]);
            window = Integer.parseInt(args[6]);
            filters = args[7];
            isKeywordIncluded = Boolean.parseBoolean(args[8]);
            isSentencePrinted = Boolean.parseBoolean(args[9]);
            pcaVarThreshold = Double.parseDouble(args[10]);
            numThread = Integer.parseInt(args[11]);

            if (!outputFolder.endsWith("/")) outputFolder += "/";
            info = "word vector file:\t\t" + wordVectorFile.getName() + "\ncorpus file:\t\t\t" + corpusFile.getName() + "\ninput file:\t\t\t\t" + inputFile.getName() + "\nminimum sample size:\t" +
                    minSampleSize + "\nminimum context size:\t" + minContextSize + "\nwindow size:\t\t\t" + window + "\nfilters:\t\t\t\t" + filters + "\nis keyword included?\t" + isKeywordIncluded +
                    "\nPCA variance threshold:\t" + pcaVarThreshold + "\nnumber of threads:\t\t" + numThread + "\n\n";
        } catch (Exception e) {
            System.out.println(e);
            warning();
        }

        if (isSentencePrinted) {
            outSent = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFolder + "__output.list"), "UTF-8"));
            outSent.write(info);
            outSent.write("argComp\tverbComp\tphraseAvgComp\tphrasePCAcomp\targ\tverb\tcontext\tsentence\tformatted sentence\n");
        }

        // initialization
        System.out.println("Initialization starting...");
        readWordVectors(wordVectorFile);
        setInput = JFile.readLinesToSet(inputFile);
        fillMapPhraseRepresentative();
        int ln = JFile.countLines(corpusFile.getAbsolutePath());
        System.out.println("Initialization finished.");
        quanta = ln / 100;
        if (quanta == 0) quanta = 1;
        JPrint.printNS(100, "|");

        // run
        Thread threads[] = new Thread[numThread];
        int qt = ln / numThread;
        int end;
        for (int i = 0; i < numThread; i++) {
            if (i == numThread - 1) end = ln;
            else end = (i + 1) * qt;
            threads[i] = new Thread(new CompositionalityCalculator(i * qt, end));
            threads[i].start();
        }

        for (int i = 0; i < numThread; i++) {
            threads[i].join();
        }

        // finalization
        if (isSentencePrinted) {
            outSent.close();
        } else {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFolder + "__output.table"), "UTF-8"));
            out.write(info);
            out.write("arg\tverb\tsample size\targComp\tverbComp\tphraseAvgComp\tphrasePCAcomp\n");
            Iterator iter;
            if (setInput == null) {
                Set keySet = mapPhraseSimilarity.keySet();
                iter = keySet.iterator();
            } else {
                iter = setInput.iterator();
            }
            while (iter.hasNext()) {
                String phrase = (String) iter.next();
                ArrayList<Double>[] arr = mapPhraseSimilarity.get(phrase);
                if (arr != null) {
                    if (arr[0].size() >= minSampleSize) {
                        out.write(phrase + "\t" + arr[0].size() + "\t" + formatter.format(JStatistics.mean(arr[0])) + "\t" + formatter.format(JStatistics.mean(arr[1])) + "\t" + formatter.format(JStatistics.mean(arr[2])) + "\t" + formatter.format(JStatistics.mean(arr[3])) + "\n");
                    } else {
                        out.write(phrase + "\t" + arr[0].size() + "\t" + "null" + "\t" + "null" + "\t" + "null" + "\t" + "null" + "\n");
                    }
                } else {
                    out.write(phrase + "\t" + "null" + "\t" + "null" + "\t" + "null" + "\t" + "null" + "\t" + "null" + "\n");
                }
            }
            out.close();
        }

        System.out.println();
    }

    private static void warning() {
        System.out.println(
                "Usage: java CompositionalityCalculator <word vector file> <corpus file> <input file> <folder to save output file>" +
                "<minimum sample size> <minimum context size> <window size> <filters> <is keyword included? true/false> " +
                "<is sentence printed? true/false> <PCA variance threshold> <number of threads>");
        System.exit(1);
    }

    private static void fillMapPhraseRepresentative() {
        if (setInput != null) {
            mapPhraseRepresentative = new HashMap<>();
            Iterator iter = setInput.iterator();
            while (iter.hasNext()) {
                String s = (String) iter.next();
                List<String> phraseTokens = Arrays.asList(s.split("\t"));
                mapPhraseRepresentative.put(s, new double[][]{getAverage(phraseTokens), getPCAphrase(phraseTokens)});
            }
        }
    }

    private static void readWordVectors(File file) {
        mapWordVector = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int c = 0;
            loop:
            while ((line = br.readLine()) != null) {
                String[] part = line.split("\t");
                if (c == 0) vectorSize = part.length - 1;
                String word = part[0];
                double[] vec = new double[vectorSize];
                for (int i = 1; i < part.length; i++) {
                    try {
                        vec[i - 1] = Double.valueOf(part[i]);
                    } catch (Exception e) {
                        continue loop;
                    }
                }
                mapWordVector.put(word, vec);
                c++;
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private String[] filter(String[] tokens, String filters) {
        if (filters.equals(".")) {
            return tokens;
        }
        ArrayList<String> result = new ArrayList<>();
        boolean found = false;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (i < tokens.length - 1 && (setInput == null || setInput.contains(token + "\t" + tokens[i + 1]))) found = true;
            if (!token.contains(":") && mapWordVector.get(StemProcessor.clear(token)) != null) {
                result.add(token);
                continue;
            }
            if (token.contains(":") && !filters.contains(token.split(":")[1]) && mapWordVector.get(StemProcessor.clear(token)) != null) {
                result.add(token);
            }
        }
        if (!found) return null;
        return result.toArray(new String[result.size()]);
    }

    private static double[] getAverage(List<String> list) {
        double[] v_ = new double[vectorSize];
        for (String s : list) {
            double[] v = mapWordVector.get(StemProcessor.clear(s));
            if (v == null) continue;
            for (int i = 0; i < vectorSize; i++) {
                v_[i] += v[i];
            }
        }
        return v_;
    }

    private static double[] getPCAphrase(List<String> phraseTokens) {
        if (mapWordVector.get(StemProcessor.clear(phraseTokens.get(0))) == null) return mapWordVector.get(StemProcessor.clear(phraseTokens.get(1)));
        if (mapWordVector.get(StemProcessor.clear(phraseTokens.get(1))) == null) return mapWordVector.get(StemProcessor.clear(phraseTokens.get(0)));

        double[][] phraseMatrix = getMatrix(phraseTokens);
        Matrix context = new Matrix(phraseMatrix).transpose();
        SingularValueDecomposition svd = context.svd();
        Matrix u = svd.getU().transpose();
        return u.getArray()[0];
    }

    private double[][] getPCA(double[][] contextMatrix_, double[][] inputVectorArr, double varThreshold) {
        Matrix context = new Matrix(contextMatrix_).transpose();
        SingularValueDecomposition svd = context.svd();

        double[] sValues = svd.getSingularValues();
        double[] explainedVariance = new double[sValues.length];
        double t = 0.0;
        for (int i = 0; i < sValues.length; i++) {
            double d = sValues[i];
            explainedVariance[i] = d * d / sValues.length;
            t += explainedVariance[i];
        }
        double[] explainedVarianceRatioCumulative = new double[sValues.length];
        double pv = 0.0;
        for (int i = 0; i < sValues.length; i++) {
            if (i > 0) pv = explainedVarianceRatioCumulative[i - 1];
            explainedVarianceRatioCumulative[i] += pv + explainedVariance[i] / t;
        }

        int nThreshold = 0;
        for (int i = 0; i < sValues.length; i++) {
            if (explainedVarianceRatioCumulative[i] > varThreshold) {
                nThreshold = i + 1;
                break;
            }
        }

        Matrix u = svd.getU().transpose();

        double[][] result = new double[inputVectorArr.length][];
        for (int i = 0; i < inputVectorArr.length; i++) {
            double[] inputVector = inputVectorArr[i];

            double[][] temp = new double[1][contextMatrix_[0].length];
            temp[0] = inputVector;
            Matrix inputVector_ = new Matrix(temp).transpose();

            temp = new double[nThreshold][];
            for (int j = 0; j < nThreshold; j++) {
                temp[j] = u.getArray()[j];
            }
            Matrix u_ = new Matrix(temp);

            Matrix uT = u_.transpose();
            Matrix uuT = u_.times(uT);
            Matrix col = u_.times(inputVector_);
            Matrix coef = uuT.solve(col);
            Matrix approach = uT.times(coef);

            result[i] = approach.transpose().getArray()[0];
        }
        return result;
    }

    private static double[][] getMatrix(List<String> list) {
        ArrayList<double[]> rList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            double[] v = mapWordVector.get(StemProcessor.clear(list.get(i)));
            if (v != null) {
                rList.add(v);
            }
        }
        return rList.toArray(new double[rList.size()][]);
    }

    public CompositionalityCalculator(int start, int end) {
        this.start = start;
        this.end = end;
        mapPhraseSimilarity = Collections.synchronizedMap(new HashMap<>());
    }

    public void run() {
        String line = "", p = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(corpusFile));
            int i = -1, cc = 0;
            while ((line = br.readLine()) != null) {

                i++;
                if (i >= start && i < end) {
                    cc++;
                    JPrint.printPlus(cc, quanta);

                    if (setInput == null && line.contains("\t")) {
                        String[] tempL = line.split("\t");
                        String[] tempP = tempL[0].split(" ");
                        String a = tempP[0];
                        String b = tempP[1];

                        String[] items = StemProcessor.getLabeledStems(tempL[1]);
                        if ((items = filter(items, filters)) == null || items.length < 2) continue;
                        int index = -1;
                        for (int j = 0; j < items.length - 1; j++) {
                            if (items[j].equals(a) && items[j + 1].equals(b)) {
                                index = j;
                                break;
                            }
                        }
                        String result = process(items, index, a, b);

                        if (isSentencePrinted) {
                            outSent.write(result + "\t" + line + "\n");
                        }
                    } else if (!line.contains("\t")) {
                        String[] items = StemProcessor.getLabeledStems(line);
                        if ((items = filter(items, filters)) == null || items.length < 2) continue;

                        for (int j = 0; j < items.length - 1; j++) {
                            String currItem = items[j];
                            String nextItem = items[j + 1];
                            p = currItem + "\t" + nextItem;

                            if (setInput == null || setInput.contains(p)) {
                                String result = process(items, j, currItem, nextItem);
                                if (isSentencePrinted && result != null) {
                                    outSent.write(result + "\t" + getSentence(line) + "\t" + line + "\n");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception is caught: " + e);
            System.out.println(p + "\t" + line);
        }
    }

    private String getSentence(String line) {
        String[] arr1 = line.split("  ");
        String result = "";
        for (String s : arr1) {
            String[] arr2 = s.split(" ");
            result += arr2[0] + " ";
        }
        return result;
    }

    private String process(String[] tokens, int index, String a, String b) {
        ArrayList<String> contextTokens = new ArrayList<>();
        int left = index - window;
        if (left < 0) left = 0;
        int right = index + 1 + window;
        if (right + 1 > tokens.length) right = tokens.length;
        for (int i = left; i < right; i++) {
            if (i == index || i == index + 1) continue;
            if (i < tokens.length - 1 && tokens[i].equals(a) && tokens[i + 1].equals(b)) {
                i++;
                continue;
            }
            if (!isKeywordIncluded && (tokens[i].equals(a) || tokens[i].equals(b))) continue;
            contextTokens.add(StemProcessor.clear(tokens[i]));
        }

        String contextSeq = "";
        for (String ct : contextTokens) contextSeq += ct + " ";

        double sim1 = 0.0, sim2 = 0.0, sim3 = 0.0, sim4 = 0.0;
        if (contextTokens.size() >= minContextSize) {

            double[] argVector = mapWordVector.get(StemProcessor.clear(a));
            double[] verbVector = mapWordVector.get(StemProcessor.clear(b));

            if (argVector == null || verbVector == null) return "null\tnull\tnull\tnull\t" + a + "\t" + b + "\t" + contextSeq;

            double[][] averagePhrase = null;
            if (setInput == null) {
                List<String> phraseTokens = List.of(a, b);
                averagePhrase = new double[][]{getAverage(phraseTokens), getPCAphrase(phraseTokens)};
            } else {
                averagePhrase = mapPhraseRepresentative.get(a + "\t" + b);
            }

            double[][] output = getPCA(getMatrix(contextTokens), new double[][]{argVector, verbVector, averagePhrase[0], averagePhrase[1]}, pcaVarThreshold);

            sim1 = JVector.getCosineSimilarity(output[0], argVector);
            sim2 = JVector.getCosineSimilarity(output[1], verbVector);
            sim3 = JVector.getCosineSimilarity(output[2], averagePhrase[0]);
            sim4 = JVector.getCosineSimilarity(output[3], averagePhrase[1]);

            ArrayList<Double>[] temp = mapPhraseSimilarity.get(a + "\t" + b);
            if (temp == null) {
                temp = new ArrayList[4];
                for (int i = 0; i < 4; i++) {
                    temp[i] = new ArrayList<>();
                }
            }
            temp[0].add(sim1);
            temp[1].add(sim2);
            temp[2].add(sim3);
            temp[3].add(sim4);

            if (!isSentencePrinted) {
                mapPhraseSimilarity.put(a + "\t" + b, temp);
            }

            return sim1 + "\t" + sim2 + "\t" + sim3 + "\t" + sim4 + "\t" + a + "\t" + b + "\t" + contextSeq;
        }

        return null;
    }
}