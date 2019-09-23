/**
 * 
 */
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dto.JobStatusDTO;
import com.mentor.ems.common.types.JobStatusType;

/**
 * @author avawasth
 *
 */
@RestController
@RequestMapping(value = "/csed")
public class SiteCoterminousSupportController {
	private static final Logger LOGGER = LoggerFactory.getLogger(SiteCoterminousSupportController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("modifycsedJob")
	Job job;

	@Resource(name = EMSCommonConstants.BEACON_PROPERTIES)
	private Properties properties;

	@RequestMapping(value = "/modify")
	public ResponseEntity<JobStatusDTO> submit() {

		JobStatusDTO jobStatusDTO;
		LOGGER.info("Modify request received for Coterminous Support End Date (CSED) - Batch processing:...");
		Map<String, JobParameter> parameterMap = new HashMap<>();
		JobParameter timeParam = new JobParameter(System.currentTimeMillis());
		parameterMap.put(EMSCommonConstants.BEACON_JOB_TIME, timeParam);

		JobParameter sqlQueryRawParam = new JobParameter(getViewProductRawSQLQuery());
		parameterMap.put(EMSCommonConstants.BEACON_PRODUCT_ENT_SQL_QUERY_RAW, sqlQueryRawParam);
		JobParameters jobParams = new JobParameters(parameterMap);
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
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		}
		LOGGER.info(" Exit from SiteCoterminousSupportController -submit method - Batch processing:...");
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
	}

	private String getViewProductRawSQLQuery() {

		StringBuilder sqlQueryRaw = new StringBuilder(getSelectClause()).append(" FROM ").append(getSchemaName())
				.append(".").append(EMSCommonConstants.PRODUCT_ENT_DTL_INFO_TABLE)
				.append(EMSCommonConstants.CSED_JOIN_QUERY).append(getWHEREClause())
				.append(EMSCommonConstants.ORDER_BY_CLAUSE_SITE);
		LOGGER.info("getViewProductRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
		return sqlQueryRaw.toString();
	}

	private String getSelectClause() {
		return EMSCommonConstants.SQL_PRODUCT_ENT_DTL_SELECT_VIEW;
	}

	private String getSchemaName() {
		return getProperty(EMSCommonConstants.BEACON_SYSTEM_SCHEMA_NAME);
	}

	private String getProperty(String prop) {
		return properties.getProperty(prop);
	}

	private String getWHEREClause() {
		return " WHERE " + EMSCommonConstants.CSED_WHERE_CLAUSE;
	}

}
