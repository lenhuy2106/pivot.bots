package database;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Database interface.
 * Implement for every different type of database.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public interface Database {

    Database saveInt(String property, int val) throws IOException;
    Database saveString(String property, String val) throws IOException;
    Database saveCollection(String property, Collection coll) throws IOException;
    int loadInt(String property, int defVal) throws IOException;
    String loadString(String property, String defVal) throws IOException;
    <T extends Collection> T loadCollection(String property, T defVal) throws IOException;
}
