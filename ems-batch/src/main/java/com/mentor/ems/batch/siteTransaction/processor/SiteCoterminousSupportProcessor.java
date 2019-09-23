/**
 * 
 */
package com.mentor.ems.batch.siteTransaction.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import com.mentor.ems.entitlement.dto.ProductEntitlementDetailDTO;
/**
 * @author avawasth
 *
 */
public class SiteCoterminousSupportProcessor implements ItemProcessor<ProductEntitlementDetailDTO,ProductEntitlementDetailDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SiteCoterminousSupportProcessor.class);
	@Override
	public ProductEntitlementDetailDTO process(ProductEntitlementDetailDTO productEntitlementDtlDTO) throws Exception {
		LOGGER.info("Enter into SiteCoterminousSupportProcessor - process method" + productEntitlementDtlDTO);
		//ProductEntitlementDetailDTO detailDTO=new ProductEntitlementDetailDTO();
		LOGGER.info("Exit SiteCoterminousSupportProcessor - process method" + productEntitlementDtlDTO);
		return productEntitlementDtlDTO;
	}
}
