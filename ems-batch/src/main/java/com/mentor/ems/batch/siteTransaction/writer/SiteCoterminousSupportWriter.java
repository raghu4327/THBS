/**
 * 
 */
package com.mentor.ems.batch.siteTransaction.writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import com.mentor.ems.common.dao.CodeDao;
import com.mentor.ems.common.dao.PartyRolesDao;

import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;

/**
 * @author avawasth
 *
 */
public class SiteCoterminousSupportWriter implements ItemWriter<ProductEntitlementDetailDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SiteCoterminousSupportWriter.class);

	@Autowired
	PartyRolesDao partyRolesDao;

	@Autowired
	CodeDao codeDao;

	@Override
	public void write(List<? extends ProductEntitlementDetailDTO> entitlementDetailList) throws Exception {
		LOGGER.info("Inside write() method----site status batch");

		List<Long> siteNumberList = entitlementDetailList.stream().map(ProductEntitlementDetailDTO::getSiteNbr)
				.distinct().collect(Collectors.toList());
		List<Long> dateCount = null;
		Date csed = null;
		for (Long siteNumber : siteNumberList) {
			dateCount=new ArrayList<>();
			List<ProductEntitlementDetailDTO> siteEntitlementList = entitlementDetailList.stream()
					.filter(p -> p.getSiteNbr().equals(siteNumber)).collect(Collectors.toList());
			List<Date> supportEndDateList = siteEntitlementList.stream()
					.map(ProductEntitlementDetailDTO::getSupportContractEndDate).collect(Collectors.toList());
			List<Date> distinctDates = supportEndDateList.stream().distinct().collect(Collectors.toList());
			if (distinctDates.size() == 1) {
				csed = distinctDates.get(0);
			} else {
				Collections.sort(distinctDates);
				for (Date date : distinctDates) {
					long count = 0;
					for (Date countDate : supportEndDateList) {
						if (date.compareTo(countDate) == 0) {
							count++;
						}
					}
					dateCount.add(count);
				}
				Long maxCount = Collections.max(dateCount);
				csed = distinctDates.get(dateCount.indexOf(maxCount));
			}
			partyRolesDao.updateCoterminousSupportDate(csed.toString(),"CUSTOMER_SITE_EXPIRY_DATE", siteNumber);
		}
		LOGGER.info("Exit write() method----site status batch");

	}
}
