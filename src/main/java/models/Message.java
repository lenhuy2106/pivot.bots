/*
 * Nhu Huy Le
 */
package models;

/**
 *
 * @author T500
 */
public class Message {
    
    private String text;
    
    private boolean dontUnderstand = false;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isDontUnderstand() {
        return dontUnderstand;
    }

    public void setDontUnderstand(boolean dontUnderstand) {
        this.dontUnderstand = dontUnderstand;
    }    
    
}
