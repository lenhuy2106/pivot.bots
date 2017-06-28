/*
 *
 */
package controllers;

import controllers.services.DatabaseService;
import controllers.services.LearningService;
import database.Constants;
import database.Template;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * SessionTest.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionTest {

    @Spy
    Random RND;

    @Mock
    DatabaseService dbService;

    @Mock
    LearningService learningService;

    @InjectMocks
    Session session = new Session();

    /**
     * Test of init method, of class Session.
     */
    @Test
    public void testInit() throws Exception {
        session.init();
        verify(dbService).loadUsers();
        verify(dbService).loadBots();
        verify(dbService).loadCurrentUser();
        verify(dbService).loadCurrentBot();
        verify(dbService).loadCategories();
        verify(dbService).loadWordsUsed();
        verify(learningService).getWords();
        verify(learningService).getQuestions();
        assertNotNull(session.getBots());
        assertNotNull(session.getCategories());
        assertNotNull(session.getMessages());
        assertNotNull(session.getQuestions());
        assertNotNull(session.getQuestionsAnswered());
        assertNotNull(session.getUsers());
        assertNotNull(session.getVocab());
        assertNotNull(session.getWords());
        assertNotNull(session.getWordsUsed());
        assertTrue(session.getUsers().contains(Constants.DEFAULT_USER));
        assertTrue(session.getBots().contains(Constants.DEFAULT_BOT));
    }

    /**
     * Test of processMessage method, of class Session.
     */
    @Test
    public void testProcessMessageNoCurrentCategory() throws Exception {
        String sentence = "I love programming and cats.";
        session.init();
        session.getMessages().add(sentence);
        session.setCurrentCategory("testCat");
        session.getQuestions().put("testCat", new HashSet<>());
        session.getQuestionsAnswered().put("testCat", new HashSet<>());
        session.processMessage(sentence, Optional.of(false));
        assertTrue(session.getMessages().contains(sentence));
        assertTrue(session.getMessages().contains(Template.NO_MORE_QUESTIONS));
        session.setCurrentCategory("testCat");
        session.processMessage(sentence, Optional.of(false));
    }

    /**
     * Test of processMessage method, of class Session.
     */
    @Test
    public void testProcessMessagePositive() throws Exception {
        String sentence = "I love programming and cats.";
        session.init();
        session.getMessages().add(sentence);
        session.setCurrentCategory("testCat");
        session.getQuestions().put("testCat", new HashSet<>());
        session.getQuestionsAnswered().put("testCat", new HashSet<>());
        session.processMessage(sentence, Optional.ofNullable(null));
        assertTrue(session.getMessages().contains(sentence));
        assertTrue(session.getMessages().contains(Template.NO_MORE_QUESTIONS));
        verify(dbService).incCount("testCat");
    }

    /**
     * Test of processMessage method, of class Session.
     */
    @Test
    public void testProcessMessageNegative() throws Exception {
        String sentence = "I hate programming and cats.";
        session.init();
        session.getMessages().add(sentence);
        session.setCurrentCategory("testCat");
        session.getQuestions().put("testCat", new HashSet<>());
        session.getQuestionsAnswered().put("testCat", new HashSet<>());
        session.processMessage(sentence, Optional.ofNullable(null));
        assertTrue(session.getMessages().contains(sentence));
        assertTrue(session.getMessages().contains(Template.NO_MORE_QUESTIONS));
        verify(dbService).decCount("testCat");
    }

}
