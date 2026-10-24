package software.coley.bentofx.persistence.impl.storage.db;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseLayoutStorageTest {

	private static final String LAYOUT_IDENTIFIER = "layout";
	private static final String CODEC_IDENTIFIER = "json";

	/**
	 * Never invoked by these tests - only supplies a non-null reference for
	 * the two tests whose null check under test is for a different
	 * constructor parameter. A real {@link EntityManagerFactory} - and the
	 * database it would open - is not needed to exercise argument validation.
	 */
	private static final EntityManagerFactory UNUSED_ENTITY_MANAGER_FACTORY =
			(EntityManagerFactory) Proxy.newProxyInstance(
					DatabaseLayoutStorageTest.class.getClassLoader(),
					new Class<?>[]{EntityManagerFactory.class},
					(proxy, method, args) -> {
						throw new UnsupportedOperationException(
								"not used by these tests: " + method
						);
					}
			);

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void rejectsAMissingEntityManagerFactory() {
		assertThatThrownBy(() ->
				new DatabaseLayoutStorage(null, LAYOUT_IDENTIFIER, CODEC_IDENTIFIER)
		)
				.describedAs("null entity manager factory")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("emf");
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void rejectsAMissingLayoutIdentifier() {
		assertThatThrownBy(() ->
				new DatabaseLayoutStorage(UNUSED_ENTITY_MANAGER_FACTORY, null, CODEC_IDENTIFIER)
		)
				.describedAs("null layout identifier")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("layoutIdentifier");
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void rejectsAMissingCodecIdentifier() {
		assertThatThrownBy(() ->
				new DatabaseLayoutStorage(UNUSED_ENTITY_MANAGER_FACTORY, LAYOUT_IDENTIFIER, null)
		)
				.describedAs("null codec identifier")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("codecIdentifier");
	}
}
