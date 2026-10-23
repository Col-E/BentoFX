package software.coley.bentofx.persistence.core.api;

import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.impl.DefaultBentoStateCapturer;

import java.util.List;
import java.util.Objects;

/**
 * Captures the current state of every available {@code Bento} as a list of
 * serializable {@link BentoState} records.
 *
 * <p>The walker side of the save pipeline, extracted as a public API so a
 * custom {@link software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider}
 * can reuse it. The default {@link LayoutSaver} chains this with a
 * {@link software.coley.bentofx.persistence.core.api.codec.LayoutCodec} and a
 * {@link software.coley.bentofx.persistence.core.api.storage.LayoutStorage}; a
 * provider that wants structured relational storage calls {@link #capture()}
 * directly and decomposes the result into its own schema. See {@link
 * PersistableLayout} for the record a codec would normally wrap this output
 * in before encoding.</p>
 *
 * <p>JavaFX Application Thread only. {@link #capture()} reads container tree,
 * scene, and stage state that must be touched on the FX thread. The default
 * {@link LayoutSaver} runs it on the FX thread for you; a caller using this
 * interface directly is responsible for the same.</p>
 *
 * @author Phil Bryant
 */
public interface BentoStateCapturer {

	/**
	 * {@return a {@link BentoStateCapturer} backed by the framework's default
	 * implementation.}
	 *
	 * @param bentoProvider supplies the {@code Bento} instances to walk.
	 * @throws NullPointerException when {@code bentoProvider} is {@code null}.
	 */
	static BentoStateCapturer create(final BentoProvider bentoProvider) {
		return new DefaultBentoStateCapturer(
				Objects.requireNonNull(bentoProvider, "bentoProvider")
		);
	}

	/**
	 * {@return the captured state of every available {@code Bento}, in the order
	 * the {@link BentoProvider} reports them.}
	 *
	 * <p>Each {@link BentoState} carries the identifier of its {@code Bento},
	 * its root branches, and the {@code DragDropStage}s whose scene root is a root
	 * branch of that {@code Bento}. Must be called on the JavaFX Application
	 * Thread.</p>
	 */
	List<BentoState> capture();
}
