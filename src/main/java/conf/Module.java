package conf;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.util.Random;

/**
 * Internal module/dependency injection class.
 * Start point for dependencies in the controller classes.
 * @author Nhu Huy Le <mail@huy-le.de>
 * @see <a href="http://www.ninjaframework.org/documentation/basic_concepts/dependency_injection.html">>http://www.ninjaframework.org/documentation/basic_concepts/dependency_injection.html</a>
 */
@Singleton
public class Module extends AbstractModule {

    /**
     * Random class instance.
     */
    private static final Random RND = new Random();

    /**
     * Bind your injections here.
     */
    @Override
    protected void configure() {
        bind(Random.class)
                .annotatedWith(Names.named("RND"))
                .toInstance(RND);
    }

}
