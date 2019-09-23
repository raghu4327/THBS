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



@Controller
@RequestMapping(value = "/autorenewal/ELD/notification")
public class ELDNotificationBatchController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ELDNotificationBatchController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("eldNotificationRenewalJob")
	Job job;

		@Resource(name = EMSCommonConstants.BEACON_PROPERTIES)
		private Properties properties;

		@RequestMapping(value = "/submit")
		public ResponseEntity<JobStatusDTO> submit() {

			JobStatusDTO jobStatusDTO;
			LOGGER.info("Submit request received for Auto Renewal - Batch processing:...");
			Map<String, JobParameter> parameterMap = new HashMap<>();
			JobParameter timeParam = new JobParameter(System.currentTimeMillis());
			parameterMap.put(EMSCommonConstants.BEACON_JOB_TIME, timeParam);
			LOGGER.debug("Length of SQL Query"+getNotificationRawSQLQuery().length());
			
			JobParameter sqlQueryRawParam = new JobParameter(getNotificationRawSQLQuery());
			parameterMap.put(EMSCommonConstants.ELD_AUTORENEWAL_NOTIFICATION_SQL_QUERY_RAW, sqlQueryRawParam);
			JobParameters jobParams = new JobParameters(parameterMap);
			JobExecution jobExecution = null;

			try {
				jobExecution = jobLauncher.run(job, jobParams);
				jobStatusDTO = new JobStatusDTO(jobExecution.getStatus().toString(),
						jobExecution.getExitStatus().getExitCode() + "|"
								+ jobExecution.getExitStatus().getExitDescription());
			} catch (Exception e) {
				jobStatusDTO = new JobStatusDTO(JobStatusType.FAILED.getStatus(), e.getMessage());
				LOGGER.error("Exception occurred during submitting the Job ", e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
			}
			LOGGER.info(" Exit from ELDNotificationBatchController -submit method - Batch processing:...");
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
		}
		
		
		private String getNotificationRawSQLQuery() {
			StringBuilder sqlQueryRaw = new StringBuilder(getSelectClause()).append(" FROM ").append(getSchemaName()).append(EMSCommonConstants.ELD_EMS_ENT_WORKFLOW_PROCESS_T).append(EMSCommonConstants.ELD_NOTIFICATION_JOIN_QUERY).append(getWHEREClause());
						LOGGER.info("getNotificationRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
			return sqlQueryRaw.toString();
			
		}

		private String getSelectClause() {
			return EMSCommonConstants.ELD_NOTIFICATION_AUTORENEWAL_SELECT_CLAUSE;
		}

		private String getWHEREClause() {
			return " WHERE " + EMSCommonConstants.ELD_NOTIFICATION_AUTORENEWAL_WHERE_CLAUSE;
		}


		private String getSchemaName() {
			return "{h-schema}";
		}

		private String getProperty(String prop) {
			return properties.getProperty(prop);
		}

	}


