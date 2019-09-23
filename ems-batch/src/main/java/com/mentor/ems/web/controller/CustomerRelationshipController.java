package com.mentor.ems.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
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

@Controller
@RequestMapping(value = "/customerBatch")
public class CustomerRelationshipController {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerRelationshipController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("partyRelationshipJob")
	Job relationshipJob;
	
	@Autowired
	@Qualifier("partyAccounttypeJob")
	Job accountTypeJob;

	@RequestMapping("/partyRelationshipJob")
	public ResponseEntity partyRelationshipJobSubmit() {
		LOGGER.info("Submit request received for Customer Party-Party Relationship Status update processing");
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(relationshipJob, jobParameters);
			LOGGER.info("Customer Party-Party Relationship Status update processing Job request submitted..");
			return ResponseEntity.accepted().build();
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			LOGGER.error("Exception occurred while submitting the Job", e);
		}
		return new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);
	}
	
	@RequestMapping("/partyAccounttypeJob")
	public ResponseEntity partyAccountTypeJobSubmit() {
		LOGGER.info("Submit request received for Customer Accounttype Relationship Status update processing");
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(accountTypeJob, jobParameters);
			LOGGER.info("Customer Accounttype Relationship Status update processing Job request submitted..");
			return ResponseEntity.accepted().build();
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			LOGGER.error("Exception occurred while submitting the Job", e);
		}
		return new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);
	}

}
