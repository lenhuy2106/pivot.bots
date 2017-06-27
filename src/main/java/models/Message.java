/*
 * Nhu Huy Le <mail@huy-le.de>
 */
package models;

/**
 * Message POJO for the dialogue.
 * Parsed from http post request.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class Message {

    /**
     * message content.
     */
    private String text;

    public String getText() {
        return text;
    }

    public void setText(
            final String text) {
        this.text = text;
    }

}
