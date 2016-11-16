package TUDITPM.Kafka.Loading;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Helper class that loads all property files declared in {@link PropertyFile}
 * into java property objects.
 * 
 * @author Tobias Mahncke
 * @version 1.1
 */
public class PropertyLoader {
	private static HashMap<PropertyFile, Properties> propertyMap = new HashMap<>();

	/**
	 * Singleton implementation to retrieve the property object with the given
	 * name.
	 * 
	 * @param propertyFile
	 *            - name of the property object to return.
	 * @return The searched property object or <code>null</code> if the object
	 *         does not exist.
	 * @throws IOException
	 *             if the file could not be loaded.
	 */
	public static Properties getProperties(PropertyFile propertyFile)
			throws IOException {
		// If the property was not yet loaded, load it from the file
		if (propertyMap.get(propertyFile) == null) {
			Properties properties = new Properties();
			FileInputStream stream;
			stream = new FileInputStream(propertyFile.getFilename());
			properties.load(stream);
			stream.close();
			propertyMap.put(propertyFile, properties);
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
	 * @throws IOException
	 *             if the file could not be loaded.
	 */
	public static String getPropertyValue(PropertyFile propertyFile, String key)
			throws IOException {
		return getProperties(propertyFile).getProperty(key);
	}
}