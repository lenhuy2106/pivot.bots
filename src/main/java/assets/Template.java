/*
 * Nhu Huy Le
 */
package assets;

import java.util.Random;

/**
 *
 * @author T500
 */
public class Template {
    
    public static final String RETRIEVAL = "Do you %s %s?";
    
    private static final Random RND = new Random();
    
    private static final String[] GENERATE = {
        "Tell me what you know about %s.",
        "What do you think of %s?",
        "Tell me something about %s.",
        "And %s?",
        "What about %s?",
        "Do you remember about %s?",
        "Finish this for me: %s is...",
        "Describe %s.",
        "Explain %s.",
        "Do you know about %s?",
    };    
    
    public static String generate() {
        return GENERATE[RND.nextInt(GENERATE.length)];
    }    
    
}
