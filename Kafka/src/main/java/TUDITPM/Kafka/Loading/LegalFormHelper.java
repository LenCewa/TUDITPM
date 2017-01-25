package TUDITPM.Kafka.Loading;

import java.util.LinkedList;

public class LegalFormHelper {

	/**
	 * Removes the legal forms of every Company from the list
	 * 
	 * @param companies
	 *            - list of companies
	 * @param legalForms
	 *            - list of legal forms possible
	 * @return - list of companies with their legal form removed
	 */
	public static LinkedList<String[]> removeLegalForms(LinkedList<String> companies,
			LinkedList<String> legalForms) {
		LinkedList<String[]> removed = new LinkedList<>();

		for (String company : companies) {
			String[] withAndStripped = {company, null};
			for (String legalForm : legalForms) {
				company = company.replace(legalForm, "").trim();
			}
			withAndStripped[1] = company;
			removed.add(withAndStripped);
		}

		return removed;
	}
}
