
package controllers;

import ninja.Result;
import ninja.Results;

import com.google.inject.Singleton;
import models.Training;
import ninja.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.simple.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import models.Message;
import org.eclipse.jetty.util.StringUtil;
import database.Database;
import database.PropertyDB;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import models.Category;
import assets.Template;

@Singleton

public class AppController {
    
    private static final Logger LOG = LoggerFactory.getLogger(AppController.class);
    
    private final static String[] TAGS_NOUN = {"NN", "NNS", "NNP", "NNPS"};
    
    private final static String[] TAGS_VERB = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
        
    public static final Random RND = new Random();
    
    /**
     * categorized.
     */
    private final Map<String, Set<String>> words = new HashMap<>();
    
    private final Map<String, Set<String>> questions = new HashMap<>();
    
    private final Map<String, Set<String>> questionsAnswered = new HashMap<>();
    
    private Set<String> vocab = new HashSet<>();
    
    private final List<String> messages = new ArrayList<>();
    
    private final Map<String, SentimentClass> sentiments = new HashMap<>();
    
    private Category current;
    
    // TODO multiple users
    Database db = new PropertyDB("userA");
    
    public AppController() throws IOException {
        // load from persistent db
            // words and questions for each category (e.g. front- or backend)
        for (Category category : Category.values()) {
            words.put(category.name(), db.loadCollection("words." + category.name(), new HashSet<>()));
            questions.put(category.name(), db.loadCollection("questions." + category.name(), new HashSet<>()));
            questionsAnswered.put(category.name(), db.loadCollection("questions.answered." + category.name(), new HashSet<>()));
        }
            // sentiment analysis
        List<String> sentimentKeys = db.loadCollection("sentiments.keys", new ArrayList<>());        
        List<String> sentimentVals = db.loadCollection("sentiments.vals", new ArrayList<>());   
        for (int i = 0; i < sentimentKeys.size(); i++) {
            sentiments.put(
                    sentimentKeys.get(i),
                    SentimentClass.valueOf(sentimentVals.get(i)));
        }
        
        vocab = words.values().stream()
                .flatMap(w -> w.stream())
                .collect(Collectors.toSet());
        messages.add("Hello UserA! I want to ask you some questions.");
    }
    
    /**
     * Index GET.
     * @return 
     */
    public Result index() {
        return Results.html();
    }
    
    /**
     * Training GET.
     *
     * @return
     */
    public Result training() {
        return Results.html().render("categories", Category.values());
    }
    
