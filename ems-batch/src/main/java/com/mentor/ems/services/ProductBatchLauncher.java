/**
 * 
 */
package com.mentor.ems.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
import org.springframework.stereotype.Service;
import com.mentor.ems.common.solr.constants.SolrConstants;
import com.mentor.ems.common.solr.entity.ProductSearchDTO;
import com.mentor.ems.common.solr.service.impl.ProductSearchService;

/**
 * @author M1028004
 *
 */

@Service
public class ProductBatchLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProductBatchLauncher.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("productOngoingIndexJob")
	Job job;

	@PersistenceContext
	EntityManager entityManager;


	@Resource(name = SolrConstants.BEACON_SYSTEM_COMMON_PROPERTIES)
	private Properties properties;

	@Autowired
	ProductSearchService productSearchService;

	public void submitBatchRequest() {
		LOGGER.info("submitBatchRequest()... Product ongoing ");
		LocalDateTime lastRunTime = getRecentTimestampFromSolr();
		LocalDateTime currentTime = LocalDateTime.now();
		LOGGER.info("submitBatchRequest()... lastRunTime=" + lastRunTime + "|currentTime=" + currentTime);
		Map<String, JobParameter> parameterMap = new HashMap<>();
		JobParameter timeParam = new JobParameter(System.currentTimeMillis());
		parameterMap.put(SolrConstants.BEACON_JOB_TIME, timeParam);
		JobParameter sqlQueryRawParam = new JobParameter(getSQLQueryRaw(lastRunTime, currentTime));
		parameterMap.put(SolrConstants.BEACON_VIEW_PRODUCT_SQL_QUERY_RAW, sqlQueryRawParam);
		JobParameters jobParams = new JobParameters(parameterMap);
		LOGGER.info("Job Parameters = " + jobParams);

		JobExecution jobExecution = null;
		try {
			jobExecution = jobLauncher.run(job, jobParams);

		} catch (JobExecutionAlreadyRunningException e) {
			LOGGER.error("JobExecutionAlreadyRunningException occurred...", e);
		} catch (JobRestartException e) {
			LOGGER.error("JobRestartException occurred...", e);
		} catch (JobInstanceAlreadyCompleteException e) {
			LOGGER.error("JobInstanceAlreadyCompleteException occurred...", e);
		} catch (JobParametersInvalidException e) {
			LOGGER.error("JobParametersInvalidException occurred...", e);
		}
		LOGGER.info("JobExecution info = " + jobExecution);

		LOGGER.info("");
	}

	private String getSQLQueryRaw(LocalDateTime startTime, LocalDateTime endTime) {
		String sqlQueryRaw = getSelectClause() + " FROM " + getSchemaName() + "." + getViewName()
				+ getWhereClause(startTime, endTime);
		LOGGER.warn("getPartNumberRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);
		return sqlQueryRaw;

	}

	private String getWhereClause(LocalDateTime startTime, LocalDateTime endTime) {
		return (" WHERE " + SolrConstants.BEACON_PART_DATETIME_RANGE_WHERE_CLAUSE)
				.replace(SolrConstants.BEACON_QUERY_START_DATE,
						getDateStringFromDate(startTime, SolrConstants.BEACON_JOB_SOLR_JAVA_DATE_FORMAT))
				.replace(SolrConstants.BEACON_QUERY_END_DATE,
						getDateStringFromDate(endTime, SolrConstants.BEACON_JOB_SOLR_JAVA_DATE_FORMAT));
	}

	private String getSelectClause() {
		return SolrConstants.SQL_PRODUCT_SELECT_CLAUSE;
	}

	private String getViewName() {
		return SolrConstants.BEACON_PART_TABLE;
	}

	private LocalDateTime getRecentTimestampFromSolr() {
		LocalDateTime lastRunDate = null;
		Date deltaStartTs = null;
		ProductSearchDTO productSearchDTO = productSearchService.getRecentlyCreatedProduct(SolrConstants.CREATE_TS);

		if (productSearchDTO != null) {
			deltaStartTs = productSearchDTO.getCreateTs();
			LOGGER.info(
					"Recent productSearchDTO from SOLR is " + productSearchDTO + "|Delta start date=" + deltaStartTs);
			Instant instant = Instant.ofEpochMilli(deltaStartTs.getTime());
			lastRunDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
		} else {
			LOGGER.warn("NO productSearchDTO Found from SOLR!!!!!!! 300minutes fall back will be used.");
			lastRunDate = LocalDateTime.now().minusMinutes(300);
		}
		return lastRunDate;
	}

	private static String getDateStringFromDate(LocalDateTime dateTime, String dateFormat) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
		return dateTime.format(formatter);
	}

	private String getSchemaName() {
		return getProperty(SolrConstants.BEACON_SYSTEM_SCHEMA_NAME);
	}

	private String getProperty(String prop) {
		return properties.getProperty(prop);
	}

}
