/*
 * Nhu Huy Le
 */
package models;

import java.util.Random;

/**
 *
 * @author T500
 */
public class Template {
    
    public static final String QUESTION_RETRIEVED = "Do you %s %s?";
    
    private static final Random RND = new Random();
    
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
        "What is your opinion of %s?",
        "Tell me your opinion about %s please.",
    };
    
    public static String random(String[] templates) {
        return templates[RND.nextInt(templates.length)];
    }   
    
}
