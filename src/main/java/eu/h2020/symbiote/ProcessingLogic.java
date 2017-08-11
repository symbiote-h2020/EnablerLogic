package eu.h2020.symbiote;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;

/**
 * Processing Logic is interface that specific Enabler Logic needs to implement.
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
	
	/**
	 * This method is called when data from PlatfromProxy is received
	 * @param dataAppearedMessage
	 */
	void measurementReceived(EnablerLogicDataAppearedMessage dataAppearedMessage);
}
