package com.mentor.ems.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import com.mentor.ems.batch.custom.reader.TrainingEntitlementDeltaItemReader;

/**
 * @author skasiram
 *
 */
public class TrainingEntitlementDeltaJobListener implements JobExecutionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(TrainingEntitlementDeltaJobListener.class);
	@Override
	public void beforeJob(JobExecution jobExecution) {
		LOGGER.info("Before TrainingEntitlementDeltaItemReader EntitlementKey to Null");
		TrainingEntitlementDeltaItemReader.setTrainingEntitlementKey(null);
		TrainingEntitlementDeltaItemReader.setOrderDetailStartKey(null);
		
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		LOGGER.info("After TrainingEntitlementDeltaItemReader EntitlementKey to Null");
		TrainingEntitlementDeltaItemReader.setTrainingEntitlementKey(null);
		TrainingEntitlementDeltaItemReader.setOrderDetailStartKey(null);
	}

}
