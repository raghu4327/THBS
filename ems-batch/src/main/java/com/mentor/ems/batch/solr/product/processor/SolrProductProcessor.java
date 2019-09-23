package com.mentor.ems.batch.solr.product.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.jobs.EMSJobDAO;
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.common.solr.entity.ProductSearchDTO;
import com.mentor.ems.product.service.SolrProductService;

@Component
public class SolrProductProcessor implements ItemProcessor<ProductSearchDTO, ProductSearchDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrProductProcessor.class);

	@Autowired
	private SolrProductService productService;

	@Autowired
	private EMSJobDAO emsJobDAO;

	@Override
	public ProductSearchDTO process(ProductSearchDTO productSearchDTO) {
		ProductSearchDTO productSearch = null;
		try {
			productSearch = productService.updateProduct(productSearchDTO);
		} catch (Exception e) {
			LOGGER.error("Error" + e + " in processing Product with partNumber" + productSearchDTO.getPartNumber());
			createSolrReprocesingJob(productSearchDTO);
		}
		return productSearch;
	}

	private void createSolrReprocesingJob(ProductSearchDTO productSearchDTO) {
		try {
			emsJobDAO.createSolrReprocesingJob(EMSCommonConstants.EMS_PRODUCT_SOLR_REPROCESSING_JOB, productSearchDTO);
		} catch (DataAccessException e) {
			LOGGER.error("Error in creating SolrReprocessing job in SolrProductProcessor-class" + e);
		}

	}

}