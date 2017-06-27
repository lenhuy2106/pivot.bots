/*
 * Nhu Huy Le <mail@huy-le.de>
 */
package models;

/**
 * Settings POJO.
 * Parsed from http post requests. Is saved in the database.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class Settings {

    /**
     * Selected user.
     */
    private String currentUser;

    /**
     * Selected bot.
     */
    private String currentBot;

    public Settings() {
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public String getCurrentBot() {
        return currentBot;
    }

    public void setCurrentBot(String currentBot) {
        this.currentBot = currentBot;
    }

    @Override
    public String toString() {
        return "Settings{" + "currentUser=" + currentUser + ", currentBot=" + currentBot + '}';
    }

}
