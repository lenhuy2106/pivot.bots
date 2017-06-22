/*
 * Nhu Huy Le
 */
package database;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import ninja.uploads.FileItem;
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
    
    public PropertyDB(String name) throws IOException {
        file = new File("src/main/java/assets/data/" + name + ".db");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            LOG.info("user database not found. Creating fresh...");
            file.createNewFile();
        }
    }
    
    @Override
    public void saveString(String property, String val) throws IOException {
        try (FileWriter writer = new FileWriter(file.getPath());) {
            props.setProperty(property, val);
            props.store(writer, "database file");
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
        try (FileReader reader = new FileReader(file.getPath());) {
            props.load(reader);
            return props.getProperty(property, defVal);
        }
    }
    
    public static Optional<String> importFile(FileItem userItem) {
        // TODO more upload verification
        try {
            if (!userItem.getFileName().endsWith(".db")) {
                throw new IOException("Uploaded file not compatible.");
            }
            Files.copy(
                    userItem.getFile().toPath(),
                    new File("src/main/java/assets/data/" + userItem.getFileName()).toPath());
            // removed ending
            return Optional.of(userItem.getFileName().substring(0, userItem.getFileName().length() - 3));
        } 
        catch (IOException ex) {
            LOG.error("Uploaded file is corrupted or already exists.", ex);
            return Optional.ofNullable(null);
        }
    }
    
}
