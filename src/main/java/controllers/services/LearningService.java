package controllers.services;

import edu.stanford.nlp.simple.Document;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import models.Learning;
import database.Template;

/**
 * Learning service abstraction.
 * Based on stanford pos tagging and sentiment analysis.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class LearningService {

    /**
     * POS tags to extract the nouns.
     */
    private static final String[] TAGS_NOUN = {"NN", "NNS", "NNP", "NNPS"};

    /**
     * POS tag to extract the verbs.
     */
    private static final String[] TAGS_VERB = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};

    /**
     * words extracted.
     */
    private Map<String, Set<String>> words = new HashMap<>();

    /**
     * questions extracteed.
     */
    private Map<String, Set<String>> questions = new HashMap<>();

    public Map<String, Set<String>> getWords() {
        return words;
    }

    public Map<String, Set<String>> getQuestions() {
        return questions;
    }

    /**
     * Extracts words/questions from a learning object.
     * @param learning Learning object to process.
     * @return LearningResult object.
     */
    public LearningResult extract(Learning learning) {
        Document corpus      = new Document(learning.getCorpus());
        Set wordsToTrain     = words.get(learning.getCategory());
        Set questionsToTrain = questions.get(learning.getCategory());
        corpus.sentences().forEach(sentence -> {
            boolean seenBefore = false;
            for (int i = 0; i < sentence.words().size(); i++) {
                // extract nouns
                if (Arrays.asList(TAGS_NOUN).contains(sentence.posTag(i))) {
                    wordsToTrain.add(sentence
                                    .word(i)
                                    .trim()
                                    .toLowerCase());
                } // extract questions
                else if (!seenBefore && Arrays.asList(TAGS_VERB).contains(sentence.posTag(i))) {
                    // no 'be' statements
                    if (!sentence.lemma(i).equals("be")) {
                        String question = String.format(Template.QUESTION_RETRIEVED,
                                sentence.lemma(i),
                                sentence.substring(i + 1, sentence.length() - 1));
                        // escape parentheses
                        question = question
                                .replace("-LRB-", "(")
                                .replace("-RRB-", ")");
                        questionsToTrain.add(question);
                    }
                    seenBefore = true;
                }
            }
        });

        return new LearningResult(wordsToTrain, questionsToTrain);
    }

    /**
     * Result wrapper class.
     */
    public static class LearningResult {

        Set<String> wordsToTrain;
        Set<String> questionsToTrain;

        public LearningResult(Set<String> wordsToTrain, Set<String> questionsToTrain) {
            this.wordsToTrain = wordsToTrain;
            this.questionsToTrain = questionsToTrain;
        }

        public Set<String> getWordsToTrain() {
            return wordsToTrain;
        }

        public Set<String> getQuestionsToTrain() {
            return questionsToTrain;
        }
    }

}
