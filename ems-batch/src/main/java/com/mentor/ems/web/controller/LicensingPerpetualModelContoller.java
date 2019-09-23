	
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
import com.mentor.ems.common.core.EMSConfigLoader;
import com.mentor.ems.common.dto.JobStatusDTO;
import com.mentor.ems.common.types.JobStatusType;

	@RestController
	@RequestMapping(value = "/autoRenewalPerpetualBatch")
	public class LicensingPerpetualModelContoller {

		private static final Logger LOGGER = LoggerFactory.getLogger(LicensingPerpetualModelContoller.class);

		@Autowired
		JobLauncher jobLauncher;

		@Autowired
		@Qualifier("perpetualLicenseJob")
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
			LOGGER.debug("Length of SQL Query"+getViewProductRawSQLQuery().length());
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
			LOGGER.info(" Exit from LicensingPerpetualModelContoller -submit method - Batch processing:...");
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
		}

		private String getViewProductRawSQLQuery() {
			String licenseRenewalConfigDay = EMSConfigLoader.getConfigValue(EMSCommonConstants.APP_CONFIG, EMSCommonConstants.PERPECTUAL_RENEW_DAY);
			StringBuilder sqlQueryRaw = new StringBuilder(getSelectClause()).append(" FROM ").append(getSchemaName()).append(".").append(EMSCommonConstants.PRODUCT_ENT_DTL_INFO_TABLE).append(EMSCommonConstants.PERPETUAL_JOIN_QUERY).append(getWHEREClause()).append("+").append(licenseRenewalConfigDay).append(")");
						LOGGER.info("getViewProductRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
			return sqlQueryRaw.toString();
		}

		private String getSelectClause() {
			return EMSCommonConstants.SQL_PRODUCT_ENT_DTL_SELECT_VIEW;
		}

		private String getWHEREClause() {
			return " WHERE " + EMSCommonConstants.PERPECTUAL_ENT_DTL_WHERE_CLAUSE;
		}


		private String getSchemaName() {
			return getProperty(EMSCommonConstants.BEACON_SYSTEM_SCHEMA_NAME);
		}

		private String getProperty(String prop) {
			return properties.getProperty(prop);
		}

	}

