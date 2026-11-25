package software.coley.bentofx.persistence.impl.storage.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class FileLayoutStorageITP {
	private static final String TEST_FILE_NAME = "test-layout.bento";
	private static final String TEST_FILE_CONTENT = "Test data for FileLayoutStorage integration test";
	private static final String PREVIOUS_FILE_CONTENT = "A layout that was already saved";

	// None of these three are @Nullable: @TempDir populates temporaryDirectory
	// before any @BeforeEach or @Test method runs, and setUp() below
	// unconditionally assigns the other two before every @Test method runs -
	// neither is ever actually null at any point this class's own code
	// observes it.
	@TempDir
	private Path temporaryDirectory;

	private FileLayoutStorage fileLayoutStorage;
	private File testFile;

	@BeforeEach
	void setUp() {
		testFile = new File(temporaryDirectory.toFile(), TEST_FILE_NAME);
		fileLayoutStorage = new FileLayoutStorage(testFile);
	}

	@Test
	void anEmptyFileIsNotALayout() throws IOException {
		Files.createFile(testFile.toPath());

		assertThat(fileLayoutStorage.exists())
				.describedAs("exists() for a zero-byte file")
				.isFalse();
	}

	@Test
	void aDirectoryIsNotALayout() throws IOException {
		Files.createDirectory(testFile.toPath());

		assertThat(fileLayoutStorage.exists())
				.describedAs("exists() for a directory with the layout's name")
				.isFalse();
	}

	@Test
	void previousLayoutSurvivesUntilTheStreamCloses() throws IOException {
		Files.writeString(testFile.toPath(), PREVIOUS_FILE_CONTENT, UTF_8);

		try (OutputStream outputStream = fileLayoutStorage.openOutputStream()) {
			outputStream.write(TEST_FILE_CONTENT.getBytes(UTF_8));
			outputStream.flush();

			assertThat(Files.readString(testFile.toPath(), UTF_8))
					.describedAs("target content while the stream is open")
					.isEqualTo(PREVIOUS_FILE_CONTENT);
		}

		assertThat(Files.readString(testFile.toPath(), UTF_8))
				.describedAs("target content after the stream closes")
				.isEqualTo(TEST_FILE_CONTENT);
	}

	@Test
	void previousLayoutSurvivesAStreamThatIsNeverClosed() throws IOException {
		Files.writeString(testFile.toPath(), PREVIOUS_FILE_CONTENT, UTF_8);

		// A save that dies part way through, which used to leave the target
		// holding whatever had been written so far.
		final OutputStream abandoned = fileLayoutStorage.openOutputStream();
		abandoned.write(TEST_FILE_CONTENT.getBytes(UTF_8));
		abandoned.flush();

		assertThat(Files.readString(testFile.toPath(), UTF_8))
				.describedAs("target content after an abandoned save")
				.isEqualTo(PREVIOUS_FILE_CONTENT);
	}

	@Test
	void testFileLayoutStorageIntegration() throws IOException {
		// Test the file does not initially exist
		assertThat(fileLayoutStorage.exists())
				.describedAs("File should not exist initially")
				.isFalse();

		// Write data to the file and verify existence
		try (OutputStream outputStream = fileLayoutStorage.openOutputStream()) {
			outputStream.write(TEST_FILE_CONTENT.getBytes(UTF_8));
		}
		assertThat(fileLayoutStorage.exists())
				.describedAs("File should exist after writing to it")
				.isTrue();
		assertThat(testFile)
				.describedAs("File length should be greater than zero after writing data")
				.isNotEmpty();

		// Read data from the file and verify the content
		StringBuilder fileContent = new StringBuilder();
		try (InputStream inputStream = fileLayoutStorage.openInputStream();
			 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				fileContent.append(line);
			}
		}
		assertThat(fileContent)
				.describedAs("File content should match the written data")
				.hasToString(TEST_FILE_CONTENT);
	}

	@Test
	void openOutputStreamCreatesMissingParentDirectories() throws IOException {
		final File nestedFile = new File(
				temporaryDirectory.toFile(),
				"nested/deeper/" + TEST_FILE_NAME
		);
		final FileLayoutStorage nestedStorage = new FileLayoutStorage(nestedFile);

		try (OutputStream outputStream = nestedStorage.openOutputStream()) {
			outputStream.write(TEST_FILE_CONTENT.getBytes(UTF_8));
		}

		assertThat(nestedStorage.exists())
				.describedAs("exists() after writing through missing parent directories")
				.isTrue();
		assertThat(Files.readString(nestedFile.toPath(), UTF_8))
				.describedAs("content written through missing parent directories")
				.isEqualTo(TEST_FILE_CONTENT);
	}

	@Test
	void writingOneByteAtATimeStillPromotesTheFileOnClose() throws IOException {
		try (OutputStream outputStream = fileLayoutStorage.openOutputStream()) {
			for (final byte b : TEST_FILE_CONTENT.getBytes(UTF_8)) {
				outputStream.write(b);
			}
		}

		assertThat(Files.readString(testFile.toPath(), UTF_8))
				.describedAs("content written one byte at a time")
				.isEqualTo(TEST_FILE_CONTENT);
	}

	@Test
	void closingTheStreamTwiceIsANoOp() throws IOException {
		final OutputStream outputStream = fileLayoutStorage.openOutputStream();
		outputStream.write(TEST_FILE_CONTENT.getBytes(UTF_8));

		outputStream.close();
		outputStream.close();

		assertThat(Files.readString(testFile.toPath(), UTF_8))
				.describedAs("content after closing the stream twice")
				.isEqualTo(TEST_FILE_CONTENT);
	}

	// The write-failure path (failed flag set, partial file deleted, target
	// left alone) is deliberately not covered here: reaching it needs the
	// delegate stream to fail on write, and every portable way tried to force
	// that either needed reflection JPMS blocks on java.io.FilterOutputStream
	// or depended on OS-specific file-locking/deletion behavior. Left
	// uncovered rather than tested with something unreliable.
}
