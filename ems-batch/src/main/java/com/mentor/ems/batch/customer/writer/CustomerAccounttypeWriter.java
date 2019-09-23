package com.mentor.ems.batch.customer.writer;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

import com.mentor.ems.common.entity.PartyXAccounttype;

/**
 * @author Sowmya Kasiramasastry
 *
 */
public class CustomerAccounttypeWriter  implements ItemWriter<PartyXAccounttype> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerAccounttypeWriter.class);

	@PersistenceContext
	EntityManager entityManager;

	@Transactional
	@Override
	public void write(List<? extends PartyXAccounttype> items) throws Exception {
		LOGGER.info("Enter into CustomerAccounttypeWriter-write method");
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Long startTime = System.currentTimeMillis();
			PartyXAccounttype partyAcctType = (PartyXAccounttype) iterator.next();
			entityManager.merge(partyAcctType);
			Long endTime = System.currentTimeMillis();
			LOGGER.error(
					"PERFORMANCE_METRICS:: Call to [[" + "Process  Customer Account Type having party Account type key " + partyAcctType.getPartyAccountTypeKey()+"]] took " + (endTime - startTime) + " ms");
		
		}
		LOGGER.info("Exit from CustomerAccounttypeWriter-write method");
	}
}
