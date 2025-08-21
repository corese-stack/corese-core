package fr.inria.corese.core.load;

import fr.inria.corese.core.compiler.parser.Pragma;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.query.QueryEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link QueryLoad} class.
 * This class uses JUnit 5 and Mockito to test various functionalities
 * including reading from readers, parsing query names, handling URLs,
 * and managing temporary file creation and writing.
 */
class QueryLoadTest {

    private QueryLoad queryLoad;

    @Mock
    private QueryEngine mockEngine;

    @Mock
    private Query mockQuery;

    @Mock
    private IDatatype mockDatatype;

    @TempDir
    Path tempDir;

    /**
     * Sets up the test environment before each test method.
     * Initializes all mocks and creates a new instance of QueryLoad with the mocked QueryEngine.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queryLoad = new QueryLoad(mockEngine);
    }



    /**
     * Tests that the {@code read} method correctly reads a single line from a {@link Reader}.
     *
     * @throws IOException if an I/O error occurs during reading.
     */
    @Test
    @DisplayName("Should correctly read a single line from a reader")
    void testReadSingleLine() throws IOException {
        String content = "SELECT * WHERE { ?s ?p ?o }";
        Reader reader = new StringReader(content);
        assertEquals(content, queryLoad.read(reader));
    }

    /**
     * Tests that the {@code read} method correctly reads multi-line content from a {@link Reader}.
     * It also verifies that newline characters are appended correctly between lines.
     *
     * @throws IOException if an I/O error occurs during reading.
     */
    @Test
    @DisplayName("Should correctly read multi-line content from a reader")
    void testReadMultiLine() throws IOException {
        String content = "LINE1\nLINE2\nLINE3";
        Reader reader = new StringReader(content);
        assertEquals("LINE1\nLINE2\nLINE3", queryLoad.read(reader));
    }

    /**
     * Tests that the {@code read} method correctly handles an empty content from a {@link Reader}.
     *
     * @throws IOException if an I/O error occurs during reading.
     */
    @Test
    @DisplayName("Should correctly read empty content from a reader")
    void testReadEmpty() throws IOException {
        String content = "";
        Reader reader = new StringReader(content);
        assertEquals(content, queryLoad.read(reader));
    }


    /**
     * Tests the {@code getName} method to ensure it correctly extracts the base name
     * of a file without its extension, handling single and multi-part extensions.
     */
    @Test
    @DisplayName("Should extract the name without the extension")
    void testGetName() {
        assertEquals("myfile", queryLoad.getName("myfile.txt"));
        assertEquals("document", queryLoad.getName("document"));
        assertEquals("archive", queryLoad.getName("archive.gz"));
    }

    /**
     * Tests the {@code getSuffix} method to ensure it correctly extracts the file extension,
     * including the leading dot. It also verifies default suffix for files without extensions.
     */
    @Test
    @DisplayName("Should extract the suffix with the dot")
    void testGetSuffix() {
        assertEquals(".txt", queryLoad.getSuffix("myfile.txt"));
        assertEquals(".txt", queryLoad.getSuffix("document"));
        assertEquals(".gz", queryLoad.getSuffix("archive.gz"));
    }


    /**
     * Tests the {@code isURL} method with various valid URL strings to ensure they are correctly identified.
     */
    @Test
    @DisplayName("Should identify valid URL strings")
    void testIsURLValid() {
        assertTrue(queryLoad.isURL("http://example.com"));
        assertTrue(queryLoad.isURL("https://www.google.com/search?q=test"));
        assertTrue(queryLoad.isURL("ftp://user:pass@host/path"));
    }

    /**
     * Tests the {@code isURL} method with various invalid URL strings to ensure they are correctly identified.
     */
    @Test
    @DisplayName("Should identify invalid URL strings")
    void testIsURLInvalid() {
        assertFalse(queryLoad.isURL("not_a_url"));
        assertFalse(queryLoad.isURL("www.example.com"));
        assertFalse(queryLoad.isURL("http:// invalid.com"));
    }


    /**
     * Tests that the {@code getResource} method throws an {@link IOException}
     * when attempting to load a non-existent classpath resource.
     */
    @Test
    @DisplayName("Should throw IOException if classpath resource is not found")
    void testGetResourceNotFound() {
        assertThrows(IOException.class, () -> queryLoad.getResource("/non_existent_resource.txt"));
    }


    /**
     * Tests that the {@code write} method successfully writes string content to a specified file path.
     *
     * @throws IOException if an I/O error occurs during writing or reading the file.
     */
    @Test
    @DisplayName("Should write string content to a named file path")
    void testWriteWithNameAndStringContent() throws IOException {
        String filePath = tempDir.resolve("named_output.txt").toString();
        String content = "Content for named file.";
        queryLoad.write(filePath, content);
        assertEquals(content, Files.readString(Path.of(filePath)));
    }

