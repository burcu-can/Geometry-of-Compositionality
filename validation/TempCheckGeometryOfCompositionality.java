import Jama.Matrix;
import Jama.SingularValueDecomposition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TempCheckGeometryOfCompositionality {
    private static HashMap<String, double[]> mapWordVector;
    private static int vectorSize;

    public static void main(String[] args) throws IOException {

        String wordVectorFile = args[0];
        String corpusFile = args[1];

        double pcaVarThreshold = 0.45;

        readWordVectors(wordVectorFile);

        BufferedReader br = new BufferedReader(new FileReader(new File(corpusFile)));
        String line = "";
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            String phrase0 = parts[0];
            String context = parts[1];
            String phrase = phrase0.replaceAll(" ", "_");

            ArrayList<String> contextTokens = new ArrayList<>(Arrays.asList(context.split(" ")));

            ArrayList<String> phraseTokens = new ArrayList<>();
            String[] pw = phrase.split("_");
            for (String w : pw) phraseTokens.add(w);

            double[] averagePhrase = getAverage(phraseTokens);
            double[][] output = getPCA(getMatrix(contextTokens), new double[][]{averagePhrase}, pcaVarThreshold);
            double sim = getCosineSimilarity(output[0], averagePhrase);

            System.out.println(sim + "\t" + line);
        }
    }

    private static void readWordVectors(String fileName) {
        mapWordVector = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
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

    private static double[] getAverage(ArrayList<String> list) {
        double[] v_ = new double[vectorSize];
        for (String s : list) {
            double[] v = mapWordVector.get(s);
            if (v == null) continue;
            for (int i = 0; i < vectorSize; i++) {
                v_[i] += v[i];
            }
        }
        return v_;
    }

    private static double[][] getMatrix(ArrayList<String> list) {
        ArrayList<double[]> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            double[] v = mapWordVector.get(list.get(i));
            if (v != null) {
                result.add(v);
            }
        }
        return result.toArray(new double[result.size()][]);
    }

    private static double[][] getPCA(double[][] contextMatrix_, double[][] inputVectorArr, double varThreshold) {
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

    private static double getCosineSimilarity(double[] v1, double[] v2) {
        if (v1 == null || v2 == null) return 0.0;
        double sum1 = 0.0, sum2 = 0.0, sum3 = 0.0;
        for (int i = 0; i < vectorSize; i++) {
            sum1 += v1[i] * v2[i];
            sum2 += v1[i] * v1[i];
            sum3 += v2[i] * v2[i];
        }
        return sum1 / (Math.sqrt(sum2) * Math.sqrt(sum3));
    }
}
