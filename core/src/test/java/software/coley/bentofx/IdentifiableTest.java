package software.coley.bentofx;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for {@link Identifiable}'s default {@code matchesIdentity} method.
 *
 * @author Phil Bryant
 */
class IdentifiableTest {

    private static final String IDENTIFIER = "identifiable-1";
    private static final String OTHER_IDENTIFIER = "identifiable-2";

    @Test
    void matchesIdentityIsTrueWhenTheOtherObjectHasTheSameIdentifier() {
        Identifiable subject = newIdentifiableImpl(IDENTIFIER);
        Identifiable other = newIdentifiableImpl(IDENTIFIER);

        assertThat(subject.matchesIdentity(other))
                .describedAs("matchesIdentity() for two objects with the same identifier")
                .isTrue();
    }

    @Test
    void matchesIdentityIsFalseWhenTheOtherObjectHasADifferentIdentifier() {
        Identifiable subject = newIdentifiableImpl(IDENTIFIER);
        Identifiable other = newIdentifiableImpl(OTHER_IDENTIFIER);

        assertThat(subject.matchesIdentity(other))
                .describedAs("matchesIdentity() for objects with different identifiers")
                .isFalse();
    }

    /**
     * {@code matchesIdentity} widened its parameter to {@code @Nullable} - this
     * is the defect that widening fixed: a {@code null} other used to be a
     * {@code NullPointerException} risk, and must now just report no match.
     */
    @Test
    void matchesIdentityIsFalseWhenTheOtherObjectIsNull() {
        Identifiable subject = newIdentifiableImpl(IDENTIFIER);

        assertThat(subject.matchesIdentity(null))
                .describedAs("matchesIdentity(null)")
                .isFalse();
    }

    /**
     * {@return an {@link Identifiable} whose {@link Identifiable#getIdentifier()} is the given identifier}
     *
     * @param identifier the identifier to return.
     */
    private static Identifiable newIdentifiableImpl(final String identifier) {
        return () -> identifier;
    }
}
