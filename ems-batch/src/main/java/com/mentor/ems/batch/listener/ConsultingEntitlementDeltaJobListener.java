package com.mentor.ems.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import com.mentor.ems.batch.custom.reader.ConsultingEntitlementDeltaItemReader;

/**
 * @author skasiram
 *
 */
public class ConsultingEntitlementDeltaJobListener implements JobExecutionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsultingEntitlementDeltaJobListener.class);
	@Override
	public void beforeJob(JobExecution jobExecution) {
		LOGGER.info("Updating ConsultingEntitlementDeltaJobListener EntitlementKey to Null");
		ConsultingEntitlementDeltaItemReader.setConsultingEntitlementKey(null);
		ConsultingEntitlementDeltaItemReader.setOrderDetailStartKey(null);
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.JobExecutionListener#afterJob(org.springframework.batch.core.JobExecution)
	 */
	@Override
	public void afterJob(JobExecution jobExecution) {
		LOGGER.info("Updating ConsultingEntitlementDeltaJobListener EntitlementKey to Null");
		ConsultingEntitlementDeltaItemReader.setConsultingEntitlementKey(null);
		ConsultingEntitlementDeltaItemReader.setOrderDetailStartKey(null);
	}

}
