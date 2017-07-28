package eu.h2020.symbiote;

/**
 * Processing logic is interface the specific Enabler Logic needs to implement.
 * 
 * @author Mario Kusek
 *
 */
public interface ProcessingLogic {
	/**
	 * Initialization of processing logic.
	 * 
	 * @param enablerLogic reference to enabler logic
	 */
	void init(EnablerLogic enablerLogic);
	
	void measurementReceived();
}
