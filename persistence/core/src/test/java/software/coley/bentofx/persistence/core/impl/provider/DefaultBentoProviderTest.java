package software.coley.bentofx.persistence.core.impl.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.Bento;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBentoProviderTest {

	private static final String FIRST_BENTO_IDENTIFIER = "first";
	private static final String SECOND_BENTO_IDENTIFIER = "second";
	private static final String DUPLICATE_BENTO_IDENTIFIER = "duplicate";

	private static final String PROVIDER_GET_ALLBENTOS_DESCRIPTION = "provider.getAllBentos()";

	@Test
	void constructorRegistersProvidedBentosByIdentifier() {
		final Bento first = new Bento(FIRST_BENTO_IDENTIFIER);
		final Bento second = new Bento(SECOND_BENTO_IDENTIFIER);
		final DefaultBentoProvider provider = new DefaultBentoProvider(first, second);

		assertThat(provider.getBento(FIRST_BENTO_IDENTIFIER))
				.describedAs("provider.getBento(\"first\")")
				.containsSame(first);
		assertThat(provider.getBento(SECOND_BENTO_IDENTIFIER))
				.describedAs("provider.getBento(\"second\")")
				.containsSame(second);
		assertThat(provider.getAllBentos())
				.describedAs(PROVIDER_GET_ALLBENTOS_DESCRIPTION)
				.containsExactlyInAnyOrder(first, second);
	}

	@Test
	void addBentoReplacesExistingBentoWithSameIdentifier() {
		final Bento original = new Bento(DUPLICATE_BENTO_IDENTIFIER);
		final Bento replacement = new Bento(DUPLICATE_BENTO_IDENTIFIER);
		final DefaultBentoProvider provider = new DefaultBentoProvider(original);

		provider.addBento(replacement);

		assertThat(provider.getBento(DUPLICATE_BENTO_IDENTIFIER))
				.describedAs("provider.getBento(\"duplicate\")")
				.containsSame(replacement);
		assertThat(provider.getAllBentos())
				.describedAs(PROVIDER_GET_ALLBENTOS_DESCRIPTION)
				.containsExactly(replacement);
	}

	@Test
	void unknownIdentifierReturnsEmptyOptional() {
		final DefaultBentoProvider provider = new DefaultBentoProvider();

		assertThat(provider.getBento("missing"))
				.describedAs("provider.getBento(\"missing\")")
				.isEmpty();
	}
}
