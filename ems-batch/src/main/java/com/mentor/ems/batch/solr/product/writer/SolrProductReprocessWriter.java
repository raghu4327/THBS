/**
 * 
 */
package com.mentor.ems.batch.solr.product.writer;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.common.solr.entity.ProductSearchDTO;
import com.mentor.ems.common.solr.service.ProductIndexService;
import com.mentor.ems.entitlement.dao.PartDao;
import com.mentor.ems.entitlement.dto.ProductJobPayloadDTO;

/**
 * @author M1028004
 *
 */
@Component
@Transactional
public class SolrProductReprocessWriter implements ItemWriter<ProductJobPayloadDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SolrProductReprocessWriter.class);

	@Autowired
	private ProductIndexService productIndexService;

	@PersistenceContext
	EntityManager entityManager;
	
	@Autowired
	private PartDao partDao;

	@Override
	public void write(List<? extends ProductJobPayloadDTO> arg0) {
		for (Iterator iterator = arg0.iterator(); iterator.hasNext();) {
			Long startTime = System.currentTimeMillis();
			ProductJobPayloadDTO payload = (ProductJobPayloadDTO) iterator.next();
			ProductSearchDTO productSearchDTO = (ProductSearchDTO) payload.getRequestDTO();
			try {
				if (null != productSearchDTO) {
					productIndexService.addToIndex(productSearchDTO);
					updateProductDescription(productSearchDTO);
					changeJobStatus(payload, EMSCommonConstants.COMPLETED);
				} else {
					changeJobStatus(payload, EMSCommonConstants.FAILED);
				}
				Long endTime = System.currentTimeMillis();
				LOGGER.error(
						"PERFORMANCE_METRICS:: Call to [[" + "Process  Solr Product Reprocessing having job param key " + payload.getEmsJobParam().getJobParamKey()+"]] took " + (endTime - startTime) + " ms");
			} catch (Exception e) {
				LOGGER.error("Exception while processing Product" +payload+e);
				changeJobStatus(payload, EMSCommonConstants.FAILED);
			}
		}

	}

	private void changeJobStatus(ProductJobPayloadDTO payload, String status) {
		payload.getEmsJobParam().setStatusCode(status);
		entityManager.merge(payload.getEmsJobParam());

	}
	private void updateProductDescription(ProductSearchDTO productSearchDTO) {
		LOGGER.info("Attempting to update Product Description into DB of description "
				+ productSearchDTO.getProductDescription());
		try {
			if(null != productSearchDTO.getProductDescription()){
			partDao.updateProductDescription(productSearchDTO);
			}
		} catch (DataAccessException e) {
			LOGGER.error("Exception " +e+" in  updating Product Description into DB of description "
					+ productSearchDTO.getProductDescription());
		}

	}
	

}
