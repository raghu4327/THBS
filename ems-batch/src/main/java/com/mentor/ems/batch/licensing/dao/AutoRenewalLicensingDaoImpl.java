package com.mentor.ems.batch.licensing.dao;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.constants.EMSErrorConstants;
import com.mentor.ems.common.entity.Part;
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.entitlement.dao.ProductEntitlementDaoImpl;
import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;

@Component
public class AutoRenewalLicensingDaoImpl implements AutoRenewalLicensingDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenewalLicensingDaoImpl.class);

	@PersistenceContext
	EntityManager entityManager;

	public long perpectualExpiryDate(String attrValue,long attrConfigKey,String attrDomain) {
		LOGGER.info("Enter into LicensingDaoImpl----perpectualExpiryDate method------");
		Date ped;
		
		
		try {
			/*Query query = entityManager.createQuery(
					"select  psd.attributeConfig.attrConfigKey from PartySecondaryDetails psd "
					+ "where psd.attrValue=:attrValue and psd.partySecondaryDetailKey=:partySecondaryDetailKey");
			query.setParameter("attrValue", attrValue);
			query.setParameter("partySecondaryDetailKey", partySecondaryDetailKey);*/
			Query query = entityManager.createQuery(
					"select  psd.createTs from PartySecondaryDetails psd "
							+ "where psd.attrValue='NO' and psd.attributeConfig.attrDomain='site' and psd.attributeConfig.attrConfigKey='107'");
			LOGGER.info("Before result");
			/*List<Object[]> list = query.getResultList();
			LOGGER.info("size" + query.getResultList().size());*/
			ped= (Date) query.getSingleResult();
			//LOGGER.info("Testingggggggg" + list.get(0));
			LOGGER.info("attrConfigKey is ------" + ped);
		} catch (Exception e) {
			LOGGER.error("errorInfo :Error fetching attrValue",e);
		}
		return attrConfigKey;
	}

}
