package com.mentor.ems.batch.installeditem.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.CodeDao;
import com.mentor.ems.common.entity.Codes;
import com.mentor.ems.common.jms.dao.InstalledItemIdDao;
import com.mentor.ems.entitlement.dao.ProductEntitlementDao;
import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;

/**
 * This method soft delete the installed item and Product Entitlement
 * 
 * @author avijeetk
 *
 */
public class InstalledItemWriter implements ItemWriter<ProductEntitlementDetailDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstalledItemWriter.class);

	@Autowired
	private InstalledItemIdDao installedItemIdDao;
	
	@Autowired
	private ProductEntitlementDao productEntitlementDao;
	
	@Autowired
	private CodeDao codeDao;

	@Override
	public void write(List<? extends ProductEntitlementDetailDTO> items) throws Exception {
		LOGGER.info("Enter into InstalledItem Writer");
		Codes codestatus = codeDao.getCodeByTypeAndDescr(EMSCommonConstants.STATUS_CODE,
				EMSCommonConstants.SOFT_DELETED);
		if (items != null && !items.isEmpty()) {
			LOGGER.info("Attempting to delete installed item of size " + items.size());
			Long startTime = System.currentTimeMillis();
			for (ProductEntitlementDetailDTO productEntitlementDetail : items) {
				// soft delete installed item
				installedItemIdDao.deleteInstalledItem(productEntitlementDetail.getInstalledItemKey(), codestatus);
				// soft delete product entitlement
				productEntitlementDao.deleteProductEntitlement(productEntitlementDetail.getProductEntitlementKey(),
						codestatus);
				// soft delete product entitlement detail
				productEntitlementDao.deleteProductEntitlementDtl(productEntitlementDetail.getProductEntlmntDtlKey(),
						codestatus);

			}
			Long endTime = System.currentTimeMillis();
			LOGGER.warn("PERFORMANCE_METRICS: InstalledItemWriter::write()_ delete installed item of size=[["
					+ items.size() + "]] Time taken=" + (endTime - startTime) + " ms");
		} else {
			LOGGER.warn("Items is null or empty. Write ignored.");
		}
		LOGGER.info("Exit from InstalledItem Writer");
	}

	@OnWriteError
	public void onWriteError(Exception e, List<? extends ProductEntitlementDetailDTO> items) {
		LOGGER.error("onWriteError() is invoked with items of size " + items.size() + " with exception " + e, e);
		for (ProductEntitlementDetailDTO item : items) {
			LOGGER.error("Deleting insatlled: item=" + item + " failed with exception e=" + e);
		}
	}

	@AfterWrite
	public void afterComplete(List<? extends ProductEntitlementDetailDTO> items) {
		LOGGER.warn("afterComplete() is invoked with items size " + items.size());
	}

}
