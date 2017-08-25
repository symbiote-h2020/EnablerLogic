package eu.h2020.symbiote;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;

/**
 * Processing Logic is interface that specific Enabler Logic needs to implement.
 * 
 * Those methods are called when some event occurred (e.g. enabler is starting or 
 * message received from other components). 
 * 
 * @author Mario Kusek
 *
 */
public interface ProcessingLogic {
	/**
	 * Initialization of processing logic. It is called when enabler is started.
	 * 
	 * @param enablerLogic reference to EnablerLogic so messages to other components can be send
	 */
	void init(EnablerLogic enablerLogic);
	
	/**
	 * This method is called when data from Platform Proxy component is received.
	 * 
	 * @param dataAppearedMessage data from Platform Proxy component
	 */
	void measurementReceived(EnablerLogicDataAppearedMessage dataAppearedMessage);
}
