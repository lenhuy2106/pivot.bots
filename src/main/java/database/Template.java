package database;

import java.util.Random;

/**
 *
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class Template {

    /**
     * Retrieved question template.
     * Format is "Do you [lemmatized verb] [object]?"
     */
    public static final String QUESTION_RETRIEVED = "Do you %s %s?";

    /**
     * Static random class instance.
     */
    private static final Random RND = new Random();

    /**
     * Generate question templates.
     * Format is "...[keyword]."
     */
    public static final String[] QUESTION_GENERATED = {
        "Tell me what you know about %s.",
        "What do you think of %s?",
        "Tell me something about %s.",
        "Do you remember about %s?",
        "Finish this for me: %s is...",
        "Please describe %s.",
        "Explain to me the term %s.",
        "Do you know about %s?",
        "Do you work with %s?",
        "Do you have experience with %s?",
        "What is your opinion on %s?",
        "Tell me your opinion about %s please.",
    };

    /**
     * Bot greeting when accessing dialogue.
     */
    public static final String FORM_GREETING = "Hello %s! I want to ask you some questions.";

    /**
     * Message, if there are no more questions retrievable or generatable.
     * The statement may ask for further interaction nevertheless.
     */
    public static final String NO_MORE_QUESTIONS = "Tell me more.";

    /**
     * Selects a template randomly.
     * @param templates Template array to randomly access.
     * @return random template.
     */
    public static String random(String[] templates) {
        return templates[RND.nextInt(templates.length)];
    }

}
