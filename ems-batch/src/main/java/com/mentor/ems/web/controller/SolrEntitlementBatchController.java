package com.mentor.ems.web.controller;

import com.mentor.ems.common.dto.JobStatusDTO;
import com.mentor.ems.common.solr.constants.SolrConstants;
import com.mentor.ems.common.types.JobStatusType;
import com.mentor.ems.common.util.StringUtil;
import com.mentor.ems.services.ProductEntitlementBatchLauncher;
import com.mentor.ems.services.SolrConsultingEntitlementOngoingBatchLauncher;
import com.mentor.ems.services.SolrEntitlementOngoingBatchLauncher;
import com.mentor.ems.services.SolrTrainingEntitlementOngoingBatchLauncher;

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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by rathinat on 6/1/17.
 * Controller class - Defined end point for Solr Entitlement load or synchronization
 */

@Controller
@RequestMapping(value = "/search/entitlement/sync")
public class SolrEntitlementBatchController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrEntitlementBatchController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("SOLREntitlementIndexJobFull")
	Job job;

	@Resource(name=SolrConstants.BEACON_SYSTEM_COMMON_PROPERTIES)
	Properties properties;

	@Autowired
	ProductEntitlementBatchLauncher productEntitlementBatchLauncher;

	@Autowired
	SolrEntitlementOngoingBatchLauncher solrEntitlementOngoingBatchLauncher;
	
	@Autowired
	SolrTrainingEntitlementOngoingBatchLauncher solrTrainingEntitlementOngoingBatchLauncher;
	
	@Autowired
	SolrConsultingEntitlementOngoingBatchLauncher solrConsultingEntitlementOngoingBatchLauncher;
	
	@RequestMapping("/ongoing/etl")
	public ResponseEntity<JobStatusDTO> ongoingETLProductBatch() {
		JobStatusDTO jobStatusDTO = productEntitlementBatchLauncher.submitBatchRequest();
		LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);
		if(jobStatusDTO.getStatus().equals(JobStatusType.FAILED.getStatus())) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		} else {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
		}	}

	@RequestMapping("/ongoing")
	public ResponseEntity<JobStatusDTO> ongoingReprocessingBatch() {
		JobStatusDTO jobStatusDTO = solrEntitlementOngoingBatchLauncher.submitBatchRequest();
		LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);
		if(jobStatusDTO.getStatus().equals(JobStatusType.FAILED.getStatus())) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		} else {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
		}	}
	
	@RequestMapping("/ongoing/training")
	public ResponseEntity<JobStatusDTO> ongoingTrainingReprocessingBatch() {
		LOGGER.info("Enter into Solr Training Entitlement ongoing load");
		JobStatusDTO jobStatusDTO = solrTrainingEntitlementOngoingBatchLauncher.submitBatchRequest();
		LOGGER.debug("jobStatusDTO returned is " + jobStatusDTO);
		LOGGER.info("Exit from Solr Training Entitlement ongoing load");
		if (jobStatusDTO.getStatus().equals(JobStatusType.FAILED.getStatus())) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		} else {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
		}
	}

	@RequestMapping("/ongoing/consulting")
	public ResponseEntity<JobStatusDTO> ongoingConsultingReprocessingBatch() {
		LOGGER.info("Enter into Solr Consulting Entitlement ongoing load");
		JobStatusDTO jobStatusDTO = solrConsultingEntitlementOngoingBatchLauncher.submitBatchRequest();
		LOGGER.debug("jobStatusDTO returned is " + jobStatusDTO);
		LOGGER.info("Exit from Solr Consulting Entitlement ongoing load");;
		if (jobStatusDTO.getStatus().equals(JobStatusType.FAILED.getStatus())) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		} else {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
		}
	}

	@RequestMapping("/submit")
	public ResponseEntity<JobStatusDTO> submit(@RequestParam(required = false) String viewType,
								 @RequestParam(required = false) BigInteger startId,
								 @RequestParam(required = false) BigInteger endId,
								 @RequestParam(required = false) String filterType,
								 @RequestParam(required = false) String filterValue,
								 @RequestParam(required = false)
									 @DateTimeFormat(pattern=SolrConstants.BEACON_JOB_SOLR_REST_DATE_FORMAT) Date startDate,
								 @RequestParam(required = false)
									 @DateTimeFormat(pattern=SolrConstants.BEACON_JOB_SOLR_REST_DATE_FORMAT) Date endDate
								 ) {
		LOGGER.info("Submit request received for Solr Entitlement Synchronization - Batch processing:" +
					"|viewType=" + viewType + "|startId=" + startId + "|endId=" + endId +
					"|filterType=" + filterType + "|filterValue=" + filterValue +
		            "|startDate=" + startDate + "|endDate=" + endDate);

		Map<String,JobParameter> parameterMap = new HashMap<>();

		JobParameter timeParam = new JobParameter(System.currentTimeMillis());
		parameterMap.put( SolrConstants.BEACON_JOB_TIME, timeParam);

		JobParameter sqlQueryRawParam = new JobParameter(getViewEntitlementRawSQLQuery(viewType, startId, endId,
				filterType, filterValue,startDate,endDate));
		parameterMap.put( SolrConstants.BEACON_VIEW_ENTITLEMENT_SQL_QUERY_RAW,
				sqlQueryRawParam);

		JobParameters jobParams = new JobParameters(parameterMap);

		LOGGER.info("Job Parameters = " + jobParams);
		JobStatusDTO jobStatusDTO = null;
		try {
			JobExecution jobExecution = jobLauncher.run(job, jobParams);
			jobStatusDTO = new JobStatusDTO(jobExecution.getStatus().toString(),jobExecution.getExitStatus().getExitCode() + "|" +
					jobExecution.getExitStatus().getExitDescription());
		} catch (JobExecutionAlreadyRunningException |JobRestartException |JobInstanceAlreadyCompleteException |JobParametersInvalidException e) {
			jobStatusDTO = new JobStatusDTO(JobStatusType.FAILED.getStatus(), e.getMessage());
			LOGGER.error("Exception occurred during submitting the Job",e);
			LOGGER.error("jobStatusDTO returned is " + jobStatusDTO);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jobStatusDTO);
		}

		LOGGER.info("Solr Entitlement Synchronization - Batch processing Job request submitted successfully.");
		LOGGER.info("jobStatusDTO returned is " + jobStatusDTO);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobStatusDTO);
	}

	private String getViewEntitlementRawSQLQuery(String viewType, BigInteger startId, BigInteger endId,
												 String filterType, String filterValue, Date startDate, Date endDate) {
		String sqlQueryRaw = getSelectClause() + " FROM " + getSchemaName() + "." + getViewName(viewType)
				 + getWhereClause(viewType, startId, endId,
				   filterType,filterValue, startDate, endDate) ;
		LOGGER.warn("getViewEntitlementRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
		return sqlQueryRaw;
	}

	private static String getViewName(String type) {
		String viewName;
		if (!StringUtil.isNullOrEmpty(type) && (
				type.equalsIgnoreCase(SolrConstants.BEACON_CON_ENTLMNT_VW) ||
				type.equalsIgnoreCase(SolrConstants.BEACON_EB_ENTLMNT_VW) ||
				type.equalsIgnoreCase(SolrConstants.BEACON_SW_ENTLMNT_VW) ||
				type.equalsIgnoreCase(SolrConstants.BEACON_HW_ENTLMNT_VW) ||
				type.equalsIgnoreCase(SolrConstants.BEACON_TRN_ENTLMNT_VW) ||
		        type.equalsIgnoreCase(SolrConstants.BEACON_PRODUCT_ENTLMNT_VW )) ) {
			viewName = type.toUpperCase();
		} else{
			LOGGER.warn("viewType " + type + " doesn't match the available list. Default will be used.");
			viewName = SolrConstants.BEACON_PRODUCT_ENTLMNT_VW;
		}
		return viewName;
	}

	private String getSelectClause() {
		return SolrConstants.SQL_PRODUCT_ENTITLEMENT_SELECT_CLAUSE;
	}

	private String getSchemaName() {
		return getProperty(SolrConstants.BEACON_SYSTEM_SCHEMA_NAME);
	}

	private String getWhereClauseSubQuery(String viewName,BigInteger startId, BigInteger endId) {
		String whereClauseSubQuery = SolrConstants.BEACON_VIEW_SUB_QUERY_MAPPING.get(getViewName(viewName));
		return whereClauseSubQuery.replace(SolrConstants.BEACON_QUERY_SCHEMA_NAME,getSchemaName()+".").
				replace(SolrConstants.BEACON_QUERY_START_ID, startId.toString()).
				replace(SolrConstants.BEACON_QUERY_END_ID, endId.toString());
	}

	private String getWhereClause(String viewType, BigInteger startId, BigInteger endId,
								  String filterType, String filterValue, Date startDate, Date endDate) {
		if (SolrConstants.BEACON_YEAR_VALUE.equalsIgnoreCase(filterType)) {
			return (" WHERE " +  SolrConstants.BEACON_DATE_TYPE_WHERE_CLAUSE).replace(SolrConstants.BEACON_YEAR_VALUE,
					Long.toString(Long.parseLong(filterValue)));
		} else if (SolrConstants.BEACON_DATE_RANGE.equalsIgnoreCase(filterType)) {
			return
					(" WHERE " +  SolrConstants.BEACON_DATE_RANGE_WHERE_CLAUSE).
					replace(SolrConstants.BEACON_QUERY_START_DATE,
							getDateStringFromDate(startDate,SolrConstants.BEACON_JOB_SOLR_JAVA_DATE_FORMAT)).
					replace(SolrConstants.BEACON_QUERY_END_DATE,
							getDateStringFromDate(endDate,SolrConstants.BEACON_JOB_SOLR_JAVA_DATE_FORMAT));
		} else if (SolrConstants.BEACON_JOB_ID.equalsIgnoreCase(filterType)) {
			return " WHERE ENTITLEMENT_NBR in ( " + getWhereClauseSubQuery(viewType,  startId, endId) + " )";
		} else {
			LOGGER.warn("No Filter Type given. Load is for the entire record base.");
			return " ";
		}
	}

	private static String getDateStringFromDate(Date date, String dateFormat) {
		SimpleDateFormat df = new SimpleDateFormat(dateFormat);
		return df.format(date);
	}
	private String getProperty(String prop) {
		return properties.getProperty(prop);
	}

}
