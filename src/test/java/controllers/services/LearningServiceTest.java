/*
 * Nhu Huy Le
 */
package controllers.services;

import java.util.HashSet;
import java.util.Set;
import models.Learning;
import org.hamcrest.Matchers;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Nhu Huy Le <mail@huy-le.de>
 */
@RunWith(MockitoJUnitRunner.class)
public class LearningServiceTest {

    LearningService learningService = new LearningService();

    @Before
    public void setUp() {
        learningService.getQuestions().put("testCat", new HashSet<>());
        learningService.getWords().put("testCat", new HashSet<>());
   }

    /**
     * Test of extract method, of class LearningService.
     */
    @Test
    public void testExtract() {
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
