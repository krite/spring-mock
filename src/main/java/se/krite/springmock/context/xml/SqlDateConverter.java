package se.krite.springmock.context.xml;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.util.ThreadSafeSimpleDateFormat;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Converts a java.util.Date to a String as a date format,
 * retaining precision down to milliseconds.
 * <p/>
 * Extended version supporting sql TimeStamp
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 * @author kristoffer.teuber
 */
public class SqlDateConverter extends AbstractSingleValueConverter {

	private final ThreadSafeSimpleDateFormat defaultFormat;
	private final ThreadSafeSimpleDateFormat[] acceptableFormats;

	/**
	 * Construct a DateConverter with standard formats and lenient set off.
	 */
	public SqlDateConverter() {
		this(false);
	}

	/**
	 * Construct a DateConverter with standard formats.
	 *
	 * @param lenient the lenient setting of {@link java.text.SimpleDateFormat#setLenient(boolean)}
	 * @since 1.3
	 */
	public SqlDateConverter(boolean lenient) {
		this("yyyy-MM-dd HH:mm:ss.S z",
				new String[]{
						"yyyy-MM-dd HH:mm:ss.S a",
						"yyyy-MM-dd HH:mm:ssz", "yyyy-MM-dd HH:mm:ss z", // JDK 1.3 needs both versions
						"yyyy-MM-dd HH:mm:ssa"},  // backwards compatibility
				lenient);
	}

	/**
	 * Construct a DateConverter with lenient set off.
	 *
	 * @param defaultFormat	 the default format
	 * @param acceptableFormats fallback formats
	 */
	public SqlDateConverter(String defaultFormat, String[] acceptableFormats) {
		this(defaultFormat, acceptableFormats, false);
	}

	/**
	 * Construct a DateConverter.
	 *
	 * @param defaultFormat	 the default format
	 * @param acceptableFormats fallback formats
	 * @param lenient		   the lenient setting of {@link java.text.SimpleDateFormat#setLenient(boolean)}
	 * @since 1.3
	 */
	public SqlDateConverter(String defaultFormat, String[] acceptableFormats, boolean lenient) {
		this.defaultFormat = new ThreadSafeSimpleDateFormat(defaultFormat, 4, 20, lenient);
		this.acceptableFormats = new ThreadSafeSimpleDateFormat[acceptableFormats.length];
		for (int i = 0; i < acceptableFormats.length; i++) {
			this.acceptableFormats[i] = new ThreadSafeSimpleDateFormat(acceptableFormats[i], 1, 20, lenient);
		}
	}

	public boolean canConvert(Class type) {
		return type.equals(Date.class) || type.equals(Timestamp.class);
	}

	public Object fromString(String str) {
		try {
			return defaultFormat.parse(str);
		} catch (ParseException e) {
			for (int i = 0; i < acceptableFormats.length; i++) {
				try {
					return acceptableFormats[i].parse(str);
				} catch (ParseException e2) {
					// no worries, let's try the next format.
				}
			}
			// no dateFormats left to try
			throw new ConversionException("Cannot parse date " + str);
		}
	}

	public String toString(Object obj) {
		if (obj instanceof Timestamp) {
			obj = new Date(((Date) obj).getTime());
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		return sdf.format(obj);
	}
}
