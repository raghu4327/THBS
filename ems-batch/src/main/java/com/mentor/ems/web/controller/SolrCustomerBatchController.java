package com.mentor.ems.web.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;

import javax.annotation.Resource;

import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mentor.ems.common.dto.JobStatusDTO;
import com.mentor.ems.common.solr.constants.SolrConstants;
import com.mentor.ems.common.types.JobStatusType;

/**
 * Created by rathinat on 6/1/17.
 * Controller class - Defined end point for Solr Customer load or synchronization
 */

@Controller
@RequestMapping(value = "/search/customer/sync")
public class SolrCustomerBatchController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrCustomerBatchController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("SOLRCustomerIndexJobFull")
	Job job;

	@Resource(name=SolrConstants.BEACON_SYSTEM_COMMON_PROPERTIES)
	private Properties properties;


	@RequestMapping("/submit")
	public ResponseEntity<JobStatusDTO> submit() {
		JobStatusDTO jobStatusDTO;
				LOGGER.info("Submit request received for Solr Customer Synchronization - Batch processing:...");

		Map<String,JobParameter> parameterMap = new HashMap<>();

		JobParameter timeParam = new JobParameter(System.currentTimeMillis());
		parameterMap.put( SolrConstants.BEACON_JOB_TIME, timeParam);

		JobParameter sqlQueryRawParam = new JobParameter(getViewEntitlementRawSQLQuery( ));
		parameterMap.put( SolrConstants.BEACON_VIEW_CUSTOMER_SQL_QUERY_RAW,
				sqlQueryRawParam);

		JobParameters jobParams = new JobParameters(parameterMap);

		LOGGER.info("Job Parameters = " + jobParams);
		JobExecution jobExecution = null;
		try {
			jobExecution = jobLauncher.run(job, jobParams);
			jobStatusDTO = new JobStatusDTO(jobExecution.getStatus().toString(),jobExecution.getExitStatus().getExitCode() + "|" +
					jobExecution.getExitStatus().getExitDescription());
		} catch (JobExecutionAlreadyRunningException |JobRestartException |JobInstanceAlreadyCompleteException |JobParametersInvalidException e) {
			jobStatusDTO = new JobStatusDTO(JobStatusType.FAILED.getStatus(), e.getMessage());
			LOGGER.error("Exception occurred during submitting the Job",e);
			LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		}
		LOGGER.info("Solr Customer Synchronization - Batch processing Job request submitted successfully.");
		LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
	}

	private String getViewEntitlementRawSQLQuery() {
		String sqlQueryRaw = getSelectClause() + " FROM " + getSchemaName() + "." + SolrConstants.BEACON_CUSTOMER_DETAILS_VW;
		LOGGER.warn("getViewEntitlementRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
		return sqlQueryRaw;
	}

	private String getSelectClause() {
		return SolrConstants.SQL_SELECT_CUSTOMER_VIEW;
	}

	private String getSchemaName() {
		return getProperty(SolrConstants.BEACON_SYSTEM_SCHEMA_NAME);
	}

	private String getProperty(String prop) {
		return properties.getProperty(prop);
	}

	
}
