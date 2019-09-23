package com.mentor.ems.batch.licensing.perpetual.writer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.core.InterfaceStepEnum;
import com.mentor.ems.common.dao.BatchMasterDao;
import com.mentor.ems.common.dao.CodeDao;
import com.mentor.ems.common.dao.ErrorDao;
import com.mentor.ems.common.dao.EventDao;
import com.mentor.ems.common.dao.SourceSystemDao;
import com.mentor.ems.common.entity.BatchMaster;
import com.mentor.ems.common.entity.Codes;
import com.mentor.ems.common.entity.EmsWorkFlowData;
import com.mentor.ems.common.entity.EmsWorkFlowProcess;
import com.mentor.ems.common.entity.Error;
import com.mentor.ems.common.entity.Event;
import com.mentor.ems.common.entity.PartySecondaryDetails;
import com.mentor.ems.entitlement.dao.EmsWorkFlowDao;
import com.mentor.ems.entitlement.dao.EventErrorDtlsDao;
import com.mentor.ems.entitlement.dao.LicenseRequisitionDaoImpl;
import com.mentor.ems.entitlement.dao.PartyDao;
import com.mentor.ems.entitlement.dto.LicenseReqResponsePayloadDTO;
import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;
import com.mentor.ems.entitlement.service.OrderUpdateLicenseRequisitionImpl;
import com.mentor.ems.entitlement.utility.EMSCommonUtil;
import com.mentor.ems.entitlement.utility.LicenseRequisitionCommons;
import com.mentor.ems.security.util.Util;

/**
 * 
 * @author skasiram
 *
 */
@Component
@Transactional
public class AutoRenewalPerpetualLicenseWriter implements ItemWriter<ProductEntitlementDetailDTO>{


