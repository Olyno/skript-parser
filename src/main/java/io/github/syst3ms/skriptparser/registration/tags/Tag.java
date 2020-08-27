package io.github.syst3ms.skriptparser.registration.tags;

import java.util.function.Supplier;

/**
 * Tags are elements one can use inside of strings to change parts of that string easily.
 * Some examples of valid tags and their behaviour:
 * <ul>
 *     <li>{@code <yellow>, <reset>}: singleton tags, no parameters</li>
 *     <li>{@code <case=uppercase>, <color=#ffffff}: parameter tags, one parameter</li>
 *     <li>{@code <link=text,link.to.my/image.png>}: plural tags, multiple parameters</li>
 *     <li>{@code &r}: short tags, no parameters</li>
 * </ul>
 * Note that you can use a backslash ('\') to escape characters inside tags.
 * @see io.github.syst3ms.skriptparser.tags.TagReset TagReset
 * @see io.github.syst3ms.skriptparser.tags.TagCase TagCase
 * @author Mwexim
 */
public interface Tag {

	/**
	 * Initialises this Tag before being used. This method is always called before all the others in
	 * an extending class.
	 * @param key the key that was matched. Note that this can be a single character used in a short tag
	 *            (like {@code &r}).
	 * @param parameters an array of strings representing all the parameters that are being passed
	 *                   to this syntax element. Elements of this array can't be {@code null} or {@link String#isEmpty() empty}.
	 * @return {@code true} if the tag was initialized successfully, {@code false} otherwise.
	 * A good practice is to check for the array length of the parameters to not interfere with other addons.
	 */
	boolean init(String key, String[] parameters);

	/**
	 * Returns a string applied by this tag.
	 * @param affected the string this tags affects.
	 * @return the applied string
	 */
	String getValue(String affected);

	String toString(boolean debug);

	/**
	 * If a tag is occasional, it can only be applied to the affected string
	 * when a developer specifically calls it in an occasional state.
	 * @return whether or not this tag is occasional, default {@code false}
	 * 
	 * @see TagManager#occasionally(Supplier)
	 */
	default boolean isOccasional() {
		return false;
	}
}
