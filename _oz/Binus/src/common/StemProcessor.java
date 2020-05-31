package common;

public class StemProcessor {
    public static String[] getLabeledStems(String line) {
        // line: <word_form1><space><stem1><space><POS1><space><morpheme_sequence1><space><tag_sequence1><space><space>
        // <word_form2><space><stem2><space><POS2><space><morpheme_sequence2><space><tag_sequence2>
        String[] segments = line.split("  ");
        String[] result = new String[segments.length];
        int i = 0;
        for (String s : segments) {
            result[i] = getLabeledStem(s);
            i++;
        }
        return result;
    }

    public static String getLabeledStem(String token) {
        // olması ol V ol/mA/SH V+PROC/N+INF/POS3S
        String[] parts = token.split(" ");
        String label = getPOSlabel(parts[2]);
        if ("pan".contains(label)) {
            label = getMCaseCode(parts[4]);
        }
        return parts[1] + ":" + label;
    }

    private static String getPOSlabel(String POS) {
        switch (POS) {
            case "-":
                return "u";
            case "V":
                return "v";
            case "N":
                return "n";
            case "Adj":
                return "a";
            case "Adv":
                return "d";
            case "Pro":
                return "p";
            case "Cnj":
                return "c";
            case "Pp":
                return "s";
            default:
                return "o";
        }
    }

    private static String getMCase(String tagSeq) {
        String[] tagArr = tagSeq.split("/");
        for (int i = tagArr.length - 1; i >= 0; i--) {
            if (tagArr[i].equals("ACC") || tagArr[i].equals("DAT") || tagArr[i].equals("ABL") || tagArr[i].equals("ILE") ||
                    tagArr[i].equals("INS") || tagArr[i].equals("LOC") || tagArr[i].equals("GEN") || tagArr[i].equals("EQU")) {
                return tagArr[i];
            }
        }
        return "NOM";
    }

    private static String getMCaseCode(String tagSeq) {
        String mCase = getMCase(tagSeq);
        switch (mCase) {
            case "NOM":
                return "0";
            case "ACC":
                return "1";
            case "DAT":
                return "2";
            case "ABL":
                return "3";
            case "ILE":
            case "INS":
                return "4";
            case "LOC":
                return "5";
            case "GEN":
                return "6";
            case "EQU":
                return "7";
            default:
                return "?";
        }
    }

    public static String clear(String s) {
        if (s.contains(":")) {
            return s.substring(0, s.length() - 2);
        } else {
            return s;
        }
    }
}