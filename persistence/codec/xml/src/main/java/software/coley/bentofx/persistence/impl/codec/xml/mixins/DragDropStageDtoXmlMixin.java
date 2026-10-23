package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.stage.Modality;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerRootBranchDto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.ROOT_BRANCH_ELEMENT_NAME;

/**
 * Jackson XML mix-in for {@code DragDropStageDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class DragDropStageDtoXmlMixin {

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String title;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Double x;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Double y;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Double width;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Double height;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Modality modality;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Double opacity;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean iconified;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean fullScreen;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean maximized;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean alwaysOnTop;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean resizable;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean showing;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean focused;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean autoCloseWhenEmpty;

	@JacksonXmlProperty(localName = ROOT_BRANCH_ELEMENT_NAME)
	public @Nullable DockContainerRootBranchDto dockContainerRootBranchDto;
}
