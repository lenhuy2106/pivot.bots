/*
 * Nhu Huy Le
 */
package database;

import java.io.IOException;
import java.util.Collection;

/**
 *
 * @author T500
 */
public interface Database {
    
    public void saveInt(String property, int val) throws IOException;
    public void saveString(String property, String val) throws IOException;
    public void saveCollection(String property, Collection coll) throws IOException;
    public int loadInt(String property, int defVal) throws IOException;
    public String loadString(String property, String defVal) throws IOException;
    public <T extends Collection> T loadCollection(String property, T defVal) throws IOException;
}
