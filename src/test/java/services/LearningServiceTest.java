/*
 * Nhu Huy Le
 */
package services;

import controllers.services.LearningService;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import models.Learning;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;
import org.mockito.Spy;

/**
 *
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class LearningServiceTest {

    @Spy
    Map<String, Set<String>> questions;

    @Spy
    Map<String, Set<String>> words;

    @InjectMocks
    LearningService learningService = new LearningService();

    public LearningServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        learningService.getQuestions().put("testCat", new HashSet<>());
        learningService.getWords().put("testCat", new HashSet<>());
   }

    @After
    public void tearDown() {
    }

    /**
     * Test of extract method, of class LearningService.
     */
    @Test
    public void testExtract() {
        System.out.println("extract");

        Learning learning = new Learning();
        learning.setCategory("testCat");
        learning.setCorpus("i love programming and cats.");
        LearningService.LearningResult result = learningService.extract(learning);

        Set<String> questionsToTrain = result.getQuestionsToTrain();
        Set<String> wordsToTrain = result.getWordsToTrain();

        assertThat(questionsToTrain, Matchers.contains("Do you love programming and cats?"));
        assertThat(wordsToTrain, Matchers.containsInAnyOrder("programming", "cats"));
    }

}