	private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenewalPerpetualLicenseWriter.class);

	
		@Autowired
		private OrderUpdateLicenseRequisitionImpl orderUpdateLicenseRequisitionImpl;
		
		@Autowired
		LicenseRequisitionDaoImpl licenseRequisitionDaoImpl;
		
		@Autowired
		LicenseRequisitionCommons licenseCommons;
		
		@Autowired
		BatchMasterDao batchMasterDao;
		
		@Autowired
		PartyDao partyDao;
		
		@Autowired
		CodeDao codeDao;
		
		@Autowired
		SourceSystemDao sourceSystemDao;
		
		@Autowired
		EventDao eventDao;

		@Autowired
		EmsWorkFlowDao emsWorkFlowDao;
		
		@Autowired
		EventErrorDtlsDao eventErrorDtlsDao;
		
		@Autowired
		ErrorDao errorDao;
		
		@Autowired
		EMSCommonUtil emsCommonUtil;
	
		/* (non-Javadoc)
		 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
		 */
		@Transactional(propagation = Propagation.REQUIRES_NEW,noRollbackFor=Exception.class)
		@SuppressWarnings("unchecked")
		@Override
	public void write(List<? extends ProductEntitlementDetailDTO> items) throws Exception {
		if (items != null && !items.isEmpty()) {
			LOGGER.info("Enter into AutoRenewalPerpetualLicenseWriter - write method");
			try {
				BatchMaster batchMaster = batchMasterDao.createBatchMaster(
						EMSCommonConstants.BATCH_TYPE_LC_AUTO_PERPETUAL,
						EMSCommonConstants.BATCH_SOURCE_INPUT_TYPE_JSON, EMSCommonConstants.EMS);
				batchMasterDao.updateBatchStatus(batchMaster, EMSCommonConstants.STATUS_INPROGRESS);

				Map<Long, List<ProductEntitlementDetailDTO>> groupingBasedOnsites = null;

				if (items != null && !items.isEmpty()) {
					// grouping based on sitenumber
					groupingBasedOnsites = groupingBasedOnsites(items);
				}

				List<Boolean> processFlag = processAutoRenewal(groupingBasedOnsites, batchMaster);
				if (processFlag.contains(false)) {
					batchMasterDao.updateBatchStatus(batchMaster, EMSCommonConstants.FAILED);
				} else {
					batchMasterDao.updateBatchStatus(batchMaster, EMSCommonConstants.COMPLETED);
				}

			} catch (Exception e) {
				LOGGER.error("Exception occured in AutoRenewalPerpetualLicenseWriter ", e);
			}

		}
		LOGGER.info("Exit from AutoRenewalPerpetualLicenseWriter - write method");
	}
		
		
	private List<Boolean> processAutoRenewal(Map<Long, List<ProductEntitlementDetailDTO>> groupingBasedOnsites,BatchMaster batchMaster) {
		Map<Long, List<ProductEntitlementDetailDTO>> groupMap=new HashMap<Long, List<ProductEntitlementDetailDTO>>();
		Long key = null;
		List<Boolean> batchFlagStatus=new ArrayList<>(); 
		try {
			if (groupingBasedOnsites != null && !groupingBasedOnsites.isEmpty()) {
			//batch events creation
			for (Entry<Long, List<ProductEntitlementDetailDTO>> site : groupingBasedOnsites.entrySet()) 
				{
				    batchFlagStatus.add(true);
					key = site.getKey();
					groupMap.put(site.getKey(), groupingBasedOnsites.get(site.getKey()));
					processLicenseBySite(groupMap, batchMaster, EMSCommonConstants.SOURCE_SYSTEM_SAP_ECC);
					groupMap.remove(key);
				}
			}else
			{
				batchFlagStatus.add(false);
				batchMasterDao.updateBatchStatus(batchMaster, EMSCommonConstants.FAILED);
			}
	}catch (Exception e) {
		    batchFlagStatus.add(false);
			groupingBasedOnsites.remove(key);
			if (groupingBasedOnsites != null && !groupingBasedOnsites.isEmpty()) {
				processAutoRenewal(groupingBasedOnsites, batchMaster);
			}
	}
		return batchFlagStatus;
	}

	/**
	 * grouping of the productEntitlementDTO based on the SiteNumber
	 * 
	 * @param items
	 * @return Map<Long,List<ProductEntitlementDetailDTO>>
	 */
	private Map<Long, List<ProductEntitlementDetailDTO>> groupingBasedOnsites(
			List<? extends ProductEntitlementDetailDTO> items) {
		LOGGER.info("Enter into groupingBasedOnSites");
		Map<Long, List<ProductEntitlementDetailDTO>> siteMap = new HashMap<Long, List<ProductEntitlementDetailDTO>>();

		if (!CollectionUtils.isEmpty(items)) {
			siteMap = items.stream().collect(Collectors.groupingBy(ProductEntitlementDetailDTO::getSiteNbr));
		}

		return siteMap;
	}

	/**
	 * @param siteAndIBMap
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
	public void processLicenseBySite(Map<Long, List<ProductEntitlementDetailDTO>> siteAndIBMap, BatchMaster batchMaster,
			String sourceSystem) throws Exception {
		LOGGER.info("Enter into processLicenseBySite");
		Event createEvent = null;
		Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
		try {
			List<Event> listOfEvents = new ArrayList<>();
			for (Entry<Long, List<ProductEntitlementDetailDTO>> productEntitlementSite : siteAndIBMap.entrySet()) {
				List<ProductEntitlementDetailDTO> productEntitlementDetails = productEntitlementSite.getValue();
				Long siteNumber = productEntitlementSite.getKey();
				Map<String, Object> paramList = new HashMap<>();
				EmsWorkFlowProcess emsWorkflowProcess = new EmsWorkFlowProcess();

				if (productEntitlementDetails != null) {
					for (ProductEntitlementDetailDTO productEntitlementDetailDTO : productEntitlementDetails) {
						paramList.put("installedItemKey", productEntitlementDetailDTO.getInstalledItemKey());
						// paramList.put("installedItemNumb",
						// productEntitlementDetailDTO.getInstalledItemNumb());
					}

				}
				String notificationNbr = emsCommonUtil.getUniqueNbr();
				Timestamp ts = new Timestamp(System.currentTimeMillis());
				createEvent = eventDao.createEvent("LA" + "|" + siteNumber + "|" + notificationNbr, "PERPETUAL",
						paramList, EMSCommonConstants.ACTIVE_RECORD, EMSCommonConstants.SOURCE_SYSTEM_EMS,
						EMSCommonConstants.SYSTEM_USER);

				listOfEvents.add(createEvent);

				PartySecondaryDetails secondaryDetailsBySiteNumber = partyDao
						.getSecondaryDetailsBySiteNumber(siteNumber);

				String attrValue = secondaryDetailsBySiteNumber.getAttrValue();

				Codes automaticCode = null;
				Codes pauseCode = null;
				Codes manualCode = null;
				Codes pendingCode = null;
				if (attrValue.equalsIgnoreCase("YES")) {

					automaticCode = codeDao.getCodeByTypeAndDescr(EMSCommonConstants.AUTHCODE_DELIVERY_STATUS_CODE,
							"Automatic");
					emsWorkflowProcess.setAuthcodeStatus(automaticCode);
					pauseCode = codeDao.getCodeByTypeAndDescr(EMSCommonConstants.ELD_AUTORENEWAL_STATUS_CODE, "Paused");
					emsWorkflowProcess.setAuthcodeStatus(automaticCode);
					emsWorkflowProcess.setEldNotificationStatus(pauseCode);
				} else {
					manualCode = codeDao.getCodeByTypeAndDescr(EMSCommonConstants.AUTHCODE_DELIVERY_STATUS_CODE,
							"Manual");
					emsWorkflowProcess.setAuthcodeStatus(manualCode);
				}

				emsWorkflowProcess.setNotificationNo(notificationNbr);
				Codes autorenewal = codeDao.getCodeByTypeAndDescr(EMSCommonConstants.ELD_AUTO_RENEWAL, "AUTO RENEWAL");
				pendingCode = codeDao.getCodeByTypeAndDescr(EMSCommonConstants.ELD_AUTORENEWAL_STATUS_CODE, "Pending");
				emsWorkflowProcess.setEldNotificationStatus(pendingCode);
				emsWorkflowProcess.setProcessNameCdKey(autorenewal);
				Codes newCode = codeDao.getCodeByTypeAndDescr(EMSCommonConstants.ELD_AUTORENEWAL_STATUS_CODE, "NEW");
				emsWorkflowProcess.setWorkflowStatus(newCode);
				emsWorkflowProcess.setDeliveryDate(timeStamp);
				emsWorkflowProcess.setSiteNo(siteNumber);
				emsWorkflowProcess.setCreateTs(new Date());
				emsWorkflowProcess.setModifyTs(new Date());
				emsWorkflowProcess.setCreateBatchKey(batchMaster);
				emsWorkflowProcess = emsWorkFlowDao.createEmsWorkflowProcess(emsWorkflowProcess);

				EmsWorkFlowData emsWorkFlowData = new EmsWorkFlowData();

				if (attrValue.equalsIgnoreCase("YES")) {
					emsWorkFlowData.setAuthCodeStatus(automaticCode);
				} else {
					emsWorkFlowData.setAuthCodeStatus(manualCode);
				}
				emsWorkFlowData.setWorkFlowProcessKey(emsWorkflowProcess);
				emsWorkFlowData.setWorkFlowStatus(newCode);
				emsWorkFlowData.setProcessNameCdKey(autorenewal);
				emsWorkFlowData.setSiteNo(siteNumber);
				emsWorkFlowData.setCreateTs(new Date());
				emsWorkFlowData.setModifyTs(new Date());
				emsWorkFlowData.setCreateBatchKey(batchMaster);
				emsWorkFlowDao.createEmsWorkflowData(emsWorkFlowData);

				// payload process to be revoked Commented for Testing purpose
				List<LicenseReqResponsePayloadDTO> processLicenseGeneration = orderUpdateLicenseRequisitionImpl
						.processLicenseGeneration((List<ProductEntitlementDetailDTO>) productEntitlementDetails,
								batchMaster, EMSCommonConstants.SOURCE_SYSTEM_SAP_ECC);
				createEvent.setEventStatus(EMSCommonConstants.COMPLETED);
				eventDao.updateEvent(createEvent, EMSCommonConstants.SYSTEM_USER);
				batchMasterDao.updateBatchStatus(batchMaster, EMSCommonConstants.STATUS_COMPLETED);
			}
		} catch (Exception e) {
			LOGGER.error(" processLicenseBySite while proccesing LicenseRequisitionResponse ");
			batchMasterDao.updateBatchStatus(batchMaster, EMSCommonConstants.FAILED);
			Error error = errorDao.getErrorByErrorCode(EMSCommonConstants.APPLICATION_ERROR);
			createEvent.setEventStatus(EMSCommonConstants.FAILED);
			eventDao.updateEvent(createEvent, EMSCommonConstants.SYSTEM_USER);
			eventErrorDtlsDao.storeEventError(createEvent, error, e.getMessage(), e.getMessage(),EMSCommonConstants.SYSTEM_USER);
			throw new Exception("Exception occured in processLicenseBySite while proccesing LicenseRequisitionResponse",
					e);
		}

	}

}


