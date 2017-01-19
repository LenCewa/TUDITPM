package TUDITPM.Spark.Loading;

/**
 * List with all property files to load. Names must match the filename in the properties path.
 * 
 * @author Tobias Mahncke
 * @author Yannick Pferr
 * @version 4.0
 */
public enum PropertyFile {
	credentials, database, kafka, spark, solr;

	/**
	 * Gets the corresponding filename including the path for the property.
	 * 
	 * @return The complete relative path.
	 */
	public String getFilename() {
		return "properties/" + name() + ".properties";
	}
}
