package uk.gov.dvla.osg.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ConfigPropertiesBuilder {

	private static Set<String> errors = new HashSet<>();
	private Properties props;

	public static ConfigPropertiesBuilder load(File file) {
		return new ConfigPropertiesBuilder(file);
	}

	/**
	 * Instantiates a new config properties object from the supplied properties
	 * file. When properties are missing, or values are of the wrong type, the
	 * logger will output the name of the class that instatiates this one.
	 *
	 * @param file   the properties file
	 * @param logger the logger to write to
	 */
	public ConfigPropertiesBuilder(File file) {
		try (InputStream input = new FileInputStream(file)) {
			props.load(input);
		} catch (IOException ioe) {
			ExceptionUtils.rethrow(ioe);
		}
	}

	/**
	 * Gets the value associated with the property key as a string. </br>
	 * </br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is
	 * missing.
	 * 
	 * @param key the key in the properties file
	 * @return the property value as a string
	 */
	public ConfigPropertiesBuilder setAsString(StringBuilder sb, String key) {
		if (keyExists(key)) {
			sb.append(props.getProperty(key));
		}
		return this;
	}

	/**
	 * Gets the value associated with the property key as a string converted to
	 * uppercase. </br>
	 * </br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is
	 * missing.
	 *
	 * @param key the key in the properties file
	 * @return the property value as an upper case string
	 */
	public ConfigPropertiesBuilder setAsStringUpper(StringBuilder sb, String key) {
		if (keyExists(key)) {
			sb.append(props.getProperty(key).toUpperCase());
		}
		return this;
	}

	/**
	 * Gets the value associated with the property key as an Integer object. </br>
	 * </br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is
	 * missing or the value is not a valid number.
	 * 
	 * @param key the key in the properties file
	 * @return the property value as an Integer object or null if no value was
	 *         assigned
	 */
	public ConfigPropertiesBuilder setAsInteger(Integer integer, String key) {
		if (valueIsNumeric(key)) {
			integer = Integer.parseInt(props.getProperty(key));
		}
		return this;
	}

	/**
	 * Gets the value associated with the property key as a long primitive type.
	 * </br>
	 * </br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is
	 * missing or the value is not a valid number.
	 * 
	 * @param key the property key
	 * @return the property value as a long primitive
	 */
	public ConfigPropertiesBuilder setAsLong(Long l, String key) {
		if (valueIsNumeric(key)) {
			l = Long.valueOf(props.getProperty(key));
		}
		return this;
	}

	/**
	 * Gets the value associated with the property key as a boolean primitive type
	 * assuming that "Y" associates to true. To provide a different value for true
	 * use the {@link #getAsBool(String, String)} method. </br>
	 * </br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is
	 * missing.
	 *
	 * @param key the property key
	 * @return true if property value equals "Y" (case insensitive)
	 */
	public ConfigPropertiesBuilder setAsBoolean(Boolean bool, String key) {
		setAsBool(bool, key, "Y");
		return this;
	}

	/**
	 * Gets the value associated with the property key as a boolean primitive type.
	 * This method returns true if the value of the property matches the flag. </br>
	 * </br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is
	 * missing.
	 *
	 * @param key  the property key
	 * @param flag the string value for true
	 * @return true if property value matches the flag (case insensitive)
	 */
	public ConfigPropertiesBuilder setAsBool(Boolean bool, String key, String flag) {
		setAsBool(bool, key, flag, null);
		return this;
	}

	/**
	 * Gets the value associated with the property key as a boolean primitive type.
	 * If the property key is missing then the default value is returned, else this
	 * method returns true if the value of the property matches the flag. </br>
	 * </br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is
	 * missing and defaultValue is null.
	 *
	 * @param key          the property key
	 * @param flag         the string value for true
	 * @param defaultValue the default value to return if key is not found
	 * @return true if key is found and property value equals matches the flag (case
	 *         insensitive). If the property key is missing then the default value
	 *         is returned.
	 */
	public ConfigPropertiesBuilder setAsBool(Boolean bool, String key, String flag, Boolean defaultValue) {
		if (defaultValue != null && !props.containsKey(key)) {
			bool = defaultValue;
			return this;
		}
		if (keyExists(key)) {
			bool = props.getProperty(key).equalsIgnoreCase(flag);
		}
		return this;
	}

	/**
	 * Gets the property value as a Set of the enum type passed in.
	 *
	 * @param           <E> the enum type
	 * @param enumClass the enum class to cast values to
	 * @param key       the key to search on
	 * @return the set of values as instances of the enum type
	 */
	public <E extends Enum<E>> ConfigPropertiesBuilder getAsEnumSet(Set<E> enumSet, Class<E> enumClass, String key) {
		try {
			if (keyExists(key)) {
				enumSet.addAll(Stream.of(props.getProperty(key).split(",")).map(String::trim)
						.map(s -> Enum.valueOf(enumClass, s)).collect(Collectors.toSet()));
			}
		} catch (IllegalArgumentException  ex) {
			errors.add(key + " : Contains an ilegal enum value");
		}
		return this;
	}

	/**
	 * Gets the list of errors that occured. This is a terminal operation.
	 *
	 * @return the sets the
	 */
	public Set<String> errors() {
		return this.errors();
	}

	private boolean keyExists(String key) {
		if (!props.containsKey(key)) {
			errors.add(key + " : Does not exist in config file");
			return false;
		}
		return true;
	}

	private boolean valueIsNumeric(String key) {
		if (!keyExists(key)) {
			return false;
		}
		if (!StringUtils.isNumeric(props.getProperty(key))) {
			errors.add(key + " : Is not a valid number");
			return false;
		}
		return true;
	}

}
