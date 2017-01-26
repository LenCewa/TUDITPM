package TUDITPM.Kafka.Loading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Helper class that loads all property files declared in {@link PropertyFile}
 * into java property objects.
 * 
 * @author Tobias Mahncke
 * @author Yannick Pferr
 * @version 5.0
 */
public class PropertyLoader {
	private static HashMap<PropertyFile, Properties> propertyMap = new HashMap<PropertyFile, Properties>();
	private static LinkedList<String> legalForms = new LinkedList<String>();
	private static boolean loaded = false;

	/**
	 * Loads all files defined in {@link PropertyFile} into a map to retrieve
	 * the values at a later time.
	 * 
	 * @throws IOException
	 */
	public PropertyLoader() throws IOException {
		for (PropertyFile file : PropertyFile.values()) {
			Properties properties = new Properties();
			FileInputStream stream;
			stream = new FileInputStream(file.getFilename());
			properties.load(stream);
			stream.close();
			propertyMap.put(file, properties);
		}

		loaded = true;

	}

	/**
	 * Singleton implementation to retrieve the property object with the given
	 * name.
	 * 
	 * @param propertyFile
	 *            - name of the property object to return.
	 * @return The searched property object or <code>null</code> if the object
	 *         does not exist.
	 */
	public static Properties getProperties(PropertyFile propertyFile) {
		// If the constructor was not called before using this method throw a
		// runtime exception -> developer mistake
		if (!loaded) {
			throw new RuntimeException("Property files where not loaded.");
		}
		return propertyMap.get(propertyFile);
	}

	/**
	 * Retrieves the given key from the property with the given name.
	 * 
	 * @param propertyFile
	 *            - name of the property object to look in.
	 * @param key
	 *            - name for the searched value.
	 * @return The searched value or <code>null</code> if the key does not exist
	 *         in the given property.
	 */
	public static String getPropertyValue(PropertyFile propertyFile, String key) {
		return getProperties(propertyFile).getProperty(key);
	}
}