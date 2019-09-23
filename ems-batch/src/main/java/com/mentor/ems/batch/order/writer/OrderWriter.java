package com.mentor.ems.batch.order.writer;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.constants.EMSErrorConstants;
import com.mentor.ems.common.core.EMSConfigLoader;
import com.mentor.ems.common.dao.BatchMasterDao;
import com.mentor.ems.common.dao.ErrorDao;
import com.mentor.ems.common.entity.BatchErrorDetails;
import com.mentor.ems.common.entity.BatchMaster;
import com.mentor.ems.common.exception.ApplicationException;
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.common.exception.ErrorData;
import com.mentor.ems.common.exception.ServiceException;
import com.mentor.ems.common.util.EmailUItil;
import com.mentor.ems.entitlement.dto.JobPayloadDTO;
import com.mentor.ems.entitlement.dto.SalesOrderDTO;
import com.mentor.ems.entitlement.service.ProductOrderService;

/**
 * Created by Tamilarasan Rathinagiri on 6/2/17.
 */
@Component
@Transactional
public class OrderWriter implements ItemWriter<JobPayloadDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderWriter.class);

	@Autowired
	ProductOrderService productOrderService;

	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	BatchMasterDao batchMasterDao;

	@Autowired
	private ErrorDao errorDao;

	@Override
	public void write(List<? extends JobPayloadDTO> arg0) {
		List<Long> jobKeyList = arg0.stream().map(jobKey->jobKey.getEmsJob().getJobKey()).collect(Collectors.toList());
		List<String> jobStatusList = arg0.stream().map(jobKey->jobKey.getEmsJob().getJobStatus()).collect(Collectors.toList());
//		LOGGER.info("Enter into OrderWriter having jobKeyList "+jobKeyList + "and size ois "+jobKeyList.size());
		LOGGER.info("Enter into OrderWriter having jobStatusList "+jobStatusList + "and size is "+jobStatusList.size());
		for (Iterator iterator = arg0.iterator(); iterator.hasNext();) {
			Long startTime = System.currentTimeMillis();
			JobPayloadDTO payload = (JobPayloadDTO) iterator.next();
			SalesOrderDTO salesOrderDTO = (SalesOrderDTO) payload.getRequestDTO();
			try {
				LOGGER.info("SalesOrderDTO transaction ID is "
						+ salesOrderDTO.getOrderPublish().getHeader().getTransactionId());
				productOrderService.processSalesOrder(salesOrderDTO);
				payload.getEmsJob().setJobStatus(EMSCommonConstants.COMPLETED);
				payload.getEmsJob().setModifyTs(new Timestamp(new Date().getTime()));
				Long endTime = System.currentTimeMillis();
				LOGGER.error(
						"PERFORMANCE_METRICS:: Call to [[" + "Process Job with job key:"+payload.getEmsJob().getJobKey()+" for Sales Order having order Number:" + salesOrderDTO.getOrderPublish().getOrder().getOrderHeader().getSalesDocumentId()+"]] took " + (endTime - startTime) + " ms");

				LOGGER.info("Processed SalesOrder " + salesOrderDTO.getOrderPublish().getHeader().getTransactionId()
						+ " successfully");
			} catch (Exception e) {
//				LOGGER.error("Exception while processing order ", payload, e);
				ErrorData error = ((ApplicationException) e).getErroData();
				if (EMSCommonConstants.BATCH_STATUS_NEW.equalsIgnoreCase(payload.getEmsJob().getJobStatus())) {
					triggerEmail(payload, salesOrderDTO, e, error);
				}
				BatchMaster batchMaster = createNewBatchMaster();
				if (EMSErrorConstants.ERR_ORDER_REJECT_CODE.equalsIgnoreCase(error.getErrorCode())) {
					updateBatchStatusAsIgnored(error, batchMaster);
					payload.getEmsJob().setBatchKey(batchMaster);
					payload.getEmsJob().setJobStatus(EMSCommonConstants.IGNORED);
					payload.getEmsJob().setModifyTs(new Timestamp(new Date().getTime()));

				} else {
					updateBatchErrorandMaster(e, batchMaster);
					payload.getEmsJob().setBatchKey(batchMaster);
					payload.getEmsJob().setJobStatus(EMSCommonConstants.FAILED_RETRY_STATUS);
					payload.getEmsJob().setModifyTs(new Timestamp(new Date().getTime()));
				}
			}

		}
	}

	private BatchMaster createNewBatchMaster() {
		BatchMaster batchMaster = null;
		try {
			batchMaster = batchMasterDao.createBatchMaster(EMSCommonConstants.ORDER_ONGOING);
		} catch (DataAccessException de) {
			LOGGER.error("Error creating BatchMaster", de);
		}
		return batchMaster;

	}

	private void updateBatchStatusAsIgnored(ErrorData error, BatchMaster batchMaster) {
		try {
			Set<BatchErrorDetails> batchErrorList = new HashSet<>();
			BatchErrorDetails batchError = new BatchErrorDetails();
			batchError.setError(errorDao.getErrorByErrorCode(error.getErrorCode()));
			batchError.setBatchMaster(batchMaster);
			batchError.setCreateTs(new Date());
			batchErrorList.add(batchError);
			batchMaster.setBatchErrorList(batchErrorList);
			entityManager.merge(batchError);
			// set batch status as ignored
			modifyBatchStatus(batchMaster, EMSCommonConstants.IGNORED);
		} catch (DataAccessException de) {
			LOGGER.info("Exception", de);
		}
	}

	private void modifyBatchStatus(BatchMaster batchMaster, String status) {
		batchMaster.setBatchStatus(status);
		batchMaster.setModifyTs(new Date());
		batchMaster.setBatchEndTs(new Date());
		entityManager.merge(batchMaster);

	}

	private void updateBatchErrorandMaster(Exception e, BatchMaster batchMaster) {
		try {
			ErrorData error = ((ApplicationException) e).getErroData();
			batchMasterDao.handleException(batchMaster, error, e);
		} catch (DataAccessException de) {
			LOGGER.info("Exception", de);
		}
	}

	private void triggerEmail(JobPayloadDTO payload, SalesOrderDTO salesOrderDTO, Exception e, ErrorData error) {
		EmailUItil emailUtil = new EmailUItil();
		String emailRecipient = EMSConfigLoader.getConfigValue(EMSCommonConstants.APP_CONFIG, "email_recipient");
		String emailBody ="<html><body><table><tr><td>"+EMSCommonConstants.MODULE_NAME+"</td></tr>"
				+ "<tr><td>"+EMSCommonConstants.ENVIRONMENT + salesOrderDTO.getOrderPublish().getHeader().getEnvironment()+"</td></tr>"
				+ "<tr><td>"+EMSCommonConstants.ORDER_NBR	+ salesOrderDTO.getOrderPublish().getOrder().getOrderHeader().getSalesDocumentId()+"</td></tr>"
				+ "<tr><td>"+EMSCommonConstants.JOB_ID + payload.getEmsJob().getJobKey()+"</td></tr>"
				+ "<tr><td>"+EMSCommonConstants.SESSION_KEY + salesOrderDTO.getOrderPublish().getHeader().getTransactionId()+"</td></tr>"
				+ "<tr><td>"+EMSCommonConstants.ERROR_MSG + error.getErrorCode() + " - "+ error.getErrorDesc()+"</td></tr></table></body></html>";
		try {
			emailUtil.sendEmail(emailBody, EMSCommonConstants.CUSTOM,EMSCommonConstants.ORDER_PROCESSING_FRAMEWORK,emailRecipient,
					salesOrderDTO.getOrderPublish().getHeader().getEnvironment());
		} catch (ServiceException se) {
			LOGGER.info("Exception", se);
		}
	}

	/**
	 * Update the job status as completed in EMS_JOB_T
	 *
	 * @param jobs
	 */
	@AfterWrite
	public void afterComplete(List<? extends JobPayloadDTO> jobs) {
		for (JobPayloadDTO jobPayloadDTO : jobs) {
			entityManager.merge(jobPayloadDTO.getEmsJob());
			LOGGER.info("AfterWrite method().. Completed with job status"+ jobPayloadDTO.getEmsJob().getJobStatus());
		}

	}

	/**
	 * Update the job status as Failed in EMS_JOB_T
	 *
	 * @param e
	 * @param jobs
	 */
	@OnWriteError
	public void onWriteError(Exception e, List<? extends JobPayloadDTO> jobs) {
		LOGGER.error("onWriteError()_ Exception occurred..",e);
		for (JobPayloadDTO jobPayloadDTO : jobs) {
			entityManager.merge(jobPayloadDTO.getEmsJob());
		}
		LOGGER.info("onWriteError method().. Completed.");
	}

}