    /**
     * Training execution POST.
     *
     * @param context
     * @param training
     * @return
     * @throws java.io.IOException
     */
    public Result trainingExec(Context context, Training training) throws IOException {
        LOG.info("Training with: {}", training);
        Document corpus      = new Document(training.getCorpus());
        Set wordsToTrain     = words.get(training.getCategory().name());
        Set questionsToTrain = questions.get(training.getCategory().name());
        
        corpus.sentences().forEach(sentence -> {
            boolean seenBefore = false;
            for (int i = 0; i < sentence.words().size(); i++) {
                // extract relevant nouns of corpus
                if (Arrays.asList(TAGS_NOUN).contains(sentence.posTag(i))) {
                    wordsToTrain.add(sentence.word(i).trim().toLowerCase());
                }
                // extract relevant questions
                else if (!seenBefore && Arrays.asList(TAGS_VERB).contains(sentence.posTag(i))) {
                    // no 'be' statements
                    if (!sentence.lemma(i).equals("be")) {
                        String question = String.format(Template.RETRIEVAL,
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
        
        LOG.info("questions extracted: " + questions.toString());
        
        // save to db
        db.saveCollection("words." + training.getCategory(), wordsToTrain);
        db.saveCollection("questions." + training.getCategory(), questionsToTrain);
        // update vocab
        vocab = words.values().stream()
                .flatMap(w -> w.stream())
                .collect(Collectors.toSet());
        return Results.html().render(training);
    }
    
    /**
     * Dialogue GET/POST.
     *
     * @param context
     * @param message
     * @return
     * @throws java.io.IOException
     */
    public Result dialogue(Context context, Optional<Message> message) throws IOException {
        if (message.isPresent()) {
            String text = message.get().getText();
            if (!StringUtil.isBlank(text)) {
                messages.add(text);
                Document doc      = new Document(text);
                AtomicInteger avg = new AtomicInteger();
                doc.sentences().forEach(sentence -> {
                    // get overall msg sentiment
                    switch (sentence.sentiment()) {
                        case VERY_POSITIVE:
                        case POSITIVE: avg.incrementAndGet(); break;
                        case VERY_NEGATIVE:
                        case NEGATIVE: avg.decrementAndGet(); break;
                        case NEUTRAL:
                        default:
                    }
                    
                    // recognizing word
                    sentence.words().stream()
                            .map(String::toLowerCase)
                            .filter(word -> vocab.contains(word))
                            .forEach(recognized -> {                               
                                // save sentiment of word
                                sentiments.put(recognized, sentence.sentiment());
                                try {
                                    // general sentiment
                                    if (current != null) {
                                        switch (sentence.sentiment()) {
                                            case VERY_POSITIVE:
                                            case POSITIVE:
                                                db.saveInt(current.name(), db.loadInt(current.name(), 0) + 1);
                                                break;
                                            case VERY_NEGATIVE:
                                            case NEGATIVE:
                                                db.saveInt(current.name(), db.loadInt(current.name(), 0) - 1);
                                                break;
                                            case NEUTRAL:
                                            default:
                                        }
                                    }
                                    // word specific sentiment
                                    db.saveCollection("words.used", sentiments.keySet());
                                    db.saveCollection("words.used.sentiment", sentiments.values());
                                }
                                catch (IOException ex) {
                                    LOG.error("db access failed.", ex);
                                }
                                LOG.info("message sentiment: {} [{}]",
                                        recognized,
                                        sentence.sentiment());
                            });
                });
                
                if (current != null) {
                    // general analysis counter
                    int delta = avg.get() >= 0
                            ? 1
                            : -1;
                    db.saveInt(current.name(), db.loadInt(current.name(), 0) + delta);
                    
                    // remove answered question
                    String prevQuestion = messages.get(messages.size() - 2);
                    Set<String> categorized = questionsAnswered.get(current.name());
                    categorized.add(prevQuestion);
                    db.saveCollection("questions.answered." + current, categorized);
                    db.saveCollection("questions.answered.sentiment" + current, categorized);
                    questions.get(current.name()).remove(prevQuestion);
                }
                
                // new question
                current = randomEnum(Category.class);
                Optional<String> nextQuestion;
                if (RND.nextBoolean()) {
                    // retrieve question
                    Set<String> subquestions = questions.get(current.name());
                    nextQuestion = subquestions.stream()
                            .skip((int) (subquestions.size() * Math.random()))
                            .findFirst();
                }
                else {
                    // generate question
                    Set<String> subwords = words.get(current.name());
                    Optional<String> nextWord = subwords.stream()
                            // only unused words
                            .filter(w -> !sentiments.keySet().contains(w))
                            .skip((int) (subwords.size() * Math.random()))
                            .findFirst();
                    nextQuestion = Optional.ofNullable(String.format(
                            Template.generate(), nextWord.get()));
                    // TODO after user answer with correct sentiment (e.g. lastWord field)
                    sentiments.put(nextWord.get(), SentimentClass.NEUTRAL);
                }
                
                messages.add(nextQuestion.orElse("Tell me more."));
            }
        }
        
        return Results.html().render("messages", messages);
    }
    
    public static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        int x = RND.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }
    
    /**
     * Analysis GET.
     *
     * @return
     * @throws java.io.IOException
     */
    public Result analysis() throws IOException {
        Map<String, Integer> counter = Arrays.asList(Category.values()).stream()
        .collect(Collectors.toMap(
                        cat -> cat.name(),
                        cat -> {
                                try {
                                    return db.loadInt(cat.name(), 0);
                                }
                                catch (IOException ex) {
                                    LOG.error("database access failed.", ex);
                                    return 0;
                                }
                            }));
        AtomicInteger sum = new AtomicInteger();
        
        // arithmetic adjust
        counter.forEach((cat, count) -> {
            counter.put(cat, count < 0
                    ? 0
                    : count);
            if (count > 0) {
                sum.addAndGet(count);
            }
        });
            // no div by zero
        sum.set(sum.get() < 1
                ? 1
                : sum.get());        
        
        return Results.html()
                .render("sum", sum.get())
                .render("words", words)
                .render("counter", counter)
                .render("sentiments", sentiments)
                .render("questionsAnswered", questionsAnswered)
                .render("questions", questions);
    }
    
}
