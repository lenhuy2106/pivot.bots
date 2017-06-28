package database;

import static database.Constants.DATA_PATH;
import static database.Constants.DELIMITER;
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
 * A database based on java property files.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class PropertyDB implements Database {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);

    /**
     * Java property file.
     */
    private Properties props = new Properties();

    /**
     * File to save/load.
     */
    private File file;

    public PropertyDB() {
    }

    public PropertyDB(String name) throws IOException {
        file = new File(DATA_PATH + name + ".db");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            LOG.info("user database not found. Creating fresh...");
            file.createNewFile();
        }
    }

    /**
     * Save a string type.
     * @param property key to save.
     * @param val string to save.
     * @return this
     * @throws IOException If file is not accessable.
     */
    @Override
    public PropertyDB saveString(String property, String val) throws IOException {
        try (FileWriter writer = new FileWriter(file.getPath());) {
            props.setProperty(property, val);
            props.store(writer, "database file");
        }

        return this;
    }

    /**
     * Save a collection type.
     * @param property key to save.
     * @param coll collection to save.
     * @return this
     * @throws IOException If file is not accessable.
     */
    @Override
    public PropertyDB saveCollection(String property, Collection coll) throws IOException {
        String toSave = (String) coll.stream().map(Object::toString).collect(Collectors.joining(DELIMITER));
        saveString(property, toSave);
        return this;
    }

    /**
     * Save an integer type.
     * @param property key to save.
     * @param val integer to save.
     * @return this
     * @throws IOException If file is not accessable.
     */
    @Override
    public PropertyDB saveInt(String property, int val) throws IOException {
        saveString(property, String.valueOf(val));
        return this;
    }

    /**
     * Load an integer type.
     * @param property key to load.
     * @param defVal dafault value.
     * @return loaded integer.
     * @throws IOException If file is not accessable.
     */
    @Override
    public int loadInt(String property, int defVal) throws IOException {
        return Integer.parseInt(loadString(property, String.valueOf(defVal)));
    }

    /**
     * Load a typed collection.
     * @param <T> generic type.
     * @param property key to load.
     * @param coll Collection to load/add into.
     * @return Added collection.
     * @throws IOException If file is not accessable.
     */
    @Override
    public <T extends Collection> T loadCollection(String property, T coll) throws IOException {
        String backend = loadString(property, "");
        StringTokenizer st = new StringTokenizer(backend, DELIMITER);
        while (st.hasMoreTokens()) {
            coll.add(st.nextToken().trim());
        }
        return coll;
    }

    /**
     * Load a string.
     * @param property key to load.
     * @param defVal default string.
     * @return loaded string.
     * @throws IOException If file is not accessable.
     */
    @Override
    public String loadString(String property, String defVal) throws IOException {
        try (FileReader reader = new FileReader(file.getPath());) {
            props.load(reader);
            return props.getProperty(property, defVal);
        }
    }

    /**
     * Imports a property file.
     * @param item property file item to import.
     * @return Imported file name without ending. Returns null if failed.
     */
    public static Optional<String> importFile(FileItem item) {
        try {
            if (!item.getFileName().endsWith(".db")) {
                throw new IOException("Uploaded file not compatible.");
            }
            Files.copy(
                    item.getFile().toPath(),
                    new File(DATA_PATH + item.getFileName()).toPath());
            // remove ending
            return Optional.of(item.getFileName().substring(0, item.getFileName().length() - 3));
        }
        catch (IOException ex) {
            LOG.error("Uploaded file is corrupted or already exists.", ex);
            return Optional.ofNullable(null);
        }
    }

}
