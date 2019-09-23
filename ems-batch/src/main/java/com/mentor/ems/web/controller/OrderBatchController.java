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

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.core.EMSConfigLoader;
import com.mentor.ems.common.util.EmailUItil;
import com.mentor.ems.common.util.EnvironmentUtil;
import com.mentor.ems.common.util.StringUtil;
import com.mentor.ems.entitlement.service.ProductOrderService;

/**
 * Created by rathinat on 6/1/17.
 */

@Controller
@RequestMapping(value = "/orderbatch")
public class OrderBatchController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBatchController.class);

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	ProductOrderService productOrderService;

	@Autowired
	@Qualifier("OrderJob")
	Job job;

	@RequestMapping("/submit")
	public ResponseEntity submit() {
		LOGGER.info("Submit request received for Order Batch processing");
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(job, jobParameters);
			LOGGER.info("Order Batch processing Job request submitted..");
			return ResponseEntity.accepted().build();
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			LOGGER.error("Exception occurred while submitting the Job", e);
		}
		return new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);
	}

	@RequestMapping("/trigger-mail")
	public ResponseEntity triggerMail() {
		LOGGER.info("Trigger Mail request received for Order Fail EMails After every 24 hours");
		String emailRecipient = EMSConfigLoader.getConfigValue(EMSCommonConstants.APP_CONFIG, "email_recipient");
		try {
			String emailBody = productOrderService.getOrderFailedJobs();
			if(!StringUtil.isNullOrEmpty(emailBody)){
			String environment = EnvironmentUtil.getEnvironment();
			EmailUItil emailUtil = new EmailUItil();
			emailUtil.sendEmail(emailBody, EMSCommonConstants.CUSTOM, EMSCommonConstants.ORDER_PROCESSING_FRAMEWORK,emailRecipient,
					environment);
			}
			return ResponseEntity.accepted().build();
		} catch (Exception e) {
			LOGGER.error("Exception occurred while triggering the mail", e);
		}
		return new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);
	}

}
