package com.mentor.ems.batch.customer.writer;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mentor.ems.common.entity.PartyXParty;

@Component
public class CustomerRelationshipWriter implements ItemWriter<PartyXParty> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerRelationshipWriter.class);

	@PersistenceContext
	EntityManager entityManager;

	@Transactional
	@Override
	public void write(List<? extends PartyXParty> items) throws Exception {
		LOGGER.info("Enter into CustomerRelationshipWriter-write method");
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Long startTime = System.currentTimeMillis();
			PartyXParty partyXParty = (PartyXParty) iterator.next();
			entityManager.merge(partyXParty);
			Long endTime = System.currentTimeMillis();
			LOGGER.error(
					"PERFORMANCE_METRICS:: Call to [[" + "Process  Customer Relationship having partyRelationshiptkey " + partyXParty.getPartyRelationshipKey()+"]] took " + (endTime - startTime) + " ms");
		
		}
		LOGGER.info("Exit from CustomerRelationshipWriter-write method");
	}

}
