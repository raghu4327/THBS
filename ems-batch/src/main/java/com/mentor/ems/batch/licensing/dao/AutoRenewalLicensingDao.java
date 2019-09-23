package com.mentor.ems.batch.licensing.dao;

import java.util.List;

import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;

public interface AutoRenewalLicensingDao {
	
	public long perpectualExpiryDate(String attrValue,long attrConfigKey,String attrDomain);
}
