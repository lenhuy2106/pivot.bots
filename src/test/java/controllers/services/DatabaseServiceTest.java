package controllers.services;

import database.Constants;
import database.Database;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Nhu Huy Le <mail@huy-le.de>
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseServiceTest {

    @Mock
    Database settingsDB;

    @Mock
    Database userDB;

    @Mock
    Database botDB;

    @InjectMocks
    DatabaseService dbService = new DatabaseService();

    public DatabaseServiceTest() throws IOException {
    }

    @Before
    public void setUp() throws Exception {
        when(settingsDB.loadCollection(anyString(), any())).thenReturn(null);
        when(botDB.loadCollection(anyString(), any())).thenReturn(null);
        when(userDB.loadCollection(anyString(), any())).thenReturn(null);
    }

    /**
     * Test of loadUsers method, of class DatabaseService.
     */
    @Test
    public void testLoadUsers() throws Exception {
        dbService.loadUsers();
        verify(settingsDB).loadCollection(eq(Constants.KEY_USERS), any(Collection.class));
    }

    /**
     * Test of loadBots method, of class DatabaseService.
     */
    @Test
    public void testLoadBots() throws Exception {
        dbService.loadBots();
        verify(settingsDB).loadCollection(eq(Constants.KEY_BOTS), any(Collection.class));
    }

    /**
     * Test of loadCurrentUser method, of class DatabaseService.
     */
    @Test
    public void testLoadCurrentUser() throws Exception {
        dbService.loadCurrentUser();
        verify(settingsDB).loadString(eq(Constants.KEY_CUR_USER), eq(Constants.DEFAULT_USER));
    }

    /**
     * Test of loadCurrentBot method, of class DatabaseService.
     */
    @Test
    public void testLoadCurrentBot() throws Exception {
        dbService.loadCurrentBot();
        verify(settingsDB).loadString(eq(Constants.KEY_CUR_BOT), eq(Constants.DEFAULT_BOT));
    }

    /**
     * Test of loadCategories method, of class DatabaseService.
     */
    @Test
    public void testLoadCategories() throws Exception {
        dbService.loadCategories();
        verify(botDB).loadCollection(eq(Constants.KEY_CATEGORIES), any(Collection.class));
    }

    /**
     * Test of loadWords method, of class DatabaseService.
     */
    @Test
    public void testLoadWords() throws Exception {
        String testCat = "testCat";
        dbService.loadWords(testCat);
        verify(botDB).loadCollection(eq(Constants.KEY_PREFIX_WORDS + testCat), any(Collection.class));
    }

    /**
     * Test of loadQuestions method, of class DatabaseService.
     */
    @Test
    public void testLoadQuestions() throws Exception {
        String testCat = "testCat";
        dbService.loadQuestions(testCat);
        verify(botDB).loadCollection(eq(Constants.KEY_PREFIX_QUESTIONS + testCat), any(Collection.class));
    }

    /**
     * Test of loadQuestionsAnswered method, of class DatabaseService.
     */
    @Test
    public void testLoadQuestionsAnswered() throws Exception {
        String testCat = "testCat";
        dbService.loadQuestionsAnswered(testCat);
        verify(userDB).loadCollection(eq(Constants.KEY_PREFIX_QUESTIONS_ANSWERED + testCat), any(Collection.class));
    }

    /**
     * Test of loadWordsUsed method, of class DatabaseService.
     */
    @Test
    public void testLoadWordsUsed() throws Exception {
        when(userDB.loadCollection(anyString(), any())).thenReturn(new ArrayList<>());
        dbService.loadWordsUsed();
        verify(userDB).loadCollection(eq(Constants.KEY_WORDS_USED), any(Collection.class));
        verify(userDB).loadCollection(eq(Constants.KEY_WORDS_USED_SENTIMENTS), any(Collection.class));
    }

    /**
     * Test of saveCategories method, of class DatabaseService.
     */
    @Test
    public void testSaveCategories() throws Exception {
        Set testColl = new HashSet<>();
        dbService.saveCategories(testColl);
        verify(botDB).saveCollection(Constants.KEY_CATEGORIES, testColl);
    }

    /**
     * Test of saveWords method, of class DatabaseService.
     */
    @Test
    public void testSaveWords() throws Exception {
        Set testColl = new HashSet<>();
        dbService.saveWords("testCat", testColl);
        verify(botDB).saveCollection(Constants.KEY_PREFIX_WORDS + "testCat", testColl);
    }

    /**
     * Test of saveQuestions method, of class DatabaseService.
     */
    @Test
    public void testSaveQuestions() throws Exception {
        Set testColl = new HashSet<>();
        dbService.saveQuestions("testCat", testColl);
        verify(botDB).saveCollection(Constants.KEY_PREFIX_QUESTIONS + "testCat", testColl);
    }

    /**
     * Test of saveQuestionsAnswered method, of class DatabaseService.
     */
    @Test
    public void testSaveQuestionsAnswered() throws Exception {
        Set testColl = new HashSet<>();
        dbService.saveQuestionsAnswered("testCat", testColl);
        verify(userDB).saveCollection(Constants.KEY_PREFIX_QUESTIONS_ANSWERED + "testCat", testColl);
    }

    /**
     * Test of incCount method, of class DatabaseService.
     */
    @Test
    public void testIncCount() throws Exception {
        when(userDB.loadInt(anyString(), anyInt())).thenReturn(0);
        dbService.incCount("testCat");
        verify(userDB).saveInt("testCat", 1);
    }

    /**
     * Test of decCount method, of class DatabaseService.
     */
    @Test
    public void testDecCount() throws Exception {
        when(userDB.loadInt(anyString(), anyInt())).thenReturn(0);
        dbService.decCount("testCat");
        verify(userDB).saveInt("testCat", -1);
    }

    /**
     * Test of saveWordsUsed method, of class DatabaseService.
     */
    @Test
    public void testSaveWordsUsed() throws Exception {
        Map testColl = new HashMap<>();
        dbService.saveWordsUsed(testColl);
        verify(userDB).saveCollection(Constants.KEY_WORDS_USED, testColl.keySet());
        verify(userDB).saveCollection(Constants.KEY_WORDS_USED_SENTIMENTS, testColl.values());
    }

    /**
     * Test of loadCount method, of class DatabaseService.
     */
    @Test
    public void testLoadCount() throws Exception {
        dbService.loadCount("testCat");
        verify(userDB).loadInt(eq("testCat"), anyInt());
    }

    /**
     * Test of saveUsers method, of class DatabaseService.
     */
    @Test
    public void testSaveUsers() throws Exception {
        Set coll = new HashSet();
        dbService.saveUsers(coll);
        verify(settingsDB).saveCollection(Constants.KEY_USERS, coll);
    }

    /**
     * Test of saveBots method, of class DatabaseService.
     */
    @Test
    public void testSaveBots() throws Exception {
        Set coll = new HashSet();
        dbService.saveBots(coll);
        verify(settingsDB).saveCollection(Constants.KEY_BOTS, coll);
    }

    /**
     * Test of saveCurrentUser method, of class DatabaseService.
     */
    @Test
    public void testSaveCurrentUser() throws Exception {
        dbService.saveCurrentUser("testUser");
        verify(settingsDB).saveString(Constants.KEY_CUR_USER, "testUser");
    }

    /**
     * Test of saveCurrentBot method, of class DatabaseService.
     */
    @Test
    public void testSaveCurrentBot() throws Exception {
        dbService.saveCurrentBot("testBot");
        verify(settingsDB).saveString(Constants.KEY_CUR_BOT, "testBot");
    }

}
