package org.aksw.fox.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * 
 * @author rspeck
 * 
 */
public class FoxTextUtil {

    public static Logger logger = Logger.getLogger(FoxTextUtil.class);

    public static final String tokenSpliter = "[\\p{Punct}&&[^-\\_/&+.]]| |\\t|\\n";

    private FoxTextUtil() {
    }

    /**
     * Gets the content from html/text as plain text.
     */
    public static synchronized String urlToText(String url) {
        logger.info("extractFromUrl ... ");
        String html = "";
        URL u = null;
        try {
            u = new URL(url);
            if (u != null) {

                HttpURLConnection connection = (HttpURLConnection) u.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                InputStream stream = connection.getInputStream();

                StringBuilder sb = new StringBuilder();
                String line;
                try {

                    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                html = sb.toString();
            }

        } catch (Exception e) {
            logger.error("\n", e);
        }

        return htmlToText(html);
    }

    /**
     * Gets the content from html/text as plain text.
     */
    public static synchronized String htmlToText(String html) {
        logger.info("extractFromHTML ... ");

        // TODO: make linebreaks, we can use a cfg for html parser?
        html = html.replaceAll("<li>", "<li>, ");
        html = html.replaceAll("</dd>", ",</dd>");
        Source src = new Source(html);
        return new TextExtractor(new Segment(src, src.getBegin(), src.getEnd())).setConvertNonBreakingSpaces(false).toString();
    }

    /**
     * 
     * @param input
     * @return
     */
    public static synchronized String[] getSentencesToken(String input) {

        List<String> result = new ArrayList<>();
        for (String sentence : _getSentences(input))
            result.addAll(new ArrayList<>(Arrays.asList(getSentenceToken(sentence))));

        return result.toArray(new String[result.size()]);
    }

    public static synchronized String[] getSentences(String source) {

        String[] sentences = _getSentences(source);

        // logger.info("sentences: " + sentences.length);

        return sentences;
    }

    /**
     * Gets sentences.
     * 
     * @param source
     *            plain text of sentences
     * @return sentences
     */
    protected static synchronized String[] _getSentences(String source) {
        // TODO: use a better one?

        InputStream modelIn = null;
        try {
            modelIn = new FileInputStream("data/openNLP/en-sent.bin");
        } catch (FileNotFoundException e) {
            logger.error("\n", e);
        }
        if (modelIn == null)
            return null;

        String[] sentences = null;
        try {
            SentenceModel model = new SentenceModel(modelIn);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
            sentences = sentenceDetector.sentDetect(source);
        } catch (IOException e) {
            logger.error("\n", e);
        } finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                } catch (IOException e) {
                    logger.error("\n", e);
                }
            }
        }

        return sentences;
    }

    /**
     * Gets token of one sentence, token defined by
     * {@link FoxTextUtil#tokenSpliter}.
     * 
     * @param sentence
     *            (with punctuation mark)
     * @return token
     */
    public static synchronized String[] getSentenceToken(String sentence) {
        // Note: Points won't removed, so we remove punctuation marks to points
        // and handle them later
        char punctuationMark = sentence.trim().charAt(sentence.trim().length() - 1);
        if (punctuationMark == '!' || punctuationMark == '?') {
            int punctuationMarkIndex = sentence.lastIndexOf(punctuationMark);
            sentence = sentence.substring(0, punctuationMarkIndex) + "." + sentence.substring(punctuationMarkIndex + 1, sentence.length());
        }

        String[] token = null;
        token = getToken(sentence);

        // remove punctuation mark(points)
        String lastToken = token[token.length - 1];
        if (lastToken.charAt(lastToken.length() - 1) == '.')
            token[token.length - 1] = lastToken.substring(0, lastToken.length() - 1);

        // add a token to keep original length
        int len = sentence.length();

        String cleanSentence = StringUtils.join(token, " ");

        int cleanSentenceLen = cleanSentence.length();

        String closeLen = "";
        while (cleanSentenceLen + closeLen.length() < len) {

            closeLen += " ";
        }
        // add this token
        if (!closeLen.isEmpty())
            token = ArrayUtils.add(token, token.length, closeLen);

        // logger.info("----");
        // logger.info("<" + len + ">");
        // logger.info("<" + cleanSentenceLen + ">");
        // logger.info("<" + sentence + ">");
        // logger.info("<" + cleanSentence + ">");
        // logger.info("<" + StringUtils.join(token, " ") + ">");
        // logger.info("<" + token[token.length - 1] + ">");

        return token;
    }

    /**
     * Gets token defined by {@link FoxTextUtil#tokenSpliter}.
     * 
     * @param in
     *            string to split
     * @return token
     */
    public static synchronized String[] getToken(String in) {
        return in.split(tokenSpliter);
    }

    public static synchronized List<Integer> getIndex(String token, String tokenInput) {

        token = Pattern.quote(" " + token.trim() + " ");
        List<Integer> indices = new ArrayList<>();
        Matcher matcher = Pattern.compile(token).matcher(" " + tokenInput + " ");
        while (matcher.find())
            indices.add(matcher.start() + 1 - 1);

        return indices;
    }
}