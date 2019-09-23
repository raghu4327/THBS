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
import com.mentor.ems.common.types.JobStatusType;

/**
 * This controlled read the declined installed item with reason code product
 * transition
 * 
 * @author avijeetk
 *
 */
@Controller
@RequestMapping(value = "/installeditembatchjob")
public class InstalledItemDeletionBatchController {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstalledItemDeletionBatchController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("installedItemDeletionJob")
	Job job;

	@Resource(name = "BEACONProperties")
	private Properties properties;

	/**
	 * Submit method for deletion job
	 * 
	 * @return
	 */
	@RequestMapping("/submit")
	public ResponseEntity<JobStatusDTO> submit() {
		JobStatusDTO jobStatusDTO;
		LOGGER.info("Submit request received for Installed Item Deletion - Batch processing:...");

		Map<String, JobParameter> parameterMap = new HashMap<>();

		JobParameter timeParam = new JobParameter(System.currentTimeMillis());
		parameterMap.put(EMSCommonConstants.BEACON_JOB_TIME, timeParam);

		JobParameter sqlQueryRawParam = new JobParameter(getInstalledItemSQLQuery());
		parameterMap.put(EMSCommonConstants.BEACON_VIEW_INSTALLED_SQL_QUERY_RAW, sqlQueryRawParam);

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
		LOGGER.info("Installed Item Deletion - Batch processing Job request submitted successfully.");
		LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
	}

	private String getInstalledItemSQLQuery() {
		StringBuilder sqlQueryRaw = new StringBuilder(getSelectClause()).append(" FROM ").append(getSchemaName())
				.append(".").append(EMSCommonConstants.PRODUCT_ENT_DTL_T).append(getWHEREClause());
		LOGGER.warn("getInstalledItemSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
		return sqlQueryRaw.toString();
	}

	private String getSelectClause() {
		return EMSCommonConstants.SQL_INSTALLED_ITEM_SELECT_VIEW;
	}

	private String getWHEREClause() {
		return " WHERE " + EMSCommonConstants.DECLINED_INSTALLED_ITEM_WHERE_CLAUSE;
	}

	private String getSchemaName() {
		return getProperty(EMSCommonConstants.BEACON_SYSTEM_SCHEMA_NAME);
	}

	private String getProperty(String prop) {
		return properties.getProperty(prop);
	}

}
