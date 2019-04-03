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
 * are passed into each method along with the property key. As Strings are
 * immutable StringBuilder objects are required. When properties are missing, or
 * values are of the wrong type, they will be added to an errors list. Use
 * {@link #errors()} as a terminating method to get the list of errors that have
 * occurred.
 */
public class ConfigPropertiesBuilder {

    private static Set<String> errors = new HashSet<>();
    private Properties props;

    /**
     * Instantiates a new config properties builder from the supplied properties
     * file.
     *
     * @param file the properties file
     * @return the config properties builder
     * @throws IOException Signals that an I/O exception has occurred while loading
     *             the properties file.
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
     * Sets the String Builder to the value of the property matching the key. </br>
     * </br>
     * If the key is not present in the properties file it is logged to the errors
     * collection.
     *
     * @param sb the StringBuilder to set the value of
     * @param key the property key
     * @return the current instance of config properties builder
     */
    public ConfigPropertiesBuilder setAsString(StringBuilder sb, String key) {
        if (keyExists(key)) {
            sb.append(props.getProperty(key));
        }
        return this;
    }

    /**
     * Sets the String Builder to the value of the property matching the key
     * converted to uppper case. </br>
     * </br>
     * If the key is not present in the properties file it is logged to the errors
     * collection.
     *
     * @param sb the StringBuilder to set the value of
     * @param key the property key
     * @return the current instance of config properties builder
     */
    public ConfigPropertiesBuilder setAsStringUpper(StringBuilder sb, String key) {
        if (keyExists(key)) {
            sb.append(props.getProperty(key).toUpperCase());
        }
        return this;
    }

    /**
     * Sets the AtomicInteger to the value of the property matching the key
     * converted to uppper case. </br>
     * </br>
     * If the key is not present in the properties file, or if the value is not
     * numeric, then it is logged to the errors collection.
     *
     * @param integer the AtomicInteger to set the value of
     * @param key the property key
     * @return the current instance of config properties builder
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
     * Sets the AtomicLong to the value of the property matching the key converted
     * to uppper case. </br>
     * </br>
     * If the key is not present in the properties file, or if the value is not
     * numeric, then it is logged to the errors collection.
     *
     * @param l the AtomicLong to set the value of
     * @param key the property key
     * @return the current instance of config properties builder
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
     * Sets the value associated with the property key to the AtomicBoolean
     * parameter assuming that "Y" associates to true. To provide a different value
     * for true use the {@link #setAsBool(AtomicBoolean, String, String)} method.
     * </br>
     * </br>
     * If the key is not present in the properties file it is logged to the errors
     * collection.
     *
     * @param bool the AtomicBoolean to set
     * @param key the property key
     * @return the current instance of config properties builder
     */
    public ConfigPropertiesBuilder setAsBoolean(AtomicBoolean bool, String key) {
        setAsBool(bool, key, "Y");
        return this;
    }

    /**
     * Sets the value associated with the property key to the AtomicBoolean
     * parameter. This method sets the AtomicBoolean parameter to true if the value
     * of the property matches the flag (case insensitive). </br>
     * </br>
     * If the key is not present in the properties file it is logged to the errors
     * collection.
     *
     * @param bool the AtomicBoolean to set
     * @param key the property key
     * @param flag the string value for true
     * @return the current instance of config properties builder
     */
    public ConfigPropertiesBuilder setAsBool(AtomicBoolean bool, String key, String flag) {
        setAsBool(bool, key, flag, null);
        return this;
    }

    /**
     * Sets the value associated with the property key to the AtomicBoolean
     * parameter. This method sets the AtomicBoolean parameter to true if the value
     * of the property matches the flag (case insensitive). If the property key is missing then the
     * AtomicBoolean is set to the defaultValue. </br>
     * </br>
     * They key is logged to the errors collection if it is not present in the
     * properties file and the default value is set to null.
     * 
     * @param bool the AtomicBoolean to set
     * @param key the property key
     * @param flag the string value for true
     * @param defaultValue the default value to return if key is not found
     * @return the current instance of config properties builder
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
     * @param <E> the enum type
     * @param enumClass the enum class to cast values to
     * @param key the key to search on
     * @return the set of values as instances of the enum type
     */
    public <E extends Enum<E>> ConfigPropertiesBuilder getAsEnumSet(Set<E> enumSet, Class<E> enumClass, String key) {
        if (keyExists(key)) {
            try {
                enumSet.addAll(
                               Stream.of(props.getProperty(key).split(","))
                                     .map(String::trim)
                                     .map(s -> Enum.valueOf(enumClass, s))
                                     .collect(Collectors.toSet())
                               );
            } catch (IllegalArgumentException ex) {
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
