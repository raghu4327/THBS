package com.mentor.ems.batch.licensing.term.processor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.core.EMSConfigLoader;
import com.mentor.ems.common.dto.InstalledItemDTO;
import com.mentor.ems.common.entity.EmsProductEntlmntDtl;
import com.mentor.ems.entitlement.dao.InstalledItemDao;
import com.mentor.ems.entitlement.dao.LicenseRequisitionDaoImpl;
import com.mentor.ems.entitlement.dao.ProductEntitlementDao;
import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;
import com.mentor.ems.entitlement.helper.EntitlementHelper;
import com.mentor.ems.entitlement.utility.LicenseRequisitionCommons;

/**
 * 
 * @author ysingh
 *
 */
@Component
public class AutoRenewalTermLicenseProcessor implements ItemProcessor<ProductEntitlementDetailDTO, ProductEntitlementDetailDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenewalTermLicenseProcessor.class);

	@Autowired
	private EntitlementHelper entitlementHelper;

	@Autowired
	private InstalledItemDao installItemDao;
	
	@Autowired
	private LicenseRequisitionCommons licenseCommons;
	
	@Autowired
	private LicenseRequisitionDaoImpl licenseRequisitionDaoImpl;
	
	@Autowired
	private ProductEntitlementDao productEntitlementDao;

	@Override
	public ProductEntitlementDetailDTO process(ProductEntitlementDetailDTO productEntitlementDtlDTO) throws Exception {
		LOGGER.info("Enter into AutoRenewalTermLicenseProcessor - process method" + productEntitlementDtlDTO);
		InstalledItemDTO installedItemDTO = new InstalledItemDTO();
		String predefinedDays = EMSConfigLoader.getConfigValue(EMSCommonConstants.APP_CONFIG,
				EMSCommonConstants.PREDEFINED_DAYS);
		int days = Integer.parseInt(predefinedDays);
		List<EmsProductEntlmntDtl> entlDtlList=new ArrayList<EmsProductEntlmntDtl>();
		//
		EmsProductEntlmntDtl emsProductEntlmntDtl=productEntitlementDao.fetchProductEntitlementDtlsByInstalledItemKey(productEntitlementDtlDTO.getInstalledItemKey());
		entlDtlList.add(emsProductEntlmntDtl);
		installedItemDTO.setInstalledItemNbr(emsProductEntlmntDtl.getInstalledItemNbr().toString());
		licenseRequisitionDaoImpl.populateProductEntitlementDTO(EMSCommonConstants.SOURCE_SYSTEM_SAP_ECC, installedItemDTO, emsProductEntlmntDtl, productEntitlementDtlDTO);
		Date licenseExpiryDate = productEntitlementDtlDTO.getLicenseExpiryDate();
		Date termStartDate = productEntitlementDtlDTO.getCotractStartDate();
		Date termEndDate = productEntitlementDtlDTO.getContractEndDate();
		String creditCategoryDate = productEntitlementDtlDTO.getCreditCategoryCode();
		Date ledGeneric = entitlementHelper.getLEDGeneric(termStartDate, termEndDate, creditCategoryDate);
		Date licenseStartDate = DateUtils.addDays(licenseExpiryDate, days);
		productEntitlementDtlDTO.setRequestedLicenseExpirationDate(ledGeneric);
		productEntitlementDtlDTO.setLicenseEffectiveDate(licenseStartDate);
		String licenseVersion = licenseCommons.calculateLicenseVersion("UPDATE", productEntitlementDtlDTO);
		productEntitlementDtlDTO.setLicenseVersion(licenseVersion);
		//licenseCommons.setLicenseDates(entlDtlList, productEntitlementDtlDTO);
		//installedItemDTO.set
		LOGGER.info("Calculated License attributes for InstalledItemKey:"+productEntitlementDtlDTO.getInstalledItemKey()+" are:Requested LED:"+productEntitlementDtlDTO.getRequestedLicenseExpirationDate()+" ,licenseVersion:"+licenseVersion);
		return productEntitlementDtlDTO;
	}

}
