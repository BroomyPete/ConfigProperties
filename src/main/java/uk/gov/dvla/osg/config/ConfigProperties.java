package uk.gov.dvla.osg.config;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;

/**
 * This is a utility class for loading java properties files and returning values
 * for the supplied keys. It acts as a wrapper around the 
 */
public class ConfigProperties {

	private final Logger LOGGER;

	private final Properties props = new Properties();
	private final String filename;

	/**
	 * Instantiates a new config properties object from the supplied properties file.
	 * When properties are missing, or values are of the wrong type, the logger 
	 * will output the name of the class that instatiates this one.
	 *
	 * @param file the properties file
	 * @param logger the logger to write to
	 */
	public ConfigProperties(File file, Logger logger) {
		this.LOGGER = logger;
		this.filename = file.getPath();
		
		try (InputStream input = new FileInputStream(file)) {
			props.load(input);
		} catch (IOException ioe) {
			ExceptionUtils.rethrow(ioe);
		}
	}

	/**
	 * Gets the name of the config file that was loaded. This is provided for logging purposes in case
	 * of any problems loading the properties.
	 *
	 * @return the file name
	 */
	public String getFileName() {
		return filename;
	}

	/**
	 * Gets the value associated with the property key as a string.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing.
	 * 
	 * @param key the key in the properties file
	 * @return the property value as a string
	 */
	public String getAsString(String key) {
		checkKeyExists(key);
		return props.getProperty(key);
	}
	
	/**
	 * Gets the value associated with the property key as a string converted to uppercase.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing.
	 *
	 * @param key the key in the properties file
	 * @return the property value as an upper case string
	 */
	public String getAsStringUpper(String key) {
		checkKeyExists(key);
		return props.getProperty(key).toUpperCase();
	}
	
	/**
	 * Gets the value associated with the property key as an int primitive type.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing
	 * or the value is not a valid number.
	 *
	 * @param key the key in the properties file
	 * @return the property value as an int primitive
	 */
	public int getAsInt(String key) {
		checkKeyExists(key);
		if (!valueIsNumeric(key)) {
			return 0;
		}
		return Integer.valueOf(props.getProperty(key));
	}

	/**
	 * Gets the value associated with the property key as an Integer object.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing
	 * or the value is not a valid number.
	 * 
	 * @param key the key in the properties file
	 * @return the property value as an Integer object or null if no value was assigned
	 */
	public Integer getAsInteger(String key) {
		checkKeyExists(key);
		if (StringUtils.isBlank(props.getProperty(key))) {
			return null;
		}
		if (!valueIsNumeric(key)) {
			return null;
		}
		return Integer.parseInt(props.getProperty(key));
	}
	
	/**
	 * Gets the value associated with the property key as a long primitive type.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing
	 * or the value is not a valid number.
	 * 
	 * @param key the property key
	 * @return the property value as a long primitive
	 */
	public long getAsLong(String key) {
		checkKeyExists(key);
		if (!valueIsNumeric(key)) {
			return 0;
		}
		
		return Long.valueOf(props.getProperty(key));
		
	}
	
	/**
	 * Gets the value associated with the property key as a boolean primitive type
	 * assuming that "Y" associates to true. To provide a different value for true
	 * use the {@link #getAsBool(String, String)} method.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing.
	 *
	 * @param key the property key
	 * @return true if property value equals "Y" (case insensitive)
	 */
	public boolean getAsBool(String key) {
		return getAsBool(key, "Y", null);
	}
	
	/**
	 * Gets the value associated with the property key as a boolean primitive type. This method
	 * returns true if the value of the property matches the flag.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing.
	 *
	 * @param key the property key
	 * @param flag the string value for true
	 * @return true if property value matches the flag (case insensitive)
	 */
	public boolean getAsBool(String key, String flag) {
		return getAsBool(key, flag, null);
	}
	
	/**
	 * Gets the value associated with the property key as a boolean primitive type. If the 
	 * property key is missing then the default value is returned, else this method
	 * returns true if the value of the property matches the flag.
	 * </br></br>
	 * Note:&nbsp&nbsp The property will be logged to the logger if the key is missing and defaultValue is null.
	 *
	 * @param key the property key
	 * @param flag the string value for true
	 * @param defaultValue the default value to return if key is not found
	 * @return true if key is found and property value equals matches the flag (case insensitive).
	 * If the property key is missing then the default value is returned.
	 */
	public boolean getAsBool(String key, String flag, Boolean defaultValue) {
		if (defaultValue != null) {
			if (!props.containsKey(key)) {
				return defaultValue;
			}
		} else {
			checkKeyExists(key);
		}
		
		return props.getProperty(key).equalsIgnoreCase(flag);
	}	
	
	/**
	 * Gets the property value as a Set of the enum type passed in.
	 *
	 * @param <E> the enum type
	 * @param enumClass the enum class to cast values to
	 * @param key the key to search on
	 * @return the set of values as instances of the enum type
	 */
	public <E extends Enum<E>> Set<E> getAsEnumSet(Class<E> enumClass, String key) {
		checkKeyExists(key);
        return Stream.of(props.getProperty(key).split(","))
        			 .map(String::trim)
        		     .map(s -> Enum.valueOf(enumClass, s))
        		     .collect(Collectors.toSet());
	}
	
	private void checkKeyExists(String key) {
		if (!props.containsKey(key)) {
			LOGGER.error("Property {} not found in file {}", key, filename);
			//System.exit(1);
		}
	}

	private boolean valueIsNumeric(String key) {
		String value = props.getProperty(key);
		if (!StringUtils.isNumeric(value)) {
			LOGGER.error("Property {} has value [{}] which is not an integer in file: {}", key, value, filename);
			return false;
		}
		return true;
	}

}
