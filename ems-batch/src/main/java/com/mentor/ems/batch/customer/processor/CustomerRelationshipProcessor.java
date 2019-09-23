package com.mentor.ems.batch.customer.processor;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.CodeDao;
import com.mentor.ems.common.entity.PartyXParty;

@Component
public class CustomerRelationshipProcessor implements ItemProcessor<PartyXParty, PartyXParty> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerRelationshipProcessor.class);

	@Autowired
	private CodeDao codeDao;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.ItemProcessor#process(java.lang.Object)
	 */

	@Override
	public PartyXParty process(PartyXParty item) throws Exception {
		LOGGER.info("Enter into CustomerRelationshipProcessor-process method");
		if (EMSCommonConstants.ACTIVE.equals(item.getStatusCode().getCodeValue())) {
			item.setStatusCode(
					codeDao.getCodeByTypeAndValue(EMSCommonConstants.STATUS_CODE, EMSCommonConstants.STATUS_INACTIVE));
		} else {
			item.setStatusCode(
					codeDao.getCodeByTypeAndValue(EMSCommonConstants.STATUS_CODE, EMSCommonConstants.STATUS_ACTIVE));
		}
		item.setModifyTs(new Date());
		LOGGER.info("Exit from CustomerRelationshipProcessor-process method");
		return item;
	}
}
