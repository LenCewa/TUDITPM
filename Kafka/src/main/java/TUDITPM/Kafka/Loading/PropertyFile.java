package TUDITPM.Kafka.Loading;

/**
 * List with all property files to load.
 * 
 * @author Tobias Mahncke
 * @version 1.1
 */
public enum PropertyFile {
	credentials, database;

	/**
	 * Gets the corresponding filename including the path for the property.
	 * 
	 * @return The complete relative path.
	 */
	public String getFilename() {
		return "properties/" + name() + ".properties";
	}
}
