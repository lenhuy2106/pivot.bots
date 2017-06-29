/*
 * Nhu Huy Le
 */
package controllers;

import com.google.inject.Inject;
import controllers.services.DatabaseService;
import controllers.services.LearningService;
import static database.Constants.DEFAULT_BOT;
import static database.Constants.DEFAULT_USER;
import database.PropertyDB;
import database.Template;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.SentimentClass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import ninja.uploads.FileItem;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls state for the user's current session.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class Session {

    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    /**
     * Random class instance.
     */
    @Inject
    private Random RND;

    @Inject
    private DatabaseService dbService;

    @Inject
    private LearningService learningService;

    /**
     * All the messages of the user's session.
     */
    private List<String> messages = new ArrayList<>();

    /**
     * Flatted (non-categorized) available words.
     */
    private Set<String> vocab = new HashSet<>();

    /**
     * All users selectable.
     */
    private Set<String> users;

    /**
     * All bots selectable.
     */
    private Set<String> bots;

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
    private Map<String, Set<String>> questions;

    /**
     * All categorized questions already answered by user.
     */
    private Map<String, Set<String>> questionsAnswered = new HashMap<>();

    /**
     * All categorized words of the bot.
     */
    private Map<String, Set<String>> words;

    /**
     * All words used and semantically analyzed.
     */
    private Map<String, SentimentClass> wordsUsed = new HashMap<>();

    public Session() {
    }

    /**
     * (Re)initializes session.
     * Clear State and reload data from database.
     * @throws IOException If Database is noch accessable.
     */
    public void init() throws IOException {
        users       = dbService.loadUsers();
        bots        = dbService.loadBots();
        currentUser = dbService.loadCurrentUser();
        currentBot  = dbService.loadCurrentBot();
        categories  = dbService.loadCategories();
        words       = learningService.getWords();
        questions   = learningService.getQuestions();
        // words and questions for each category
        for (String category : categories) {
            words.put(category, dbService.loadWords(category));
            questionsAnswered.put(category, dbService.loadQuestionsAnswered(category));
            questions.put(category, dbService.loadQuestions(category).stream()
                    .filter(q -> !questionsAnswered.get(category).contains(q))
                    .collect(Collectors.toSet()));
        }
        wordsUsed = dbService.loadWordsUsed();
        vocab = words.values().stream()
                .flatMap(w -> w.stream())
                .collect(Collectors.toSet());
        messages.clear();
        messages.add(String.format(
                Template.FORM_GREETING,
                currentUser));
        // always available
        users.add(DEFAULT_USER);
        bots.add(DEFAULT_BOT);
    }

    /**
     * Synchronizes the vocab with the categorized words.
     */
    public void updateVocab() {
        vocab = words.values().stream()
                .flatMap(w -> w.stream())
                .collect(Collectors.toSet());
    }

    /**
     * Processes a whole message.
     * @param text The message to be processed.
     * @param skip If last question wasn't understood.
     * @throws IOException If database is not accessable.
     */
    public void processMessage(String text, Optional<Boolean> skip) throws IOException {
        if (currentCategory != null) {
            // message is relevant answer
            String prevQuestion = messages.get(messages.size() - 1);
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
        messages.add(text);
        // random category
        Optional<String> nextCategory = categories.stream()
                .skip((int) ((categories.size()) * Math.random()))
                .findFirst();
        currentCategory = nextCategory.orElse(null);
        messages.add(nextCategory.isPresent()
                ? nextQuestion(nextCategory.get()).orElse(Template.NO_MORE_QUESTIONS)
                : Template.NO_MORE_QUESTIONS);
    }

    /**
     * Provides the next question of s category for the dialogue if
     * possible.
     * Either
     *  1. retrieves an extracted question or
     *  2. generates a new question from template and keyword.
     * @param category The category for which the question should be processed.
     * @return Optional next question.
     */
    private Optional<String> nextQuestion(String category) {
        // random category
        Optional<String> nextQuestion;
        if (RND.nextBoolean() && !questions.get(category).isEmpty()) {
            LOG.info("retrieve question.");
            Set<String> subquestions = questions.get(category);
            // prefiltered(questions left)
            nextQuestion = subquestions.stream()
                    .skip((int) (subquestions.size() * Math.random()))
                    .findFirst();
        }
        else {
            LOG.info("generate question.");
            Set<String> subwords = words.get(category);
            // filter-on-demand(all words)
            Set<String> filtered = subwords.stream()
                    .filter(w -> !wordsUsed.keySet().contains(w))
                    .collect(Collectors.toSet());
            String nextWord = filtered.stream()
                    .skip((int) (filtered.size() * Math.random()))
                    .findFirst()
                    .orElse(category);
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
                    }
                    catch (IOException ex) {
                        LOG.error("db access failed.", ex);
                    }
                    LOG.info("message sentiment: {} [{}]",
                            recognized,
                            sentence.sentiment());
                });
        dbService.saveWordsUsed(wordsUsed);
    }

    /**
     * Mandatory changes to currently chosen user and bot.
     * Changes by uploading db files is made possible.
     * @param upUser Uploaded user file.
     * @param upBot Uploaded bot file.
     * @param changeUser next user chosen.
     * @param changeBot next bot chosen.
     */
    public void updateWith(
            Optional<FileItem> upUser,
            Optional<FileItem> upBot,
            String changeUser,
            String changeBot) {
        upUser.ifPresent((userItem) -> {
            LOG.info("uploading new user...");
            users.add(PropertyDB.importFile(userItem).orElse(currentUser));
        });
        upBot.ifPresent((botItem) -> {
            LOG.info("uploading new bot...");
            bots.add(PropertyDB.importFile(botItem).orElse(currentBot));
        });

        if (StringUtil.isNotBlank(changeUser) || StringUtil.isNotBlank(changeBot)) {
            reset(changeUser, changeBot);
        }
    }

    private void reset(String changeUser, String changeBot) {
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
        }
        catch (IOException ex) {
            LOG.error("Failed to reinitialize App.", ex);
        }
    }

    public DatabaseService getDbService() {
        return dbService;
    }

    public LearningService getLearningService() {
        return learningService;
    }

    public List<String> getMessages() {
        return messages;
    }

    public Set<String> getVocab() {
        return vocab;
    }

    public Set<String> getUsers() {
        return users;
    }

    public Set<String> getBots() {
        return bots;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public String getCurrentBot() {
        return currentBot;
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public Map<String, Set<String>> getQuestions() {
        return questions;
    }

    public Map<String, Set<String>> getQuestionsAnswered() {
        return questionsAnswered;
    }

    public Map<String, Set<String>> getWords() {
        return words;
    }

    public Map<String, SentimentClass> getWordsUsed() {
        return wordsUsed;
    }

    public void setCurrentCategory(String currentCategory) {
        this.currentCategory = currentCategory;
    }

}
