package com.mentor.ems.batch.autorenewal.eldnotification.writer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.constants.EMSErrorConstants;
import com.mentor.ems.common.core.InterfaceStepEnum;
import com.mentor.ems.common.dao.BatchMasterDao;
import com.mentor.ems.common.dao.CodeDao;
import com.mentor.ems.common.dao.EventDao;
import com.mentor.ems.common.dao.InterfaceStepErrorDao;
import com.mentor.ems.common.dao.PartyRolesDao;
import com.mentor.ems.common.entity.BatchMaster;
import com.mentor.ems.common.entity.EmsWorkFlowProcess;
import com.mentor.ems.common.entity.Event;
import com.mentor.ems.common.entity.InterfaceStepError;
import com.mentor.ems.common.exception.ErrorData;
import com.mentor.ems.common.util.ErrorAssociationUtil;
import com.mentor.ems.entitlement.dao.EldNotificationDao;
import com.mentor.ems.entitlement.dao.EmsWorkFlowDao;
import com.mentor.ems.entitlement.dto.ELDNotificationDTO;
import com.mentor.ems.entitlement.service.EldNotificationService;

@Component
@Transactional
public class AutoRenewalEldNotificationWriter implements ItemWriter<ELDNotificationDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenewalEldNotificationWriter.class);


	@Autowired
	EmsWorkFlowDao emsWorkFlowDao;

	@Autowired
	BatchMasterDao batchMasterDao;

	@Autowired
	PartyRolesDao partyRolesDao;

	@Autowired
	InterfaceStepErrorDao interfaceStepErrorDao;

	@Autowired
	EldNotificationService eldNotificationService;

	@Autowired
	EldNotificationDao eldNotificationDao;
	
	@Autowired
	EventDao eventDao;
	
	@Autowired
	CodeDao codeDao;
	
	@Autowired
	ErrorAssociationUtil errorAssociationUtil;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void write(List<? extends ELDNotificationDTO> items) throws Exception {
		if (items != null && !items.isEmpty()) {
			LOGGER.info("Enter into AutoRenewalPerpetualLicenseWriter - write method");
			Event event = null;
			Boolean prepareELDNotificationCommon = false;
			try {
				BatchMaster	batchMaster = batchMasterDao.createBatchMaster(EMSCommonConstants.ELD_NOTIFICATION_RENEWAL,
                        EMSCommonConstants.BATCH_SOURCE_INPUT_TYPE_JSON, EMSCommonConstants.EMS);
				
				batchMasterDao.updateBatchStatus(batchMaster,EMSCommonConstants.STATUS_INPROGRESS);

				for (ELDNotificationDTO eldNotificationDTO : items) {
					Map<Event, Boolean> eventFlagMap = eldNotificationService.processELDWorkflow(eldNotificationDTO);

					for (Entry<Event, Boolean> prepareflag : eventFlagMap.entrySet()) {
						event = prepareflag.getKey();
						prepareELDNotificationCommon = prepareflag.getValue();
					}
					if (prepareELDNotificationCommon) {
						List<String> notificationIds = new ArrayList<>();
						notificationIds.add(eldNotificationDTO.getNotificationNo());
						for (String notificationNumber : notificationIds) {
							EmsWorkFlowProcess workflowProcess = emsWorkFlowDao
									.fetchWorkFlowProcessByNotificationNumber(notificationNumber);
							if (null != workflowProcess) {
								workflowProcess.setEldNotificationStatus(codeDao.getCodeByTypeAndValue(
										EMSCommonConstants.ELD_NOTIFICATION_STATUS_CODE, EMSCommonConstants.Completed));
								//workflowProcess.set(batchMaster);
								workflowProcess.setModifyTs(new Date());
								emsWorkFlowDao.updateNotificationStatuses(workflowProcess);
							}

						}
						eventDao.updateEventStatus(event.getEventKey(),EMSCommonConstants.COMPLETED);
					} else {
						eventDao.updateEventStatus(event.getEventKey(),EMSCommonConstants.FAILED);
					}
				}
				batchMasterDao.updateBatchStatus(batchMaster, EMSCommonConstants.STATUS_COMPLETED);
				
			} catch (Exception e) {
				Long eventId = event.getEventKey();// Event Id should be passed here
				LOGGER.error(" Exception occured in autorenewal ELD notification process having eventID "
						+ eventId, e);
				ErrorData errorData = new ErrorData();
				errorData.setErrorCode(EMSErrorConstants.ERR_CODE_PUBLISH_INTERMEDIATE_INSTALLITEM_TOPIC);
				LOGGER.error("Exception occured while reprocessing in AutoRenewalEldNotification InterfaceStepError with stepName"
						+ InterfaceStepEnum.ELD_NOTIFICATION_XML_SEND.toString() + e);
				ELDNotificationDTO eldNotificatioDTO = new ELDNotificationDTO();
				InterfaceStepError interfaceStepError = insertInterfaceStepError(eldNotificatioDTO, errorData,
						e.getMessage(), eventId);
				if (null != interfaceStepError) {
					errorAssociationUtil.insertErrorAssociation(EMSCommonConstants.ELD_AUTO_RENEWAL, EMSCommonConstants.ELD, event,
							null, null, interfaceStepError, null, null);
				}
				LOGGER.error("Exception occured while reprocessing InterfaceStepError in AutoRenewalEldNotification with stepName"
						+ InterfaceStepEnum.ELD_NOTIFICATION_XML_SEND.toString() + e);
			}

		}
		LOGGER.info("Exit from AutoRenewalPerpetualLicenseWriter - write method");
	}

	/**
	 * @param publishDTO
	 * @param error
	 * @param errorDesc
	 * @param eventId
	 * @return
	 */
	private InterfaceStepError insertInterfaceStepError(Object publishDTO, ErrorData error, String errorDesc,
			Long eventId) {
		InterfaceStepError interfaceStepError = null;
		try {
			interfaceStepError = interfaceStepErrorDao.insertInterfaceStepError(
					EMSCommonConstants.INSTALLED_ITEM_INTERFACE, EMSCommonConstants.INSTALLED_ITEM_STEP_ERROR,
					error.getErrorCode(), EMSCommonConstants.INSTALLED_ITEM_NBR, publishDTO, errorDesc, eventId);
		} catch (Exception e) {
			LOGGER.error("Exception occcured in insertInterfaceStepError method", e);

		}
		return interfaceStepError;
	}
}
