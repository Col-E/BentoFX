package software.coley.bentofx.persistence.core.api.provider;

/**
 * Common contract for persistence component providers that are discovered at
 * runtime.
 * <p>
 * Provider identifiers are stable names that applications can use to select a
 * specific provider when more than one implementation is available. When only
 * one provider of a given type is available, the default persistence provider
 * can select it automatically.
 *
 * <p>Extending this interface is what makes a component replaceable without a
 * code change: an application swaps the format it writes, or where it writes to,
 * by changing which implementation it depends on, and selects between several with
 * {@link software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile}. Only
 * {@link LayoutCodecProvider} and {@link LayoutStorageProvider} work this way -
 * every other provider in this package is supplied by the application directly.</p>
 *
 * <p><b>Implementing one.</b> Discovery uses {@code java.util.ServiceLoader}, so a
 * new codec or storage implementation is registered by declaring it in its own
 * {@code module-info} - {@code provides LayoutCodecProvider with MyCodecProvider;}
 * - and putting that module on the module path. Nothing else needs to change, and
 * no application code refers to the implementation class. This is the only place a
 * reader of this package needs the mechanism by name.</p>
 *
 * @author Phil Bryant
 */
public interface LayoutPersistenceComponentProvider {

	/**
	 * Returns the stable identifier for this provider.
	 *
	 * @return the stable provider identifier
	 */
	String getIdentifier();

	/**
	 * Indicates whether this provider should be preferred when more than one
	 * provider of the same type is available and the application has not
	 * explicitly selected one.
	 *
	 * @return {@code true} when this provider should be preferred by default
	 */
	default boolean isDefault() {
		return false;
	}
}
