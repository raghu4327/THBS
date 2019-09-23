/**
 *
 */
package com.mentor.ems.batch.solr.product.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import com.mentor.ems.batch.solr.product.helper.SolrProductHelper;
import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.jobs.EMSJobDAO;
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.common.solr.entity.ProductSearchDTO;
import com.mentor.ems.common.solr.service.ProductIndexService;

/**
 * @author M1028004
 *
 */
@Component
public class SolrOngoingProductWriter implements ItemWriter<ProductSearchDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SolrOngoingProductWriter.class);

	@Autowired
	private ProductIndexService productIndexService;

	@Autowired
	public SolrTemplate solrTemplate;

	@Autowired
	private SolrProductHelper solrProductHelper;

	@Autowired
	private EMSJobDAO emsJobDAO;

	@Override
	public void write(List<? extends ProductSearchDTO> items) {
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

	private void createSolrReprocesingJob(List<? extends ProductSearchDTO> items) {
		LOGGER.info("Creating Solr Reprocessing job during failure-createSolrReprocesingJob method");
		for (ProductSearchDTO productSearchDTO : items) {
			try {
				emsJobDAO.createSolrReprocesingJob(EMSCommonConstants.EMS_PRODUCT_SOLR_REPROCESSING_JOB,
						productSearchDTO);
			} catch (DataAccessException e) {
				LOGGER.error("Exception in SolrOngoingProductWriter-  createSolrReprocesingJob method", e);
			}
		}

	}

}
