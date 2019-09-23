package com.mentor.ems.batch.solr.product.helper;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.common.solr.entity.ProductSearchDTO;
import com.mentor.ems.common.util.StringUtil;
import com.mentor.ems.entitlement.dao.PartDao;

@Component
public class SolrProductHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrProductHelper.class);

	@Autowired
	private PartDao partDao;

	public void updateProductDescription(List<? extends ProductSearchDTO> items) {

		for (ProductSearchDTO productSearchDTO : items) {
			LOGGER.info("Attempting to update Product Description into DB of description "
					+ productSearchDTO.getProductDescription());
			try {
				if (!StringUtil.isNullOrEmpty(productSearchDTO.getProductDescription())
						&& !productSearchDTO.getProductDescription().equals(productSearchDTO.getPartDescription())) {
					partDao.updateProductDescription(productSearchDTO);
				}
			} catch (DataAccessException e) {
				LOGGER.error("Exception in  updating Product Description into DB of description "
						+ productSearchDTO.getPartNumber() + e);
			}
		}

	}

}
