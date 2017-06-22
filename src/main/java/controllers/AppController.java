
package controllers;

import ninja.Result;
import ninja.Results;

import com.google.inject.Singleton;
import models.Learning;
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
import models.Template;
import ninja.params.Param;
import ninja.uploads.DiskFileItemProvider;
import ninja.uploads.FileItem;
import ninja.uploads.FileProvider;

@FileProvider(DiskFileItemProvider.class)
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
    
    private final Map<String, SentimentClass> wordsUsed = new HashMap<>();
    
    private final Map<String, Set<String>> questions = new HashMap<>();
    
    private final Map<String, Set<String>> questionsAnswered = new HashMap<>();
    
    private final List<String> messages = new ArrayList<>();
    
    private final Database settingsDB = new PropertyDB("settings");
    
    private final Set<String> users = settingsDB.loadCollection("users", new HashSet<>());

    private final Set<String> bots = settingsDB.loadCollection("bots", new HashSet<>());
    
    private String currentUser = settingsDB.loadString("currentUser", "defaultUser");
    
    private String currentBot = settingsDB.loadString("currentBot", "defaultBot");
    
    private Set<String> vocab = new HashSet<>();
    
    private Category currentCat;
    
    private Database userDB = new PropertyDB(currentUser);
    
    private Database botDB  = new PropertyDB(currentBot);
    
    public AppController() throws IOException {
        init();
    }

    private void init() throws IOException {
        // load from persistent userDB
            // words and questions for each category (e.g. front- or backend)
        for (Category category : Category.values()) {
            words.put(category.name(), botDB.loadCollection("words." + category.name(), new HashSet<>()));
            questions.put(category.name(), botDB.loadCollection("questions." + category.name(), new HashSet<>()));
            questionsAnswered.put(category.name(), userDB.loadCollection("questions.answered." + category.name(), new HashSet<>()));
        }
            // sentiment analysis
        List<String> sentimentKeys = userDB.loadCollection("words.used", new ArrayList<>());
        List<String> sentimentVals = userDB.loadCollection("words.used.sentiment", new ArrayList<>());   
        for (int i = 0; i < sentimentKeys.size(); i++) {
            wordsUsed.put(
                    sentimentKeys.get(i),
                    SentimentClass.valueOf(sentimentVals.get(i)));
        }
        
        vocab = words.values().stream()
                .flatMap(w -> w.stream())
                .collect(Collectors.toSet());
        messages.add(String.format(
                "Hello %s! I want to ask you some questions.",
                currentUser));
        users.add("defaultUser");
        bots.add("defaultBot");
    }
    
    /**
     * Index GET.
     * @return 
     */
    public Result index() {
        return Results.html()
                .render("currentUser", currentUser);
    }
    
    /**
     * Learning GET.
     *
     * @return
     */
    public Result learning() {
        return Results.html()
                .render("currentUser", currentUser)
                .render("categories", Category.values());
    }
    
    /**
     * Learning execution POST.
     *
     * @param context
     * @param learning
     * @return
     * @throws java.io.IOException
     */
    public Result learningStart(Context context, Learning learning) throws IOException {
        LOG.info("learning with: {}", learning);
        Document corpus      = new Document(learning.getCorpus());
        Set wordsToTrain     = words.get(learning.getCategory().name());
        Set questionsToTrain = questions.get(learning.getCategory().name());
        
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
        
        LOG.info("questions extracted: " + questions.toString());
        
        // save to botDB
        botDB.saveCollection("words." + learning.getCategory(), wordsToTrain);
        botDB.saveCollection("questions." + learning.getCategory(), questionsToTrain);
        // update vocab
        vocab = words.values().stream()
                .flatMap(w -> w.stream())
                .collect(Collectors.toSet());
        return Results.html()
                .render("currentUser", currentUser)
                .render(learning);
    }
    
    /**
     * Dialogue GET/POST.
     *
     * @param context
     * @param messageOpt
     * @param skip
     * @return
     * @throws java.io.IOException
     */
    public Result dialogue(
            Context context,
            Optional<Message> messageOpt,
            @Param("skip") Optional<Boolean> skip) throws IOException {
        
        String text = messageOpt.isPresent()
                ? messageOpt.get().getText()
                : "";
        
        // there must be a user message
        if (StringUtil.isBlank(text)) {
            return Results.html()
                    .render("currentUser", currentUser)
                    .render("messages", messages);
        }
        
        messages.add(text);
        
        // message is relevant answer
        if (currentCat != null) {
            // flag question
            String prevQuestion = messages.get(messages.size() - 2);
            Set<String> categorized = questionsAnswered.get(currentCat.name());
            categorized.add(prevQuestion);
            questions.get(currentCat.name()).remove(prevQuestion);
            userDB.saveCollection("questions.answered." + currentCat, categorized);
            
            if (!skip.isPresent()) {
                Document doc = new Document(text);
                AtomicInteger avg = new AtomicInteger();
                doc.sentences().forEach(sentence -> {
                    // get overall msg sentiment
                    switch (sentence.sentiment()) {
                        case VERY_POSITIVE:
                        case POSITIVE:
                            avg.incrementAndGet();
                            break;
                        case VERY_NEGATIVE:
                        case NEGATIVE:
                            avg.decrementAndGet();
                            break;
                        case NEUTRAL:
                        default:
                    }

                    // general sentiment counter
                    int delta = avg.get() >= 0
                            ? 1
                            : -1;
                    try {
                        userDB.saveInt(currentCat.name(), userDB.loadInt(currentCat.name(), 0) + delta);
                    } catch (IOException ex) {
                        LOG.error("db access failed.", ex);
                    }

                    // word specific sentiment counter
                    sentence.words().stream()
                            .map(String::toLowerCase)
                            .filter(word -> vocab.contains(word))
                            .forEach(recognized -> {
                                // save sentiment of word
                                wordsUsed.put(recognized, sentence.sentiment());
                                try {
                                    // general sentiment
                                    switch (sentence.sentiment()) {
                                        case VERY_POSITIVE:
                                        case POSITIVE:
                                            userDB.saveInt(currentCat.name(), userDB.loadInt(currentCat.name(), 0) + 1);
                                            break;
                                        case VERY_NEGATIVE:
                                        case NEGATIVE:
                                            userDB.saveInt(currentCat.name(), userDB.loadInt(currentCat.name(), 0) - 1);
                                            break;
                                        case NEUTRAL:
                                        default:
                                    }
                                    userDB.saveCollection("words.used", wordsUsed.keySet());
                                    userDB.saveCollection("words.used.sentiment", wordsUsed.values());
                                } catch (IOException ex) {
                                    LOG.error("db access failed.", ex);
                                }
                                LOG.info("message sentiment: {} [{}]",
                                        recognized,
                                        sentence.sentiment());
                            });
                });
            }
        }
        
        // next random question
        currentCat = randomEnum(Category.class);
        Optional<String> nextQuestion;
        AtomicInteger filtered = new AtomicInteger(-1);
            // retrieve question
        if (RND.nextBoolean()) {
            Set<String> subquestions = questions.get(currentCat.name());
            nextQuestion = subquestions.stream()
                    .filter(w -> !questionsAnswered.get(currentCat.name()).contains(w))
                    .peek(w -> filtered.incrementAndGet())
                    .skip((int) (filtered.get() * Math.random()))
                    .findFirst();
        }
            // generate question
        else {
            Set<String> subwords = words.get(currentCat.name());
            String nextWord = subwords.stream()
                    // only unused words
                    .filter(w -> !wordsUsed.keySet().contains(w))
                    .peek(w -> filtered.incrementAndGet())
                    .skip((int) (filtered.get() * Math.random()))
                    .findFirst()
                    .orElse(currentCat.name());
            nextQuestion = Optional.ofNullable(String.format(
                    Template.random(Template.QUESTION_GENERATED), nextWord));
            // TODO after user answer with correct sentiment (e.g. lastWord field)
            wordsUsed.put(nextWord, SentimentClass.NEUTRAL);
        }
        
        messages.add(nextQuestion.orElse("Tell me more."));
        
        return Results.html()
                .render("currentUser", currentUser)
                .render("messages", messages);
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
        .collect(Collectors.toMap(cat -> cat.name(),
                        cat -> {
                                try {
                                    return userDB.loadInt(cat.name(), 0);
                                }
                                catch (IOException ex) {
                                    LOG.error("database access failed.", ex);
                                    return 0;
                                }
                            }));
        AtomicInteger sum = new AtomicInteger();
        
        // bot potential
        long botPotential = 
                words.values().stream()
                        .flatMap(s -> s.stream())
                        .count()
                + questions.values().stream()
                        .flatMap(s -> s.stream())
                        .count();
        long usedPotential =
                wordsUsed.size()
                + questionsAnswered.values().stream()
                        .flatMap(s -> s.stream())
                        .count();
        
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
        botPotential = botPotential == 0
                ? 1
                : botPotential;
        sum.set(sum.get() < 1
                ? 1
                : sum.get());
        usedPotential = usedPotential > botPotential
                ? botPotential
                : usedPotential;
        
        return Results.html()
                .render("currentUser", currentUser)
                .render("sum", sum.get())
                .render("words", words)
                .render("counter", counter)
                .render("wordsUsed", wordsUsed)
                .render("questionsAnswered", questionsAnswered)
                .render("questions", questions)
                .render("botPotential", botPotential)
                .render("usedPotential", usedPotential);
    }
    
    public Result settings(
            Context ctx,
            @Param("upUser") Optional<FileItem> upUser,
            @Param("upBot") Optional<FileItem> upBot) throws IOException {        
        
        String changeUser = ctx.getParameter("currentUser");
        String changeBot  = ctx.getParameter("currentBot");        
        
        upUser.ifPresent((userItem) -> {
            LOG.info("uploading new user...");
            users.add(PropertyDB.importFile(userItem).orElse(currentUser));
        });
        upBot.ifPresent((botItem) -> {
            LOG.info("uploading new bot...");
            bots.add(PropertyDB.importFile(botItem).orElse(currentBot));
        });
        
        if (StringUtil.isNotBlank(changeUser) || StringUtil.isNotBlank(changeBot)) { 
           // reset settings
            currentUser = StringUtil.isBlank(changeUser)
                    ? currentUser
                    : changeUser;
            messages.clear();
            words.clear();
            wordsUsed.clear();
            questionsAnswered.clear();
            currentCat = null;

            currentBot = StringUtil.isBlank(changeBot)
                    ? currentBot
                    : changeBot;
            vocab.clear();
            questions.clear();
            
            try {
                userDB = new PropertyDB(currentUser);
                botDB  = new PropertyDB(currentBot);
                
                users.add(currentUser);
                bots.add(currentBot);
                
                settingsDB.saveCollection("users", users);
                settingsDB.saveCollection("bots", bots);

                init();
            }
            catch (IOException ex) {
                LOG.error("Failed to reinitialize App.", ex);
            }
        }
        
        return Results.html()
                .render("users", users)
                .render("bots", bots)
                .render("currentUser", currentUser)
                .render("currentBot", currentBot);
    }

   
}
