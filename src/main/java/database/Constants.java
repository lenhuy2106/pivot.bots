package database;

/**
 * Constant class.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public final class Constants {

    private Constants() {
    }

    public static final String NO_CATEGORY = "General";
    public static final String DEFAULT_BOT = "defaultBot";
    public static final String DEFAULT_USER = "defaultUser";
    public static final String DATA_PATH = "src/main/java/assets/data/";
    public static final String FILENAME_SETTINGS = "settings";

    /**
     * Value delimiter for collections.
     */
    public static final String DELIMITER = "|";

    public static final String KEY_USERS = "users";
    public static final String KEY_BOTS = "bots";
    public static final String KEY_CUR_USER = "currentUser";
    public static final String KEY_CUR_BOT = "currentBot";
    public static final String KEY_CATEGORIES = "categories";
    public static final String KEY_PREFIX_WORDS = "words.";
    public static final String KEY_PREFIX_QUESTIONS = "questions.";
    public static final String KEY_PREFIX_QUESTIONS_ANSWERED = "questions.answered.";
    public static final String KEY_WORDS_USED = "words.used";
    public static final String KEY_WORDS_USED_SENTIMENTS = "words.sentiment";
    
}
