package com.mentor.ems.services;

import com.mentor.ems.common.dto.JobStatusDTO;
import com.mentor.ems.common.solr.constants.SolrConstants;
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

import java.util.HashMap;
import java.util.Map;

/**
 * This class will launch the ongoing Solr Training Entitlements
 * 
 * @author avijeetk
 *
 */
@Service
public class SolrTrainingEntitlementOngoingBatchLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrTrainingEntitlementOngoingBatchLauncher.class);

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    @Qualifier("SOLRTrainingEntitlementOngoing")
    Job job;   

    public JobStatusDTO submitBatchRequest() {
        JobStatusDTO jobStatusDTO = null;

        LOGGER.info("submitBatchRequest()... SOLRTrainingEntitlementOngoing Sync....");

        Map<String,JobParameter> parameterMap = new HashMap<>();

        JobParameter timeParam = new JobParameter(System.currentTimeMillis());
        parameterMap.put( SolrConstants.BEACON_JOB_TIME, timeParam);
        JobParameters jobParams = new JobParameters(parameterMap);

        LOGGER.debug("Job Parameters = " + jobParams);

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
        LOGGER.info("Exit from SOLRTrainingEntitlementOngoing Sync");
        return jobStatusDTO;
    }
}
