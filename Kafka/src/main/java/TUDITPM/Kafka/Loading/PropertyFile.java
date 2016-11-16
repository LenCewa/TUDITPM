package TUDITPM.Kafka.Loading;

/**
 * List with all property files to load. Names must match the filename in the properties path.
 * 
 * @author Tobias Mahncke
 * @version 1.2
 */
public enum PropertyFile {
	credentials, database, kafka;

	/**
	 * Gets the corresponding filename including the path for the property.
	 * 
	 * @return The complete relative path.
	 */
	public String getFilename() {
		return "properties/" + name() + ".properties";
	}
}
