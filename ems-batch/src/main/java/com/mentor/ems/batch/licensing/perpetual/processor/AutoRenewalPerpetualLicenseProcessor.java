package com.mentor.ems.batch.licensing.perpetual.processor;

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
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.entitlement.dao.LicenseRequisitionDaoImpl;
import com.mentor.ems.entitlement.dao.ProductEntitlementDao;
import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;
import com.mentor.ems.entitlement.utility.LicenseRequisitionCommons;

/**
 * 
 * @author djayacha
 *
 */
@Component
public class AutoRenewalPerpetualLicenseProcessor
		implements ItemProcessor<ProductEntitlementDetailDTO, ProductEntitlementDetailDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenewalPerpetualLicenseProcessor.class);

	@Autowired
	LicenseRequisitionCommons licenseCommons;

	@Autowired
	LicenseRequisitionDaoImpl licenseRequisitionDaoImpl;

	@Autowired
	private ProductEntitlementDao productEntitlementDao;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.ItemProcessor#process(java.lang.Object)
	 */
	@Override
	public ProductEntitlementDetailDTO process(ProductEntitlementDetailDTO productEntitlementDtlDTO) throws Exception {
		LOGGER.info("Enter into AutoRenewalPerpetualLicenseProcessor - process method" + productEntitlementDtlDTO);
		productEntitlementDtlDTO.setInstalledItemType(EMSCommonConstants.ITEM_TYPE_PERPETUAL_CD);
		// productEntitlementDtlDTO.setInstalledItemNumb(licenseRequisitionDaoImpl.getInstallItemNumberForInstalledItemKey(productEntitlementDtlDTO.getInstalledItemKey()));
		deriveLicenseReqAttributes(productEntitlementDtlDTO);
		LOGGER.info("Installed Item DTO value " + productEntitlementDtlDTO);
		return productEntitlementDtlDTO;
	}

	/**
	 * @param productEntitlementDtlDTO
	 * @throws DataAccessException
	 */
	private void deriveLicenseReqAttributes(ProductEntitlementDetailDTO productEntitlementDtlDTO)
			throws DataAccessException {
		try {
			InstalledItemDTO installedItemDTO = new InstalledItemDTO();
			
			String predefinedDays = EMSConfigLoader.getConfigValue(EMSCommonConstants.APP_CONFIG,
					EMSCommonConstants.PERPECTUAL_PREDEFINED_DAYS);
			int days = Integer.parseInt(predefinedDays);
			List<EmsProductEntlmntDtl> entlDtlList = new ArrayList<EmsProductEntlmntDtl>();
			EmsProductEntlmntDtl emsProductEntlmntDtl = productEntitlementDao
					.fetchProductEntitlementDtlsByInstalledItemKey(productEntitlementDtlDTO.getInstalledItemKey());
			entlDtlList.add(emsProductEntlmntDtl);
			installedItemDTO.setInstalledItemNbr(emsProductEntlmntDtl.getInstalledItemNbr().toString());
			licenseRequisitionDaoImpl.populateProductEntitlementDTO(EMSCommonConstants.SOURCE_SYSTEM_SAP_ECC,
					installedItemDTO, emsProductEntlmntDtl, productEntitlementDtlDTO);

			Date licenseExpiryDate = productEntitlementDtlDTO.getLicenseExpiryDate();
			Date licenseStartDate = DateUtils.addDays(licenseExpiryDate, days);
			if (null != productEntitlementDtlDTO.getSiteNbr()) {
				// fetch Site PED
				Date sitePED = licenseRequisitionDaoImpl.getSitePED(productEntitlementDtlDTO.getSiteNbr());
				productEntitlementDtlDTO.setRequestedLicenseExpirationDate(sitePED);
			}
			productEntitlementDtlDTO.setLicenseEffectiveDate(licenseStartDate);
			;
			String licenseVersion = licenseCommons.calculateLicenseVersion("UPDATE", productEntitlementDtlDTO);
			productEntitlementDtlDTO.setLicenseVersion(licenseVersion);

			LOGGER.info("Calculated License attributes for InstalledItemNbr:"
					+ productEntitlementDtlDTO.getInstalledItemNumb() + " are:Requested LED:"
					+ productEntitlementDtlDTO.getRequestedLicenseExpirationDate() + " ,licenseVersion:"
					+ licenseVersion);

		} catch (Exception e) {
			LOGGER.error("Exception occured in AutoRenewalPerpetualLicenseProcessor" + e);

		}
	}

}
