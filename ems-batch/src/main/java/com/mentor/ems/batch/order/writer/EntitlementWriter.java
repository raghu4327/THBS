package com.mentor.ems.batch.order.writer;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.core.EMSConfigLoader;
import com.mentor.ems.common.exception.ServiceException;
import com.mentor.ems.common.listener.EMSLogMDCRequestListener;
import com.mentor.ems.common.util.EMSUtil;
import com.mentor.ems.common.util.EmailUItil;
import com.mentor.ems.entitlement.domain.type.EntitlementType;
import com.mentor.ems.entitlement.dto.EntitlementJobRequestDTO;
import com.mentor.ems.entitlement.dto.JobPayloadDTO;
import com.mentor.ems.entitlement.factory.EMSEntitlementFactory;
import com.mentor.ems.entitlement.services.EMSEntitlementHandler;
import com.mentor.ems.entitlement.services.impl.ConsultingEntitlementHandler;
import com.mentor.ems.entitlement.services.impl.TrainingEntitlementHandler;
import com.mentor.ems.entitlement.utility.EntitlementUtil;
import com.mentor.ems.product.dto.ProductHierarchyDTO;

/**
 * Created by Tamilarasan Rathinagiri on 6/2/17.
 */
@Transactional
@Component
public class EntitlementWriter implements ItemWriter<JobPayloadDTO>, StepExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementWriter.class);

	public static final String MODULE_NAME = "MODULE NAME: EMS-Entitlement-Creation-Ongoing-Service";

	public static final String SALES_ORDER_LINE_NBR = "SALES_DOCUMENT_LINE_NBR: ";

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	public String status = null;

	@Autowired
	EntitlementUtil entitlementUtil;

	@Autowired
	TrainingEntitlementHandler trainingEntitlementHandler;

	@Autowired
	ConsultingEntitlementHandler consultingEntitlementHandler;

	@PersistenceContext
	EntityManager entityManager;

	@Override
	public void write(List<? extends JobPayloadDTO> arg0) throws Exception {
		for (Iterator iterator = arg0.iterator(); iterator.hasNext();) {
			Long startTime = System.currentTimeMillis();
			JobPayloadDTO payload = (JobPayloadDTO) iterator.next();
			EntitlementJobRequestDTO entitlementJobRequestDTO = (EntitlementJobRequestDTO) payload.getRequestDTO();
			try {

				LOGGER.info("Processing EntitlementJobRequestDTO [[" + entitlementJobRequestDTO
						+ "]] sales Order Number " + entitlementJobRequestDTO.getSalesOrderNbr()
						+ " with SalesOrderLineNbr" + entitlementJobRequestDTO.getSalesOrderLineNbr());

				if (entitlementJobRequestDTO.getEmsEntitlement().getProduct() != null
						&& entitlementJobRequestDTO.getEmsEntitlement().getProduct().getProductType()
								.getProductClassCode() == EMSCommonConstants.PRODUCT_CLASS_CODE_OT
						|| entitlementJobRequestDTO.getEmsEntitlement().getProduct().getProductType()
								.getProductClassCode() == EMSCommonConstants.PRODUCT_CLASS_CODE_IP) {
					ProductHierarchyDTO productHierarchyDTO = entitlementUtil.getProductGroup(
							entitlementJobRequestDTO.getEmsEntitlement().getProduct().getProductNumber());
					LOGGER.info("Product groupdivision code " + productHierarchyDTO.getProductDivision().getCode());
					LOGGER.info(
							"Product groupdivision desc " + productHierarchyDTO.getProductDivision().getDescription());
					if (EMSCommonConstants.PRODUCT_DIVISION_500
							.equalsIgnoreCase(productHierarchyDTO.getProductDivision().getCode())) {
						consultingEntitlementHandler.processEntitlementRequest(entitlementJobRequestDTO);
					}

					if (EMSCommonConstants.PRODUCT_DIVISION_600
							.equalsIgnoreCase(productHierarchyDTO.getProductDivision().getCode())) {
						trainingEntitlementHandler.processEntitlementRequest(entitlementJobRequestDTO);
					}
				} else {

					EntitlementType entitlementType = entitlementJobRequestDTO.getEmsEntitlement().getEntitlementType();
					EMSEntitlementHandler emsEntitlementHandler = EMSEntitlementFactory
							.getEMSEntitlementHandler(entitlementType);
					if (emsEntitlementHandler instanceof TrainingEntitlementHandler) {
						trainingEntitlementHandler.processEntitlementRequest(entitlementJobRequestDTO);

					} else {
						if (emsEntitlementHandler instanceof ConsultingEntitlementHandler) {
							consultingEntitlementHandler.processEntitlementRequest(entitlementJobRequestDTO);

						} else {
							emsEntitlementHandler.processEntitlementRequest(entitlementJobRequestDTO);
						}
					}
				}
				payload.getEmsJob().setJobStatus("COMPLETED");
				payload.getEmsJob().setModifyTs(new Timestamp(new Date().getTime()));
				Long endTime = System.currentTimeMillis();
				LOGGER.error("PERFORMANCE_METRICS:: Call to [[" + "Process Job with job key:"
						+ payload.getEmsJob().getJobKey() + " Entitlement having order Line Number:"
						+ entitlementJobRequestDTO.getSalesOrderLineNbr() + "]] took " + (endTime - startTime) + " ms");

				LOGGER.info("Processed EntitlementJobRequestDTO " + entitlementJobRequestDTO.getSalesOrderNbr()
						+ " with SalesOrderLineNbr" + entitlementJobRequestDTO.getSalesOrderLineNbr());
			} catch (Exception e) {
				LOGGER.error("Exception while processing entitlement ", payload, e);
				status = EMSCommonConstants.FAILED_RETRY_STATUS;
				// this.jobExecution.stop();
				triggerEmail(payload, entitlementJobRequestDTO, e);

				if (payload != null) {
					payload.getEmsJob().setJobStatus(EMSCommonConstants.FAILED_RETRY_STATUS);
					payload.getEmsJob().setModifyTs(new Timestamp(new Date().getTime()));
				}
				// throw new ApplicationException(e);
			}
		}
	}

	/**
	 * @param payload
	 * @param entitlementJobRequestDTO
	 * @param e
	 */
	private void triggerEmail(JobPayloadDTO payload, EntitlementJobRequestDTO entitlementJobRequestDTO, Exception e) {
		EmailUItil emailUtil = new EmailUItil();
		String environment = EMSUtil.getEnvironment(EMSCommonConstants.LOOKUP);
		String emailRecipient = EMSConfigLoader.getConfigValue(EMSCommonConstants.APP_CONFIG, "email_recipient");
		String emailBody = "<html><body><table><tr><td>" + MODULE_NAME + "</td></tr>" + "<tr><td>"
				+ EMSCommonConstants.ENVIRONMENT + environment + "</td></tr>" + "<tr><td>"
				+ EMSCommonConstants.ORDER_NBR + entitlementJobRequestDTO.getSalesOrderNbr() + "</td></tr>" + "<tr><td>"
				+ SALES_ORDER_LINE_NBR + entitlementJobRequestDTO.getSalesOrderLineNbr() + "</td></tr>" + "<tr><td>"
				+ EMSCommonConstants.JOB_ID + payload.getEmsJob().getJobKey() + "</td></tr>" + "<tr><td>"
				+ EMSCommonConstants.SESSION_KEY + EMSLogMDCRequestListener.getMDCApplicationId() + "</td></tr>"
				+ "<tr><td>" + EMSCommonConstants.ERROR_MSG + e.getMessage() + "</td></tr></table></body></html>";
		try {
			emailUtil.sendEmail(emailBody, EMSCommonConstants.CUSTOM,
					EMSCommonConstants.MULE_SEND_EMAIL_ENTITLEMENT_SUBJECT, emailRecipient, environment);
		} catch (ServiceException se) {
			LOGGER.info("Exception occured while triggering email in EntitlementWriter-triggerEmail method", se);
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
			if (EMSCommonConstants.FAILED_RETRY_STATUS.equalsIgnoreCase(jobPayloadDTO.getEmsJob().getJobStatus())) {

				this.stepExecution.setTerminateOnly();

			}
		}

		LOGGER.info("AfterWrite method().. Completed.");
	}

	/**
	 * Update the job status as Failed in EMS_JOB_T
	 *
	 * @param e
	 * @param jobs
	 */
	@OnWriteError
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onWriteError(Exception e, List<? extends JobPayloadDTO> jobs) {
		LOGGER.error("onWriteError()_ Exception occurred..", e);
		for (JobPayloadDTO jobPayloadDTO : jobs) {
			jobPayloadDTO.getEmsJob().setJobStatus(EMSCommonConstants.FAILED_RETRY_STATUS);
			entityManager.merge(jobPayloadDTO.getEmsJob());
		}

		LOGGER.info("onWriteError method().. Completed.");
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		// TODO Auto-generated method stub
		this.stepExecution = stepExecution;

	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		// TODO Auto-generated method stub
		return null;
	}

}
