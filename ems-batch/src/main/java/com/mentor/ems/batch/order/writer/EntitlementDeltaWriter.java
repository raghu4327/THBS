package com.mentor.ems.batch.order.writer;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mentor.ems.entitlement.domain.type.EntitlementType;
import com.mentor.ems.entitlement.dto.EntitlementJobRequestDTO;
import com.mentor.ems.entitlement.dto.JobPayloadDTO;
import com.mentor.ems.entitlement.factory.EMSEntitlementFactory;
import com.mentor.ems.entitlement.services.EMSEntitlementHandler;
import com.mentor.ems.entitlement.services.impl.ConsultingEntitlementHandler;
import com.mentor.ems.entitlement.services.impl.TrainingEntitlementHandler;

/**
 * @author Sanjay
 *
 */
public class EntitlementDeltaWriter implements ItemWriter<JobPayloadDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementDeltaWriter.class);

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
			LOGGER.info("Processing EntitlementJobRequestDTO [[" + entitlementJobRequestDTO
					+ "]] sales Order detail key " + entitlementJobRequestDTO.getOrderDetailKey()
					+ " with order header key" + entitlementJobRequestDTO.getOrderHdrKey());
			if (!StringUtils.isEmpty(entitlementJobRequestDTO.getOrderDetailKey())) {
				EntitlementType entitlementType = entitlementJobRequestDTO.getEmsEntitlement().getEntitlementType();
				EMSEntitlementHandler emsEntitlementHandler = EMSEntitlementFactory
						.getEMSEntitlementHandler(entitlementType);

				if (emsEntitlementHandler instanceof TrainingEntitlementHandler) {
					trainingEntitlementHandler.processEntitlementRequest(entitlementJobRequestDTO);

				}
				if (emsEntitlementHandler instanceof ConsultingEntitlementHandler) {
					consultingEntitlementHandler.processEntitlementRequest(entitlementJobRequestDTO);
				}
			}
			Long endTime = System.currentTimeMillis();
			LOGGER.error(
					"PERFORMANCE_METRICS:: Call to [[" + "Processed EntitlementDeltaRequest JobRequestDTO having order detail " + entitlementJobRequestDTO.getOrderDetailKey()+"]] took " + (endTime - startTime) + " ms");
			LOGGER.info("Processed EntitlementDeltaRequest JobRequestDTO order detail "
					+ entitlementJobRequestDTO.getOrderDetailKey());

		}
	}

	/**
	 * Update the job status as completed in EMS_JOB_T
	 *
	 * @param jobs
	 */
	@AfterWrite
	public void afterComplete(List<? extends JobPayloadDTO> jobs) {
		LOGGER.info("AfterWrite method().. Completed.");
	}

	/**
	 * Update the job status as Failed in EMS_JOB_T
	 *
	 * @param e
	 * @param jobs
	 */
	@OnWriteError
	@Transactional
	public void onWriteError(Exception e, List<? extends JobPayloadDTO> jobs) {
		LOGGER.info("onWriteerror method().. Completed.", e);

	}

}
