package com.mentor.ems.web.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.slf4j.Logger;
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

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dto.JobStatusDTO;
import com.mentor.ems.common.solr.constants.SolrConstants;
import com.mentor.ems.common.types.JobStatusType;
import com.mentor.ems.services.ProductBatchLauncher;

@Controller
@RequestMapping(value = "/search/product/sync")
public class SolrProductBatchController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrProductBatchController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("productIndexJob")
	Job job;

	@Resource(name = SolrConstants.BEACON_SYSTEM_COMMON_PROPERTIES)
	private Properties properties;

	@Autowired
	private ProductBatchLauncher productBatchLauncher;

	@RequestMapping("/submit")
	public ResponseEntity<JobStatusDTO> submit() {
		JobStatusDTO jobStatusDTO;
		LOGGER.info("Submit request received for Solr Product Synchronization - Batch processing:...");

		Map<String, JobParameter> parameterMap = new HashMap<>();

		JobParameter timeParam = new JobParameter(System.currentTimeMillis());
		parameterMap.put(SolrConstants.BEACON_JOB_TIME, timeParam);

		JobParameter sqlQueryRawParam = new JobParameter(getViewProductRawSQLQuery());
		parameterMap.put(SolrConstants.BEACON_VIEW_PRODUCT_SQL_QUERY_RAW, sqlQueryRawParam);

		JobParameters jobParams = new JobParameters(parameterMap);

		LOGGER.info("Job Parameters = " + jobParams);
		JobExecution jobExecution = null;

		try {	
			jobExecution = jobLauncher.run(job, jobParams);
			jobStatusDTO = new JobStatusDTO(jobExecution.getStatus().toString(),
					jobExecution.getExitStatus().getExitCode() + "|"
							+ jobExecution.getExitStatus().getExitDescription());			
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			jobStatusDTO = new JobStatusDTO(JobStatusType.FAILED.getStatus(), e.getMessage());
			LOGGER.error("Exception occurred during submitting the Job ", e);
			LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		}
		LOGGER.info("Solr Product Synchronization - Batch processing Job request submitted successfully.");
		LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
	}

	private String getViewProductRawSQLQuery() {
		String sqlQueryRaw = getSelectClause() + " FROM " + getSchemaName() + "."
				+ EMSCommonConstants.PRODUCT_INFO_TABLE + " PART " + " LEFT OUTER JOIN " + getSchemaName() + "."
				+ EMSCommonConstants.SOURCE_SYSTEM_TABLE + " SOURCESYS " + getJOINClause() + getWHEREClause();
		LOGGER.warn("getViewProductRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
		return sqlQueryRaw;
	}

	private String getSelectClause() {
		return SolrConstants.SQL_PRODUCT_SELECT_VIEW;
	}

	private String getWHEREClause() {
		return " WHERE " + SolrConstants.BEACON_PART_TYPE_WHERE_CLAUSE;
	}

	private String getJOINClause() {
		return " ON " + SolrConstants.BEACON_PART_SOURCESYSTEM_JOIN_CLAUSE;
	}

	private String getSchemaName() {
		return getProperty(SolrConstants.BEACON_SYSTEM_SCHEMA_NAME);
	}

	private String getProperty(String prop) {
		return properties.getProperty(prop);
	}

	@RequestMapping("/ongoing")
	public ResponseEntity ongoingProductBatch() {
		productBatchLauncher.submitBatchRequest();
		return ResponseEntity.accepted().build();
	}

}
