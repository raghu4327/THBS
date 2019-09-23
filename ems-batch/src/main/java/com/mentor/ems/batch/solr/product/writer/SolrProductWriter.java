package com.mentor.ems.batch.solr.product.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.batch.solr.product.helper.SolrProductHelper;
import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.jobs.EMSJobDAO;
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.common.solr.entity.ProductSearchDTO;
import com.mentor.ems.common.solr.service.ProductIndexService;

/**
 * @author M1038241
 *
 */
@Component
public class SolrProductWriter implements ItemWriter<ProductSearchDTO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrProductWriter.class);

	@Autowired
	private ProductIndexService productIndexService;

	@Autowired
	private EMSJobDAO emsJobDAO;

	@Autowired
	private SolrProductHelper solrProductHelper;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends ProductSearchDTO> items) throws Exception {
		if (items != null && !items.isEmpty()) {
			LOGGER.info("Attempting to write ProductSearchDTO into SOLR of size " + items.size());
			Long startTime = System.currentTimeMillis();
			try {
				productIndexService.addToIndex(items);
				// update product description in Beacon DB.
				solrProductHelper.updateProductDescription(items);
			} catch (Exception e) {
				LOGGER.error("Error writing Product", e);
				createSolrReprocesingJob(items);
			}
			Long endTime = System.currentTimeMillis();
			LOGGER.warn("PERFORMANCE_METRICS: SolrProductWriter::write()_ Wrote ProductSearchDTOs of size=[["
					+ items.size() + "]] Time taken=" + (endTime - startTime) + " ms");
		} else {
			LOGGER.warn("Items is null or empty. Write ignored.");
		}
	}

	@OnWriteError
	public void onWriteError(Exception e, List<? extends ProductSearchDTO> items) {
		LOGGER.error("onWriteError() is invoked with items of size " + items.size() + " with exception " + e, e);
		for (ProductSearchDTO item : items) {
			LOGGER.error("Writing to Solr : item=" + item + " failed with exception e=" + e);
		}
	}

	@AfterWrite
	public void afterComplete(List<? extends ProductSearchDTO> items) {
		LOGGER.warn("afterComplete() is invoked with items size " + items.size());
	}

	private void createSolrReprocesingJob(List<? extends ProductSearchDTO> items) {
		LOGGER.info("Creating Solr Reprocessing job during failure-createSolrReprocesingJob method");
		for (ProductSearchDTO productSearchDTO : items) {
			try {
				emsJobDAO.createSolrReprocesingJob(EMSCommonConstants.EMS_PRODUCT_SOLR_REPROCESSING_JOB,
						productSearchDTO);
			} catch (DataAccessException e) {
				LOGGER.error("Error writing failed partnumber in Batch" + productSearchDTO.getPartNumber(), e);
			}
		}

	}

}
