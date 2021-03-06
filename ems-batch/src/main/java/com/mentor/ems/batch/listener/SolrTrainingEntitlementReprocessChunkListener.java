package com.mentor.ems.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.jobs.EMSJobDAO;
import com.mentor.ems.common.exception.DataAccessException;

/**
 * Listener class to count the failed job
 * 
 * @author avijeetk
 *
 */
public class SolrTrainingEntitlementReprocessChunkListener implements JobExecutionListener {
	
	@Autowired
	private EMSJobDAO emsJobDAO;
	

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrTrainingEntitlementReprocessChunkListener.class);

	@Override
	public void beforeJob(JobExecution jobExecution) {
		LOGGER.info("Before job start");
		
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		LOGGER.info("After job completion");
		int count;
		try {
			count = emsJobDAO.updateFailedJobStatus(EMSCommonConstants.SOLR_TRAINING_ENTITLEMENT_REPROCESSING_TYPE_JOB);
			LOGGER.info("Updated failed record count is "+count);
		} catch (DataAccessException e) {
			LOGGER.error("Exception occured in OrderChunkListener",e);
		}
		
	}
	
	

}

