package com.mentor.ems.batch.autorenewal.eldnotification.processor;

import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.entity.EmsWorkFlowProcess;
import com.mentor.ems.entitlement.dto.ELDNotificationDTO;

/**
 * 
 * @author Raghu
 *
 */
@Component
public class AutoRenewalEldNotificationProcessor
		implements ItemProcessor<EmsWorkFlowProcess, ELDNotificationDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenewalEldNotificationProcessor.class);

	@Override
	public ELDNotificationDTO process(EmsWorkFlowProcess emsWorkFlowProcess) throws Exception {
		// TODO Auto-generated method stub
		ELDNotificationDTO eldNotificationDTO = new ELDNotificationDTO();
		try {
		eldNotificationDTO.setNotificationNo(emsWorkFlowProcess.getNotificationNo());
		eldNotificationDTO.setAuthcodeStatus(emsWorkFlowProcess.getAuthcodeStatus().getCodeDescr());
		//eldNotificationDTO.setEldNotificationStatus(emsWorkFlowProcess.getEldNotificationStatus().getCodeDescr());
		eldNotificationDTO.setProcessName(emsWorkFlowProcess.getProcessNameCdKey().getCodeDescr());
		eldNotificationDTO.setWorkflowStatus(emsWorkFlowProcess.getWorkflowStatus().getCodeDescr());
		eldNotificationDTO.setDeliveryDate(emsWorkFlowProcess.getDeliveryDate());
		eldNotificationDTO.setSiteNo(emsWorkFlowProcess.getSiteNo());
		Timestamp cts=new Timestamp(emsWorkFlowProcess.getCreateTs().getTime());
		//Timestamp mts=new Timestamp(emsWorkFlowProcess.getModifyTs().getTime());  
		eldNotificationDTO.setCreateTs(cts);
		//eldNotificationDTO.setModifyTs(mts);
	}catch (Exception e) {
	}
		return eldNotificationDTO;
	
	}
}
	

