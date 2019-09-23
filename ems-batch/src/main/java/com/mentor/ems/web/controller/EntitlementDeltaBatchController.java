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
import com.mentor.ems.batch.custom.reader.ConsultingEntitlementDeltaItemReader;
import com.mentor.ems.batch.custom.reader.TrainingEntitlementDeltaItemReader;
@Controller
@RequestMapping(value = "/entitlementdeltabatch")
public class EntitlementDeltaBatchController {

	private static  final Logger LOGGER = LoggerFactory.getLogger(EntitlementBatchController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	@Qualifier("ConsultingEntitlementDeltaJob")
	Job consultingDeltaJob;

	@Autowired
	@Qualifier("TrainingEntitlementDeltaJob")
	Job trainingDeltaJob;

	@RequestMapping("/consulting/submit")
	public ResponseEntity consultingSubmit() {
		LOGGER.info("Submit request received for consulting entitltement delta processing");
      	ConsultingEntitlementDeltaItemReader.setConsultingEntitlementKey(null);
		ConsultingEntitlementDeltaItemReader.setOrderDetailStartKey(null);
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(consultingDeltaJob, jobParameters);
			LOGGER.info("Consulting entitltement delta processing completed..");
			return ResponseEntity.accepted().build();
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			LOGGER.error("Exception occurred while submitting the Job", e);
		}
		return new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);
	}

	@RequestMapping("/training/submit")
	public ResponseEntity triningSubmit() {
		LOGGER.info("Submit request received for training entitltement delta processing");
      	TrainingEntitlementDeltaItemReader.setTrainingEntitlementKey(null);
		TrainingEntitlementDeltaItemReader.setOrderDetailStartKey(null);
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(trainingDeltaJob, jobParameters);
			LOGGER.info("Training entitltement delta processing completed....");
			return ResponseEntity.accepted().build();
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			LOGGER.error("Exception occurred while submitting the Job", e);
		}
		return new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);
	}

}
