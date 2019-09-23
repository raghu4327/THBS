/*package com.mentor.ems.services;

import java.sql.Date;
import java.util.List;

import com.mentor.ems.common.entity.PartySecondaryDetails;
import com.mentor.ems.entitlement.dto.LicenseRequisitionInstalledItemDTO;
import com.mentor.ems.entitlement.dto.LicenseRequisitionLicenseDetails;

public class PerpetualLicenseRenewServiceImpl  implements PerpetualLicenseRenewService{
	public void autoRenewalForperpetualLicenses(List<LicenseRequisitionInstalledItemDTO> installedItem,
			PartySecondaryDetails partySecondaryDetails) {
		Date licenseExpirationDate = null;
		Date licenseStartDate = null;

		for (LicenseRequisitionInstalledItemDTO licenseRequisitionInstalledItemDTO : installedItem) {
			if (licenseRequisitionInstalledItemDTO.getLicenseType().equalsIgnoreCase("perpetual")) {
				List<LicenseRequisitionLicenseDetails> licenseRequisitionLicenseDetailslist = licenseRequisitionInstalledItemDTO
						.getLicenseDetails();
				for (LicenseRequisitionLicenseDetails licenseRequisitionLicenseDetails : licenseRequisitionLicenseDetailslist) {
					//licenseStartDate = (Date) licenseRequisitionLicenseDetails.getCurrentLicenseExpirationDate()+NUMBEROFDAYS+licenseRequisitionInstalledItemDTO.getInstalledItemKey();
					licenseRequisitionInstalledItemDTO.setCurrentLicenseExpirationDate(licenseStartDate);
				}
			}
		}
		licenseExpirationDate = (Date) partySecondaryDetails.getPerpetualExpDt();

		//return licenseStartDate;
	}
}
*/