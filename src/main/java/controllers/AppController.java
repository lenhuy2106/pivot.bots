package controllers;

import com.google.inject.Inject;
import ninja.Result;
import ninja.Results;

import com.google.inject.Singleton;
import static database.Constants.DEFAULT_BOT;
import static database.Constants.DEFAULT_USER;
import static database.Constants.GENERAL_CATEGORY;
import static database.Constants.NO_MORE_QUESTIONS;
import models.Learning;
import ninja.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import database.PropertyDB;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.SentimentClass;
import static edu.stanford.nlp.simple.SentimentClass.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import database.Template;
import ninja.params.Param;
import ninja.uploads.DiskFileItemProvider;
import ninja.uploads.FileItem;
import ninja.uploads.FileProvider;
import controllers.services.DatabaseService;
import controllers.services.LearningService;
import controllers.services.LearningService.LearningResult;

/**
 * The Application Controller.
 * Contains the logic of the application.
 * @author Nhu Huy Le <mail@huy-le.de>
 * @see <a href="http://www.ninjaframework.org/documentation/basic_concepts/controllers.html"></a>
 */
@FileProvider(DiskFileItemProvider.class)
@Singleton
public class AppController {

    private static final Logger LOG = LoggerFactory.getLogger(AppController.class);

    /**
     * The service providing the learning.
     */
    private final LearningService learningService;

    /**
     * the service providing the database.
     */
    private final DatabaseService dbService;

    /**
     * All the messages of the user's session.
     */
    private final List<String> messages = new ArrayList<>();

    /**
     * Flatted (non-categorized) available words.
     */
    private Set<String> vocab = new HashSet<>();

    /**
     * All users selectable.
     */
    private final Set<String> users;

    /**
     * All bots selectable.
     */
    private final Set<String> bots;

    /**
     * The selected user.
     */
    private String currentUser;

    /**
     * The selected bot.
     */
    private String currentBot;

    /**
     * The randomly chosen category for the current question.
     */
    private String currentCategory;

    /**
     * All categories provided by bot.
     */
    private Set<String> categories = new HashSet<>();

    /**
     * All categorized questions of the bot.
     */
    private final Map<String, Set<String>> questions;

    /**
     * All categorized questions already answered by user.
     */
    private final Map<String, Set<String>> questionsAnswered = new HashMap<>();

    /**
     * All categorized words of the bot.
     */
    private final Map<String, Set<String>> words;

    /**
     * All words used and semantically analyzed.
     */
    private Map<String, SentimentClass> wordsUsed = new HashMap<>();

    /**
     * Random class instance.
     */
    @Inject
    private Random RND;

    /**
     * Ctor.
     * @param learningService Injected learning service.
     * @param dbService Injected database service.
     * @throws IOException If database is not accessable.
     */
    @Inject
    public AppController(
            LearningService learningService,
            DatabaseService dbService) throws IOException {
        this.learningService = learningService;
        this.dbService = dbService;
        users = dbService.loadUsers();
        bots  = dbService.loadBots();
        currentUser = dbService.loadCurrentUser();
        currentBot  = dbService.loadCurrentBot();
        words = learningService.getWords();
        questions = learningService.getQuestions();

        init();
    }

    /**
     * Index GET.
     * @return website.
     */
    public Result index() {
        return Results.html()
                .render("currentUser", currentUser);
    }

    /**
     * Learning GET.
     * @return website.
     */
    public Result learning() {
        return Results.html()
                .render("currentUser", currentUser)
                .render("categories", categories);
    }

    /**
     * Learning execution POST.
     * @param context Injected session context.
     * @param learning Injected/parsed learning object.
     * @return website.
     * @throws java.io.IOException If database is not accessable.
     */
    public Result learningStart(Context context, Learning learning) throws IOException {
        LOG.info("learning with: {}", learning);
        categories.add(learning.getCategory());
        dbService.saveCategories(categories);
        init();

        LearningResult learningResult = learningService.extract(learning);
        LOG.info("questions extracted: " + questions.toString());

        // save to botDB
        dbService.saveWords(
                learning.getCategory(),
                learningResult.getWordsToTrain());
        dbService.saveQuestions(
                learning.getCategory(),
                learningResult.getQuestionsToTrain());
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
     * @param context Injected session context.
     * @param messageOpt Optional message answer.
     * @param skip If question wasn't understood.
     * @return website.
     * @throws java.io.IOException If database is not accessable.
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
                    .render("currentBot", currentBot)
                    .render("vocab", vocab)
                    .render("messages", messages);
        }

        messages.add(text);

        // message is relevant answer
        if (currentCategory != null) {
            // flag question
            String prevQuestion = messages.get(messages.size() - 2);
            Set<String> categorized = questionsAnswered.get(currentCategory);
            categorized.add(prevQuestion);
            questions.get(currentCategory).remove(prevQuestion);
            dbService.saveQuestionsAnswered(currentCategory, categorized);

            if (!skip.isPresent()) {
                Document doc = new Document(text);
                AtomicInteger avg = new AtomicInteger();
                for (Sentence sentence : doc.sentences()) {
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

                    // whole statement sentiment counter
                    if (avg.get() >= 0) {
                        dbService.incCount(currentCategory);
                    }
                    else {
                        dbService.decCount(currentCategory);
                    }
                    // per sentence (word-specific) sentiment counter
                    countSentiment(sentence);
                }
            }
        }

