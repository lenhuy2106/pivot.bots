/*
 * Nhu Huy Le <mail@huy-le.de>
 */
package controllers.services;

import static database.Constants.*;
import database.Database;
import database.PropertyDB;
import edu.stanford.nlp.simple.SentimentClass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The service abstraction for the database access.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public final class DatabaseService {

    /**
     * settings database.
     */
    private final Database settingsDB;

    /**
     * user database.
     */
    private Database userDB;

    /**
     * bot database.
     */
    private Database botDB;

    public DatabaseService() throws IOException {
        this.settingsDB  = new PropertyDB(FILENAME_SETTINGS);
        this.userDB      = new PropertyDB(loadCurrentUser());
        this.botDB       = new PropertyDB(loadCurrentBot());
    }

    public Database getSettingsDB() {
        return settingsDB;
    }

    public Database getUserDB() {
        return userDB;
    }

    public Database getBotDB() {
        return botDB;
    }

    public Set<String> loadUsers() throws IOException {
        return settingsDB.loadCollection(KEY_USERS, new HashSet<>());
    }

    public Set<String> loadBots() throws IOException {
        return settingsDB.loadCollection(KEY_BOTS, new HashSet<>());
    }

    public String loadCurrentUser() throws IOException {
        return settingsDB.loadString(KEY_CUR_USER, DEFAULT_USER);
    }

    public String loadCurrentBot() throws IOException {
        return settingsDB.loadString(KEY_CUR_BOT, DEFAULT_BOT);
    }

    public Set<String> loadCategories() throws IOException {
        return botDB.loadCollection(KEY_CATEGORIES, new HashSet<>());
    }

    public Set<String> loadWords(String category) throws IOException {
        return botDB.loadCollection(KEY_PREFIX_WORDS + category, new HashSet<>());
    }

    public Set<String> loadQuestions(String category) throws IOException {
        return botDB.loadCollection(KEY_PREFIX_QUESTIONS + category, new HashSet<>());
    }

    public Set<String> loadQuestionsAnswered(String category) throws IOException {
        return userDB.loadCollection(KEY_PREFIX_QUESTIONS_ANSWERED + category, new HashSet<>());
    }

    public Map<String, SentimentClass> loadWordsUsed() throws IOException {
        Map<String, SentimentClass> wordsUsed = new HashMap<>();
        List<String> sentimentKeys = userDB.loadCollection(KEY_WORDS_USED, new ArrayList<>());
        List<String> sentimentVals = userDB.loadCollection(KEY_WORDS_USED_SENTIMENTS, new ArrayList<>());
        for (int i = 0; i < sentimentKeys.size(); i++) {
            wordsUsed.put(
                    sentimentKeys.get(i),
                    SentimentClass.valueOf(sentimentVals.get(i)));
        }
        return wordsUsed;
    }

    public void saveCategories(Set<String> categories) throws IOException {
        botDB.saveCollection(KEY_CATEGORIES, categories);
    }

    public void saveWords(String category, Set wordsToTrain) throws IOException {
        botDB.saveCollection(
                KEY_PREFIX_WORDS + category,
                wordsToTrain);
    }
    public void saveQuestions(String category, Set questionsToTrain) throws IOException {
        botDB.saveCollection(
                KEY_PREFIX_QUESTIONS + category,
                questionsToTrain);
    }

    public void saveQuestionsAnswered(String currentCategory, Set<String> questionsAnswered) throws IOException {
        userDB.saveCollection(KEY_PREFIX_QUESTIONS_ANSWERED + currentCategory, questionsAnswered);
    }

    public void incCount(String currentCategory) throws IOException {
        userDB.saveInt(currentCategory, userDB.loadInt(currentCategory, 0) + 1);
    }

    public void decCount(String currentCategory) throws IOException {
        userDB.saveInt(currentCategory, userDB.loadInt(currentCategory, 0) - 1);
    }

    public void saveWordsUsed(Map<String, SentimentClass> wordsUsed) throws IOException {
        userDB.saveCollection(KEY_WORDS_USED, wordsUsed.keySet());
        userDB.saveCollection(KEY_WORDS_USED_SENTIMENTS, wordsUsed.values());
    }

    public int loadCount(String category) throws IOException {
        return userDB.loadInt(category, 0);
    }

    public void saveUsers(Set<String> users) throws IOException {
        settingsDB.saveCollection(KEY_USERS, users);
    }

    public void saveBots(Set<String> bots) throws IOException {
        settingsDB.saveCollection(KEY_BOTS, bots);
    }

    public void saveCurrentUser(String user) throws IOException {
        settingsDB.saveString(KEY_CUR_USER, user);
        userDB = new PropertyDB(user);
    }

    public void saveCurrentBot(String bot) throws IOException {
        settingsDB.saveString(KEY_CUR_BOT, bot);
        botDB = new PropertyDB(bot);
    }

}
