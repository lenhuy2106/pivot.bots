/*
 * Nhu Huy Le
 */
package database;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author T500
 */
public class PropertyDB implements Database {
    
    private static final Logger LOG = LoggerFactory.getLogger(Database.class);
    
    private static final String DELIMITER = "|";    
    
    private final Properties props = new Properties();
    
    private final File file;

    public PropertyDB(String user) throws IOException {
        file = new File(user + ".db");
        if (!file.exists()) {
            LOG.info("user database not found. Creating fresh...");
            file.createNewFile();
        }
    }
    
    @Override
    public void saveString(String property, String val) throws IOException {
        try (FileWriter writer = new FileWriter(file.getName());) {
            props.setProperty(property, val);
            props.store(writer, "user database");
        }
    }

    @Override
    public void saveCollection(String property, Collection coll) throws IOException {
        String toSave = (String) coll.stream().map(Object::toString).collect(Collectors.joining(DELIMITER));
        saveString(property, toSave);
    }

    @Override
    public void saveInt(String property, int val) throws IOException {
        saveString(property, String.valueOf(val));
    }

    @Override
    public int loadInt(String property, int defVal) throws IOException {
        return Integer.parseInt(loadString(property, String.valueOf(defVal)));
    }
    
    @Override
    public <T extends Collection> T loadCollection(String property, T coll) throws IOException {
        String backend = loadString(property, "");
        StringTokenizer st = new StringTokenizer(backend, DELIMITER);
        while (st.hasMoreTokens()) {
            coll.add(st.nextToken().trim());
        }
        return coll;
    }
    
    @Override
    public String loadString(String property, String defVal) throws IOException {
        try (FileReader reader = new FileReader(file.getName());) {
            props.load(reader);
            return props.getProperty(property, defVal);
        }
    }
    
}
