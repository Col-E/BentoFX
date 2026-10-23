package software.coley.bentofx.persistence.core.api.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.Bento;

import static org.assertj.core.api.Assertions.assertThat;

class BentoProviderTest {

	private static final String FIRST_BENTO_IDENTIFIER = "first";
	private static final String SECOND_BENTO_IDENTIFIER = "second";

	private static final String PROVIDER_GET_ALL_BENTOS_DESCRIPTION = "BentoProvider.of(...).getAllBentos()";

	@Test
	void ofResolvesEveryBentoItWasGiven() {
		final Bento first = new Bento(FIRST_BENTO_IDENTIFIER);
		final Bento second = new Bento(SECOND_BENTO_IDENTIFIER);

		final BentoProvider provider = BentoProvider.of(first, second);

		assertThat(provider.getBento(FIRST_BENTO_IDENTIFIER))
				.describedAs("BentoProvider.of(...).getBento(\"first\")")
				.containsSame(first);
		assertThat(provider.getBento(SECOND_BENTO_IDENTIFIER))
				.describedAs("BentoProvider.of(...).getBento(\"second\")")
				.containsSame(second);
		assertThat(provider.getAllBentos())
				.describedAs(PROVIDER_GET_ALL_BENTOS_DESCRIPTION)
				.containsExactlyInAnyOrder(first, second);
	}

	@Test
	void ofWithNoBentosResolvesNothing() {
		final BentoProvider provider = BentoProvider.of();

		assertThat(provider.getBento(FIRST_BENTO_IDENTIFIER))
				.describedAs("BentoProvider.of().getBento(\"first\")")
				.isEmpty();
		assertThat(provider.getAllBentos())
				.describedAs(PROVIDER_GET_ALL_BENTOS_DESCRIPTION)
				.isEmpty();
	}
}
