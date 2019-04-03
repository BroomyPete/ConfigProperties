package uk.gov.dvla.osg.config;

import java.io.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * This loads properties using a fluid builder api. Objects to assign values to
 * are passed into each method along with the property key. As Strings are immutable
 * StringBuilder objects are required. When properties are missing, 
 * or values are of the wrong type, they will be added to an errors list. 
 * Use {@link #errors()} as a terminating method to get the list of errors that have occurred.
 */
public class ConfigPropertiesBuilder {

	private static Set<String> errors = new HashSet<>();
	private Properties props;

	/**
	 * Instantiates a new config properties builder from the supplied properties
	 * file. 
	 *
	 * @param file   the properties file
	 */
	public static ConfigPropertiesBuilder load(File file) {
		return new ConfigPropertiesBuilder(file);
	}


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
	public ConfigPropertiesBuilder setAsInteger(AtomicInteger integer, String key) {
		if (valueIsNumeric(key)) {
			String value = props.getProperty(key);
			Integer parseInt = Integer.valueOf(value);
            integer.set(parseInt);
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
	public ConfigPropertiesBuilder setAsLong(AtomicLong l, String key) {
		if (valueIsNumeric(key)) {
			String value = props.getProperty(key);
            Long parseLong = Long.valueOf(value);
            l.set(parseLong);
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
	public ConfigPropertiesBuilder setAsBoolean(AtomicBoolean bool, String key) {
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
	public ConfigPropertiesBuilder setAsBool(AtomicBoolean bool, String key, String flag) {
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
	public ConfigPropertiesBuilder setAsBool(AtomicBoolean bool, String key, String flag, Boolean defaultValue) {
		if (defaultValue != null && !props.containsKey(key)) {
			bool.set(defaultValue);
			return this;
		}
		if (keyExists(key)) {
			String value = props.getProperty(key);
            bool.set(value.equalsIgnoreCase(flag));
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
		if (keyExists(key)) {
			try {
					enumSet.addAll(Stream.of(props.getProperty(key).split(","))
							.map(String::trim)
							.map(s -> Enum.valueOf(enumClass, s))
							.collect(Collectors.toSet()));
			} catch (IllegalArgumentException  ex) {
				errors.add(key + " : Contains an ilegal enum value");
			}
		}
		return this;
	}

	/**
	 * Gets the list of errors that occured. This is a terminal operation.
	 *
	 * @return the set of errors or an empty set if none have occurred.
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
