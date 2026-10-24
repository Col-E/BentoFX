package software.coley.bentofx.persistence.testfixtures.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLayoutStorageTest {

	private static final String STORAGE_EXISTS_DESCRIPTION = "storage.exists()";
	private static final String STORAGE_TO_BYTE_ARRAY_DESCRIPTION = "storage.toByteArray()";

	@Test
	void defaultStorageDoesNotExist() {
		try(final InMemoryLayoutStorage storage = new InMemoryLayoutStorage()) {

			assertThat(storage.exists())
					.describedAs(STORAGE_EXISTS_DESCRIPTION)
					.isFalse();
			assertThat(storage.toByteArray())
					.describedAs(STORAGE_TO_BYTE_ARRAY_DESCRIPTION)
					.isEmpty();
		}
	}

	@Test
	void emptyContentIsNotALayout() {
		try(final InMemoryLayoutStorage storage = new InMemoryLayoutStorage(new byte[0])) {

			assertThat(storage.exists())
					.describedAs(STORAGE_EXISTS_DESCRIPTION)
					.isFalse();
		}
	}

	@Test
	void storageExistsAfterOutputStreamIsClosed() throws IOException {
		try(final InMemoryLayoutStorage storage = new InMemoryLayoutStorage()) {

			try (OutputStream outputStream = storage.openOutputStream()) {
				outputStream.write(new byte[]{1, 2, 3});
			}

			assertThat(storage.exists())
					.describedAs(STORAGE_EXISTS_DESCRIPTION)
					.isTrue();
			assertThat(storage.openInputStream().readAllBytes())
					.describedAs("storage.openInputStream().readAllBytes()")
					.containsExactly(1, 2, 3);
		}
	}

	@Test
	void writeCopiesInputBytes() {
		try(final InMemoryLayoutStorage storage = new InMemoryLayoutStorage()) {
			final byte[] bytes = {1, 2, 3};

			storage.write(bytes);
			bytes[0] = 9;

			assertThat(storage.toByteArray())
					.describedAs(STORAGE_TO_BYTE_ARRAY_DESCRIPTION)
					.containsExactly(1, 2, 3);
		}
	}

	@Test
	void toByteArrayReturnsDefensiveCopy() {
		try(final InMemoryLayoutStorage storage = new InMemoryLayoutStorage(new byte[]{1, 2, 3})) {

			final byte[] copy = storage.toByteArray();
			copy[0] = 9;

			assertThat(storage.toByteArray())
					.describedAs(STORAGE_TO_BYTE_ARRAY_DESCRIPTION)
					.containsExactly(1, 2, 3);
		}
	}

	@Test
	void deleteClearsStorage() {
		try(final InMemoryLayoutStorage storage = new InMemoryLayoutStorage(new byte[]{1, 2, 3})) {

			storage.delete();

			assertThat(storage.exists())
					.describedAs(STORAGE_EXISTS_DESCRIPTION)
					.isFalse();
			assertThat(storage.toByteArray())
					.describedAs(STORAGE_TO_BYTE_ARRAY_DESCRIPTION)
					.isEmpty();
		}
	}
}