    /**
     * Tests that the {@code write} method successfully writes the string value of an {@link IDatatype}
     * to a specified file path.
     *
     * @throws IOException if an I/O error occurs during writing or reading the file.
     */
    @Test
    @DisplayName("Should write the string value of an IDatatype to a named file path")
    void testWriteWithNameAndIDatatypeContent() throws IOException {
        String filePath = tempDir.resolve("datatype_output.txt").toString();
        when(mockDatatype.stringValue()).thenReturn("IDatatype content.");
        queryLoad.write(filePath, mockDatatype);
        assertEquals("IDatatype content.", Files.readString(Path.of(filePath)));
    }


    /**
     * Tests that the {@code parse} method (taking a string name) correctly reads a query
     * from a file and defines it in the {@link QueryEngine}.
     * It also verifies that {@code setPragma} is called on the resulting {@link Query} object.
     *
     * @throws LoadException   if an error occurs during loading the query.
     * @throws EngineException if an error occurs during defining the query in the engine.
     * @throws IOException     if an I/O error occurs during file operations.
     */
    @Test
    @DisplayName("Should parse query from a string name and define it in the engine")
    void testParseStringName() throws LoadException, EngineException, IOException {
        String queryContent = "SELECT * WHERE { ?s ?p ?o }";
        Path tempFile = tempDir.resolve("query.rq");
        Files.writeString(tempFile, queryContent);

        when(mockEngine.defQuery(anyString())).thenReturn(mockQuery);

        queryLoad.parse(tempFile.toString());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEngine).defQuery(queryCaptor.capture());
        assertEquals(queryContent, queryCaptor.getValue());

        verify(mockQuery).setPragma(Pragma.FILE, tempFile.toString());
    }

    /**
     * Tests that the {@code parse} method (taking a string name) throws a {@link LoadException}
     * if the specified query file does not exist.
     */
    @Test
    @DisplayName("Should throw LoadException if query file is not found during parse(String)")
    void testParseStringNameFileNotFound() {
        assertThrows(LoadException.class, () -> queryLoad.parse("nonexistent.rq"));
    }

    /**
     * Tests that the {@code parse} method (taking a {@link Reader}) correctly reads a query
     * and defines it in the {@link QueryEngine}.
     *
     * @throws LoadException   if an error occurs during loading the query.
     * @throws EngineException if an error occurs during defining the query in the engine.
     */
    @Test
    @DisplayName("Should parse query from a Reader and define it in the engine")
    void testParseReader() throws LoadException, EngineException {
        String queryContent = "ASK { ?s ?p ?o }";
        Reader reader = new StringReader(queryContent);

        when(mockEngine.defQuery(anyString())).thenReturn(mockQuery);

        queryLoad.parse(reader);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEngine).defQuery(queryCaptor.capture());
        assertEquals(queryContent, queryCaptor.getValue());
    }


    /**
     * Tests that the {@code readWE} method correctly reads content from a local file path.
     *
     * @throws LoadException if an error occurs during loading the query.
     * @throws IOException   if an I/O error occurs during file operations.
     */
    @Test
    @DisplayName("Should read content from a local file path")
    void testReadWEFromFile() throws LoadException, IOException {
        String content = "Content of the local file.";
        Path file = tempDir.resolve("local_file.txt");
        Files.writeString(file, content);

        assertEquals(content, queryLoad.readWE(file.toString()));
    }


    /**
     * Tests that the {@code readWE} method throws a {@link LoadException}
     * when attempting to read a non-existent file.
     */
    @Test
    @DisplayName("Should throw LoadException for a non-existent file during readWE")
    void testReadWEFileNonExistent() {
        assertThrows(LoadException.class, () -> queryLoad.readWE("nonexistent_file.txt"));
    }


    /**
     * Tests that the {@code basicParse} method correctly parses content from a local file path.
     *
     * @throws EngineException if an error occurs during parsing or engine operations.
     * @throws IOException     if an I/O error occurs during file operations.
     */
    @Test
    @DisplayName("Should parse content from a local file path in basicParse")
    void testBasicParseFromFile() throws EngineException, IOException {
        String content = "BASIC PARSED CONTENT.";
        Path file = tempDir.resolve("basic_parse_file.txt");
        Files.writeString(file, content);


        assertEquals(content, queryLoad.basicParse(file.toString()));
    }

    /**
     * Tests that the {@code basicParse} method throws an {@link EngineException}
     * if it fails to read the specified file.
     */
    @Test
    @DisplayName("Should throw EngineException if basicParse fails to read the file")
    void testBasicParseFileFailure() {
        assertThrows(EngineException.class, () -> queryLoad.basicParse("invalid/nonexistent.txt"));
    }
}
