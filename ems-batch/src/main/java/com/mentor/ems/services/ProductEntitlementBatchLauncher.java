package com.mentor.ems.services;

import com.mentor.ems.common.dto.JobStatusDTO;
import com.mentor.ems.common.solr.constants.SolrConstants;
import com.mentor.ems.common.solr.entity.EntitlementSearchDTO;
import com.mentor.ems.common.solr.service.impl.EntitlementSearchService;
import com.mentor.ems.common.types.JobStatusType;
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

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class ProductEntitlementBatchLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductEntitlementBatchLauncher.class);

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    @Qualifier("SOLREntitlementIndexJobFull")
    Job job;

    @Resource(name= SolrConstants.BEACON_SYSTEM_COMMON_PROPERTIES)
    private Properties properties;

    @Autowired
    EntitlementSearchService entitlementSearchService;

    public JobStatusDTO submitBatchRequest() {
        JobStatusDTO jobStatusDTO = null;
        LOGGER.info("submitBatchRequest()... Product entitlement Sync....");
        LocalDateTime lastRunTime = getRecentTimestampFromSolr();
        LocalDateTime currentTime = LocalDateTime.now();

        LOGGER.info("submitBatchRequest()... lastRunTime=" + lastRunTime + "|currentTime=" + currentTime);

        Map<String,JobParameter> parameterMap = new HashMap<>();

        JobParameter timeParam = new JobParameter(System.currentTimeMillis());
        parameterMap.put( SolrConstants.BEACON_JOB_TIME, timeParam);

        JobParameter sqlQueryRawParam = new JobParameter(getSQLQueryRaw(lastRunTime,currentTime));
        parameterMap.put( SolrConstants.BEACON_VIEW_ENTITLEMENT_SQL_QUERY_RAW,
                sqlQueryRawParam);

        JobParameters jobParams = new JobParameters(parameterMap);

        LOGGER.info("Job Parameters = " + jobParams);

        JobExecution jobExecution = null;
        try {
            jobExecution = jobLauncher.run(job, jobParams);
            LOGGER.info("JobExecution info = " + jobExecution);
            jobStatusDTO = new JobStatusDTO(jobExecution.getStatus().toString(),jobExecution.getExitStatus().getExitCode() + "|" +
                    jobExecution.getExitStatus().getExitDescription());
        } catch (JobExecutionAlreadyRunningException|JobRestartException|JobInstanceAlreadyCompleteException|JobParametersInvalidException e) {
            jobStatusDTO = new JobStatusDTO(JobStatusType.FAILED.getStatus(), e.getMessage());
            LOGGER.error("Exception occurred during submitting the Job",e);
        }
        return jobStatusDTO;
    }

    private String getSQLQueryRaw(LocalDateTime startTime, LocalDateTime endTime) {
        String sqlQueryRaw = getSelectClause() + " FROM " + getSchemaName() + "." + getViewName()
                + getWhereClause(startTime, endTime) ;
        LOGGER.warn("getViewEntitlementRawSQLQuery()_ sqlQueryRaw=" + sqlQueryRaw);

        return sqlQueryRaw;

    }

    private String getWhereClause(LocalDateTime startTime, LocalDateTime endTime) {
        return
                (" WHERE " +  SolrConstants.BEACON_DATETIME_RANGE_WHERE_CLAUSE).
                        replace(SolrConstants.BEACON_QUERY_START_DATE,
                                getDateStringFromDate(startTime,SolrConstants.BEACON_JOB_SOLR_JAVA_DATE_FORMAT)).
                        replace(SolrConstants.BEACON_QUERY_END_DATE,
                                getDateStringFromDate(endTime,SolrConstants.BEACON_JOB_SOLR_JAVA_DATE_FORMAT));
    }

    private String getSelectClause() {
        return SolrConstants.SQL_PRODUCT_ENTITLEMENT_SELECT_CLAUSE;
    }

    private String getViewName() {
        return SolrConstants.BEACON_PRODUCT_ENTLMNT_VW;
    }

    private LocalDateTime getRecentTimestampFromSolr() {
        LocalDateTime lastRunDate = null;
        Date deltaStartTs = null;
        EntitlementSearchDTO entitlementSearchDTO =
                entitlementSearchService.getRecentlyCreatedProductEntitlement(SolrConstants.DELTA_START_TS);

        if (entitlementSearchDTO != null ) {
            deltaStartTs = entitlementSearchDTO.getDeltaStartTs();
            LOGGER.info("Recent entitlementSearchDTO from SOLR is " + entitlementSearchDTO + "|Delta start date=" + deltaStartTs);
            Instant instant = Instant.ofEpochMilli(deltaStartTs.getTime());
            lastRunDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } else {
            LOGGER.warn("NO entitlementSearchDTO Found from SOLR!!!!!!! 300minutes fall back will be used.");
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
