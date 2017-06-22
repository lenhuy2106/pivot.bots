/*
 * Nhu Huy Le
 */
package models;

/**
 *
 * @author T500
 */
public class Settings {
    
    private String currentUser;
    
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
