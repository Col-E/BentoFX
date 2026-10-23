package software.coley.bentofx.persistence.core.api;

/**
 * Outputs a BentoFX docking layout for persistence.
 *
 * @author Phil Bryant
 */
public interface LayoutSaver extends AutoCloseable {

	/**
	 * Saves the current docking layout.
	 *
	 * <p>Capturing the layout requires the JavaFX application thread, so this
	 * must be called while the JavaFX toolkit is still running. Call it before
	 * {@code Platform.exit()}, not from a shutdown hook that runs afterwards:
	 * once the toolkit has stopped, work handed to it is accepted and never
	 * run.</p>
	 *
	 * <p><b>Only attached containers are saved.</b> A capture sees a
	 * {@code DockContainerRootBranch} only once that branch has a {@code Scene},
	 * because that is when it registers itself with its {@code Bento}. Containers
	 * handed back by {@link LayoutRestorer#restoreLayout} arrive unattached, so a
	 * save taken between restoring a layout and placing it finds nothing.
	 * Implementations are expected not to overwrite a persisted layout with an
	 * empty capture, but what survives is then the <em>older</em> layout - attach
	 * before saving. See {@link BentoLayout#getRootBranches()}.</p>
	 *
	 * @throws BentoStateException when the layout cannot be saved.
	 */
	void saveLayout() throws BentoStateException;

	/**
	 * Releases resources held by this saver, saving the layout first when the
	 * implementation supports it.
	 *
	 * <p>Close the saver <em>before</em> shutting the JavaFX toolkit down. An
	 * implementation that saves here needs the JavaFX application thread to
	 * capture the layout, and after {@code Platform.exit()} that thread will
	 * never run the request. Implementations bound their wait, so a misordered
	 * shutdown costs the final save rather than hanging, but the save is still
	 * lost.</p>
	 */
	@Override
	default void close() {
		// Default no-op. Implementations that own resources should override.
	}
}
