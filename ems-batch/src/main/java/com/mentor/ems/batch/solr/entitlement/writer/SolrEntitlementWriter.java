package com.mentor.ems.batch.solr.entitlement.writer;

import com.mentor.ems.common.solr.entity.EntitlementSearchDTO;
import com.mentor.ems.common.solr.service.EntitlementIndexService;
import com.mentor.ems.entitlement.services.impl.SolrEntitlementHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Component - Writer class for - Solr Entitlement - ETL Delta use cases
 */
@Component
public class SolrEntitlementWriter implements ItemWriter<EntitlementSearchDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrEntitlementWriter.class);

    @Autowired
    private EntitlementIndexService entitlementIndexService;

    @Autowired
    private SolrEntitlementHelper solrEntitlementHelper;

    @Override
    public void write(List<? extends EntitlementSearchDTO> items) throws Exception {
        if ( items!= null && !items.isEmpty()) {
            LOGGER.info("Attempting to write EntitlementSearchDTO into SOLR of size " + items.size());
            Long startTime = System.currentTimeMillis();
            entitlementIndexService.addToIndex(items);
            Long endTime = System.currentTimeMillis();
            LOGGER.warn("PERFORMANCE_METRICS: SolrEntitlementWriter::write()_ Wrote EntitlementSearchDTOs of size=[[" + items.size() +
                    "]] Time taken=" + (endTime-startTime) + " ms");
        } else {
            LOGGER.warn("Items is null or empty. Write ignored.");
        }
    }


    @OnWriteError
    public void onWriteError(Exception e, List<? extends EntitlementSearchDTO> items) {
        LOGGER.error("onWriteError() is invoked with items of size " + items.size() + " with exception " + e, e);
        for (EntitlementSearchDTO item : items) {
            LOGGER.error("Writing to Solr : item=" + item + " failed with exception e=" + e );
        }
        List<String> entitlenmentNumbersList = items.stream().
                map(EntitlementSearchDTO::getId).
                collect(Collectors.toList());
        solrEntitlementHelper.processSolrEntitlementIds(entitlenmentNumbersList);
    }

    @AfterWrite
    public void afterComplete(List<? extends EntitlementSearchDTO> items) {
        LOGGER.info("afterComplete() : items size " + items.size());
    }
}
