package software.coley.bentofx.persistence.core.api.codec;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.state.BentoState;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * What a {@link LayoutCodec} encodes and decodes: the state that makes up a
 * layout, together with the layout-level metadata that travels with it.
 *
 * <p>The state is the {@link BentoState} list a save captured. The one piece of
 * metadata so far is an optional display name, the human-readable label a user
 * gave a layout they saved. It rides inside the encoded layout so that a stored
 * layout carries its own name - a file copied to another machine, or a row read
 * back from a database, arrives knowing what it is called - rather than an
 * application having to keep names in a table of its own.</p>
 *
 * <p>A layout saved without a name, such as the session layout, has no display
 * name. The state list is never {@code null}, though it may be empty.</p>
 *
 * @param displayName the layout's human-readable name, or {@code null} when it
 * has none.
 * @param bentoStates the state that makes up the layout. Copied on
 * construction, so this record does not share a list with its caller.
 * @param group the group the layout belongs to, or {@code null} when it belongs
 * to none.
 * @param groups the groups that exist, which is empty on every layout but the
 * group catalog - see
 * {@link software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers#GROUP_CATALOG_LAYOUT_IDENTIFIER}.
 * Copied on construction.
 *
 * @author Phil Bryant
 */
public record PersistableLayout(
		@Nullable String displayName,
		List<BentoState> bentoStates,
		@Nullable String group,
		List<String> groups
) {

	/**
	 * Canonical constructor, copying both lists so the layout is immutable.
	 */
	public PersistableLayout {
		requireNonNull(bentoStates, "bentoStates");
		requireNonNull(groups, "groups");
		bentoStates = List.copyOf(bentoStates);
		groups = List.copyOf(groups);
	}

	/**
	 * Constructs a layout that belongs to no group and declares none, which is
	 * every layout but one a user has grouped and the group catalog itself.
	 *
	 * @param displayName the layout's human-readable name, or {@code null} when
	 * it has none.
	 * @param bentoStates the state that makes up the layout.
	 */
	public PersistableLayout(
			final @Nullable String displayName,
			final List<BentoState> bentoStates
	) {
		this(displayName, bentoStates, null, List.of());
	}

	/**
	 * {@return a layout of the supplied state with no display name}, for one
	 * that is not saved under a name of its own.
	 *
	 * @param bentoStates the state that makes up the layout.
	 */
	public static PersistableLayout of(final List<BentoState> bentoStates) {
		return new PersistableLayout(null, bentoStates);
	}

	/**
	 * {@return a group catalog holding the supplied group names, which is a
	 * layout in name only: it carries no state and is never restored.}
	 *
	 * @param groups the groups that exist.
	 */
	public static PersistableLayout ofGroups(final List<String> groups) {
		return new PersistableLayout(null, List.of(), null, groups);
	}

	/**
	 * {@return an entry recording which named layout is active, which is a
	 * layout in name only: it carries no state and is never restored.}
	 *
	 * <p>Reuses {@code displayName} to carry the active layout's identifier,
	 * the same way {@link #ofGroups} reuses {@code groups} for an unrelated
	 * catalog.</p>
	 *
	 * @param layoutIdentifier the active layout's identifier, or {@code null}
	 * when no named layout is active.
	 */
	public static PersistableLayout ofActiveLayout(
			final @Nullable String layoutIdentifier
	) {
		return new PersistableLayout(layoutIdentifier, List.of());
	}

	/**
	 * {@return this layout's state under a different name and group.}
	 *
	 * <p>What a rename and a move to another group both come down to. The state
	 * is carried over untouched, which is the point: the alternative is
	 * capturing the containers on screen, and those belong to whichever layout
	 * is showing rather than to the one being renamed.</p>
	 *
	 * @param displayName the name to store, or {@code null} to store none.
	 * @param group the group to store, or {@code null} for no group.
	 */
	public PersistableLayout withNaming(
			final @Nullable String displayName,
			final @Nullable String group
	) {
		return new PersistableLayout(displayName, bentoStates, group, groups);
	}

	/**
	 * {@return the layout's display name, if it has one.}
	 */
	public Optional<String> findDisplayName() {
		return Optional.ofNullable(displayName);
	}

	/**
	 * {@return the group the layout belongs to, if it belongs to one.}
	 */
	public Optional<String> findGroup() {
		return Optional.ofNullable(group);
	}
}
