package conf;

import java.util.List;
import ninja.Filter;

/**
 * Internal filters configuration class for the website access.
 * Good for authorization and security.
 * @author Nhu Huy Le <mail@huy-le.de>
 * @see <a href="http://www.ninjaframework.org/documentation/basic_concepts/filters.html">http://www.ninjaframework.org/documentation/basic_concepts/filters.html</a>
 */
public class Filters implements ninja.application.ApplicationFilters {

    /**
     * Adding filters here.
     * @param filters filters to add.
     */
    @Override
    public void addFilters(final List<Class<? extends Filter>> filters) {
    }
}
