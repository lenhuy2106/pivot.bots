package database;

/**
 * Constant class.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public final class Constants {

    private Constants() {
    }

    /**
     * Default bot name.
     */
    public static final String DEFAULT_BOT = "defaultBot";
    /**
     * Default user name.
     */
    public static final String DEFAULT_USER = "defaultUser";
    /**
     * Path to application user and bot data.
     */
    public static final String DATA_PATH = "src/main/java/assets/data/";
    /**
     * Filename for the settings data file.
     */
    public static final String FILENAME_SETTINGS = "settings";

    /**
     * Value delimiter for collections.
     */
    public static final String DELIMITER = "|";

    // Keys used for database access.
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
