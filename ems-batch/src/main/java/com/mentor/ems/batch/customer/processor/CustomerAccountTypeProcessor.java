package com.mentor.ems.batch.customer.processor;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.CodeDao;
import com.mentor.ems.common.entity.PartyXAccounttype;

public class CustomerAccountTypeProcessor implements ItemProcessor<PartyXAccounttype, PartyXAccounttype> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerAccountTypeProcessor.class);

	@Autowired
	private CodeDao codeDao;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.ItemProcessor#process(java.lang.Object)
	 */

	@Override
	public PartyXAccounttype process(PartyXAccounttype item) throws Exception {
		LOGGER.info("Enter into CustomerAccountTypeProcessor-process method  having party Account Type key  "
				+ item.getPartyAccountTypeKey());
		LOGGER.info("Status Check" + item.getCodes1().getCodeValue());
		if (EMSCommonConstants.STATUS_ACTIVE.equalsIgnoreCase(item.getCodes1().getCodeValue())) {
			item.setCodes1(
					codeDao.getCodeByTypeAndValue(EMSCommonConstants.STATUS_CODE, EMSCommonConstants.STATUS_INACTIVE));
		} else {
			item.setCodes1(
					codeDao.getCodeByTypeAndValue(EMSCommonConstants.STATUS_CODE, EMSCommonConstants.STATUS_ACTIVE));
		}
		item.setModifyTs(new Date());
		LOGGER.info("Exit from CustomerRelationshipProcessor-process method");
		return item;
	}

}