        messages.add(nextQuestion().orElse(NO_MORE_QUESTIONS));

        return Results.html()
                .render("currentUser", currentUser)
                .render("currentBot", currentBot)
                .render("vocab", vocab)
                .render("messages", messages);
    }
    /**
     * Analysis GET.
     * @return website.
     * @throws java.io.IOException If database is not accessable.
     */
    public Result analysis() throws IOException {
        Map<String, Integer> counter = categories.stream()
                .collect(Collectors.toMap(cat -> cat,
                        cat -> {
                            try {
                                return dbService.loadCount(cat);
                            }
                            catch (IOException ex) {
                                LOG.error("database access failed.", ex);
                                return 0;
                            }
                        }));
        AtomicInteger sum = new AtomicInteger();

        // bot potential
        long botPotential
                = words.values().stream()
                        .flatMap(s -> s.stream())
                        .count()
                + questions.values().stream()
                        .flatMap(s -> s.stream())
                        .count();
        long usedPotential
                = wordsUsed.size()
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

    /**
     * Settings GET.
     * @param ctx Session context.
     * @param upUser Optional user to import.
     * @param upBot Optional bot to import.
     * @return website.
     * @throws IOException If database is not accessable.
     */
    public Result settings(
            Context ctx,
            @Param("upUser") Optional<FileItem> upUser,
            @Param("upBot") Optional<FileItem> upBot) throws IOException {

        String changeUser = ctx.getParameter("currentUser");
        String changeBot = ctx.getParameter("currentBot");

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
            words.clear();
            wordsUsed.clear();
            questionsAnswered.clear();
            currentCategory = null;

            currentBot = StringUtil.isBlank(changeBot)
                    ? currentBot
                    : changeBot;
            vocab.clear();
            questions.clear();

            try {
                dbService.saveCurrentUser(currentUser);
                dbService.saveCurrentBot(currentBot);

                users.add(currentUser);
                bots.add(currentBot);

                dbService.saveUsers(users);
                dbService.saveBots(bots);

                init();
            } catch (IOException ex) {
                LOG.error("Failed to reinitialize App.", ex);
            }
        }

        return Results.html()
                .render("users", users)
                .render("bots", bots)
                .render("currentUser", currentUser)
                .render("currentBot", currentBot);
    }

    /**
     * (Re)initializes current bot and user.
     * @throws IOException
     */
    private void init() throws IOException {
        // load from persistent userDB
        // words and questions for each category (e.g. front- or backend)
        categories = dbService.loadCategories();
        for (String category : categories) {
            words.put(category, dbService.loadWords(category));
            questions.put(category, dbService.loadQuestions(category));
            questionsAnswered.put(category, dbService.loadQuestionsAnswered(category));
        }
        // sentiment analysis
        wordsUsed = dbService.loadWordsUsed();

        vocab = words.values().stream()
                .flatMap(w -> w.stream())
                .collect(Collectors.toSet());
        messages.clear();
        messages.add(String.format(
                "Hello %s! I want to ask you some questions.",
                currentUser));
        // always available
        users.add(DEFAULT_USER);
        bots.add(DEFAULT_BOT);
    }

    /**
     * Provides the next question of random category for the dialogue if possible.
     * Either
     *  1. retrieves an extracted question or
     *  2. generates a new question from template and keyword.
     * @return Optional next question.
     */
    private Optional<String> nextQuestion() {
        // random category
        Optional<String> nextQuestion;
        AtomicInteger filtered = new AtomicInteger(0);
        currentCategory = categories.stream()
                .skip((int) (categories.size() * Math.random()))
                .findFirst()
                .orElse(GENERAL_CATEGORY);
            // retrieve question
        if (RND.nextBoolean()) {
            Set<String> subquestions = questions.get(currentCategory);
            nextQuestion = subquestions.stream()
                    .filter(w -> !questionsAnswered.get(currentCategory).contains(w))
                    .peek(w -> filtered.incrementAndGet())
                    .skip((int) (filtered.get() * Math.random()))
                    .findFirst();
        }
            // generate question
        else {
            Set<String> subwords = words.get(currentCategory);
            String nextWord = subwords.stream()
                    // only unused words
                    .filter(w -> !wordsUsed.keySet().contains(w))
                    .peek(w -> filtered.incrementAndGet())
                    .skip((int) (filtered.get() * Math.random()))
                    .findFirst()
                    .orElse(currentCategory);
            nextQuestion = Optional.of(String.format(
                    Template.random(Template.QUESTION_GENERATED), nextWord));
            wordsUsed.put(nextWord, SentimentClass.NEUTRAL);
        }
        return nextQuestion;
    }

    /**
     * Semantically process a sentence for the analysis.
     * @param sentence to process.
     * @throws IOException If database is not accessable.
     */
    private void countSentiment(Sentence sentence) throws IOException {
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
                                dbService.incCount(currentCategory);
                                break;
                            case VERY_NEGATIVE:
                            case NEGATIVE:
                                dbService.decCount(currentCategory);
                                break;
                            case NEUTRAL:
                            default:
                        }
                        dbService.saveWordsUsed(wordsUsed);
                    }
                    catch (IOException ex) {
                        LOG.error("db access failed.", ex);
                    }
                    LOG.info("message sentiment: {} [{}]",
                            recognized,
                            sentence.sentiment());
                });
    }

}
