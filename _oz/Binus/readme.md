**Binus** is a Java project that consists of two packages: freq and embed.

"freq" contains frequent-based operators: Miner, Monitor, RelativeClausePercentageExtractor, and SubcatDistributionExtractor.

* **Miner** mines co-occured (adjacent) token pairs in given corpus file.

* **Monitor** selects significant pairs that contains given elements of arg(or verb) list and generates an arg-verb list.

* **RelativeClausePercentageExtractor** analysis each pairs in an arg-verb list in reverse order and counts ones contains DHK, YAcAK, UL+YAn morphemes. (Example: *diktiği nal, yiyeceği ayva, yazılan destan* etc.)

* **SubcatDistributionExtractor** scans left periphery of an arg-verb pair and only verb in corpus sentences till another verb and counts morphological cases. It prints a subcat distribution of arg-verb pair and the difference from verb subcat distribution.
    * Example: (yol:0 aç:v)    


Subcat distribution of the phrase:

| ACC  | DAT  | ABL  | ILE  | LOC  |
|------|------|------|------|------|
| 0.01 | 0.69 | 0.03 | 0.02 | 0.25 |

Difference from subcat dist of the verb:

| ACC  | DAT  | ABL  | ILE  | LOC  |
|------|------|------|------|------|
| -0.47 | 0.49 | -0.03 | -0.02 | 0.03 |

The second table shows a balance between accusative and dative. This means that the verb *aç-* often requires accusative without *yol* and starts to require dative with *yol*.

"embed" contains **CompositionalityCalculator**. This program scans each sentences in corpus file and calculates compositionality scores for given arg-verb pairs.

**DATA & INPUT**

1. **Corpus file**
    * "GiTuCo.filt.uniq.stemB.sample"
    * This file is main source for all processes.
    * GiTuCo ~ Gigantic Turkish Collection. It consists of 5 billion tokens in various domains such as news, social media, popular science blogs, columns, etc.
    * filt ~ filtered and tokenized. Also sentence detection by Stanford CoreNLP applied on the raw file.
    * uniq ~ sort & uniq bash commands applied.
    * stemB ~ stemmed by NaiveStemmer of TMoST. B means (b)iggest stems selected.
    * sample ~ 0.0001% of original file.
    
    * Line format:
        + Sentence: token1[SPACE][SPACE]token2[SPACE][SPACE]...
        + Token...: word_form[SPACE]stem[SPACE]POS[SPACE]morpheme_sequence[SPACE]tag_sequence
        + Example.: lazio lazio - - -  ile ile Pp ile Pp+ILE  anlaştığı anlaş V anlaş/DHK/SH V+REC/Adj+PAR+PAST/POS3S  iddia iddia N iddia N  edilen eT V eT/UL/YAn V/PASS/Adj+PAR+CONTI  futbolcu futbolcu N futbol/CH N/N+SC  
    
2. **Word-Vector List (.vec file)**
    * "GiTuCo.filt.uniq.stemB.format_S_Rcuspod.samp44p_w5_v200_mc10_i5_cbow.w2v.vec"
    * format ~ the converted form of corpus file line format.
    * S ~ contains only stem
    * Rcuspod ~ removes the tokens labeled as c,u,s,p,o,d. See: Abbreviations.
    * samp44p ~ sample 44% of original file. The percentage is selected due to memory constraints of word2vec algorithm.
    * w5 ~ word embedding window size.
    * v200 ~ vector size.
    * mc10 ~ the minimum number of observations for the token.
    * i5 ~ number of epochs in Gensim Word2Vec (details: https://datascience.stackexchange.com/questions/9819/number-of-epochs-in-gensim-word2vec-implementation)
    * cbow ~ continuous bag of words.
    * w2v ~ word2vec
    * vec ~ simple word vector format:
        word[TAB]number[TAB]number[TAB]...

3. **Verb List**
    * "_verb_list_50"
    * selected verbs from GiTuCo.
    * 50 most common verbs.

4. **Arg-Verb List**
    * "_arg_verb_list_50x100"
    * Corpus File --> Miner --> Duo File & Verb List --> Monitor --> Arg-Verb List
    * phrases examined whether they are idioms or literal expressions.

**USAGE**

First:

    cd out/production/Binus

1. **Mining:**

        java -cp .:../../../_lib/Jackido-0.1.jar freq.Miner ../../../_io/GiTuCo.filt.uniq.stemB.sample cuspod 10

2. **Monitoring:**

        java -cp .:../../../_lib/Jackido-0.1.jar freq.Monitor ../../../_io/GiTuCo.filt.uniq.stemB.sample.duo_10_cuspod ../../../_io/_verb_list_50 10 20 2

3. **Relative Clause Extracting:**

        java -cp .:../../../_lib/Jackido-0.1.jar freq.RelativeClausePercentageExtractor ../../../_io/GiTuCo.filt.uniq.stemB.sample ../../../_io/_arg_verb_list_50x100
    
4. **Subcategorization Extracting:**

        java -cp .:../../../_lib/Jackido-0.1.jar freq.SubcatDistributionExtractor ../../../_io/GiTuCo.filt.uniq.stemB.sample ../../../_io/_arg_verb_list_50x100    

5. **CompositionalityCalculator**

    Arguments:<br/>
    [word vector file] [corpus file] [input file] [folder to save output file] [minimum sample size] [minimum context size] [window size] [filters] [is keyword included? true/false] [is sentence printed? true/false] [PCA variance threshold] [number of threads]

    Table Mode:<br/>
        * let [is sentence printed?] be false<br/>
    
        java -cp .:../../../_lib/Jama-1.0.3.jar:../../../_lib/Jackido-0.1.jar embed.CompositionalityCalculator ../../../_io/GiTuCo.filt.uniq.stemB.format_S_Rcuspod.samp44p_w5_v200_mc10_i5_cbow.w2v.vec ../../../_io/GiTuCo.filt.uniq.stemB.sample ../../../_io/_arg_verb_list_50x100 ../../../_io/ 30 15 20 cuspod true false 0.45 8


    List Mode:<br/>
        * let [is sentence printed?] be true<br/>
        * in this mode, [minimum sample size] is ineffective.<br/>
        * [number of threads] must be 1. otherwise result list is never stable due to randomness of threads.<br/>
        * just write "null" for [input file]
    
        java -cp .:../../../_lib/Jama-1.0.3.jar:../../../_lib/Jackido-0.1.jar embed.CompositionalityCalculator ../../../_io/GiTuCo.filt.uniq.stemB.format_S_Rcuspod.samp44p_w5_v200_mc10_i5_cbow.w2v.vec ../../../_io/tr25_sent.stemB_ null ../../../_io/ 30 15 20 cuspod true true 0.45 1

**ABBREVIATIONS**

c   conjunction<br/>
u   unknown<br/>
s   postposition<br/>
p   pronoun<br/>
o   other<br/>
d   adverb<br/>
a   adjective<br/>
n   noun<br/>
v   verb<br/>
0   BARE<br/>
1   ACC<br/>
2   DAT<br/>
3   ABL<br/>
4   ILE<br/>
5   LOC<br/>
6   GEN<br/>
7   EQU<br/>
