/*
 * Nhu Huy Le
 */
package database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import ninja.uploads.FileItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Nhu Huy Le <mail@huy-le.de>
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertyDBTest {

    private static final String PATH_TEST = "test.db";

    @Mock
    File file;

    @Spy
    Properties props;

    @InjectMocks
    private PropertyDB instance = new PropertyDB();

    public PropertyDBTest() throws IOException {
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        File file = new File(PATH_TEST);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    @AfterClass
    public static void afterClass() throws IOException {
        File file = new File(PATH_TEST);
    }

    @Before
    public void before() {
        when(file.getPath()).thenReturn(PATH_TEST);
    }

    @After
    public void after() throws FileNotFoundException {
        // clear file content
        props.clear();
        file.delete();
    }

    /**
     * Test of saveString method, of class PropertyDB.
     */
    @Test
    public void testSaveString() throws Exception {
        System.out.println("saveString");
        String property = "prop";
        String val = "val";
        instance.saveString(property, val);
        verify(props).setProperty(property, val);
        // description
        verify(props).store(any(FileWriter.class), anyString());
    }

    /**
     * Test of saveCollection method, of class PropertyDB.
//     */
    @Test
    public void testSaveCollection() throws Exception {
        System.out.println("saveCollection");
        String key = "coll";
        instance.saveCollection(key, Arrays.asList(new String[] {"1", "2"}));
        verify(props).setProperty(eq(key), anyString());
    }

    /**
     * Test of saveInt method, of class PropertyDB.
     */
    @Test
    public void testSaveInt() throws Exception {
        System.out.println("saveInt");
        String key = "int";
        int val = 0;
        instance.saveInt(key, val);
        verify(props).setProperty(eq(key), eq(String.valueOf(0)));
    }

    /**
     * Test of loadInt method, of class PropertyDB.
     */
    @Test
    public void testLoadInt() throws Exception {
        System.out.println("loadInt");
        String key = "int2";
        int result = instance.loadInt(key, 1);
        verify(props).getProperty(eq(key), eq("1"));
        assertEquals(result, 1);

    }

    /**
     * Test of loadCollection method, of class PropertyDB.
     */
    @Test
    public void testLoadCollection() throws Exception {
        System.out.println("loadCollection");
        String key = "prop";
        Collection coll = new HashSet<>();
        Collection result = instance.loadCollection(key, coll);
        assertTrue(result.isEmpty());
    }

    /**
     * Test of loadString method, of class PropertyDB.
//     */
    @Test
    public void testLoadString() throws Exception {
        System.out.println("loadString");
        String property = "string2";
        String test = "test";
        String result = instance.loadString(property, test);
        verify(props).load(any(FileReader.class));
        verify(props).getProperty(eq(property), eq(test));
        assertEquals(test, result);
    }

    /**
     * Test of importFile method, of class PropertyDB.
     */
    @Test
    public void testImportInvalidFile() {
        System.out.println("importFile");
        FileItem item = Mockito.mock(FileItem.class);
        when(item.getFileName()).thenReturn("invalid");
        Optional<String> result = PropertyDB.importFile(item);
        assertFalse(result.isPresent());
    }

}
