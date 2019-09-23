/**
 * 
 */
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

/**
 * @author ysingh
 * 
 * This class is used to run the order reprocessing job which is configured in order-job-configuration.xml
 *
 */

@Controller
@RequestMapping(value = "/order")
public class OrderReprocessController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderReprocessController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("OrderProcessJob")
	Job job;
	
	@RequestMapping("/submit")
	public ResponseEntity submit() {
		LOGGER.info("Submit request received for Order processing");
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(job, jobParameters);
			LOGGER.debug("Job Parameters " +jobParameters);
			LOGGER.info("Order  processing Job request submitted..");
			return ResponseEntity.accepted().build();
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			LOGGER.error("Exception occurred while submitting the Job", e);
		}
		return new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);
	}


}
