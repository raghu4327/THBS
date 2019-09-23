package com.mentor.ems.batch.solr.customer.writer;

import com.mentor.ems.common.solr.entity.CustomerSearchDTO;
import com.mentor.ems.common.solr.service.CustomerIndexService;
import com.mentor.ems.customer.service.SolrCustomerServiceHelper;
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
 * Component - Writer class for - Solr Customer
 */
@Component
public class SolrCustomerWriter implements ItemWriter<CustomerSearchDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCustomerWriter.class);

    @Autowired
    private CustomerIndexService customerIndexService;

    @Autowired
    private SolrCustomerServiceHelper solrCustomerServiceHelper;


    @Override
    public void write(List<? extends CustomerSearchDTO> items) throws Exception {
        if ( items!= null && !items.isEmpty()) {
            LOGGER.info("Attempting to write CustomerSearchDTO into SOLR of size " + items.size());
            Long startTime = System.currentTimeMillis();
            customerIndexService.addToIndex(items);
            Long endTime = System.currentTimeMillis();
            LOGGER.error("PERFORMANCE_METRICS: SolrCustomerWriter::write()_ Wrote CustomerSearchDTOs of size=[[" + items.size() +
                    "]] Time taken=" + (endTime-startTime) + " ms");
        } else {
            LOGGER.warn("Items is null or empty. Write ignored.");
        }
    }


    @OnWriteError
    public void onWriteError(Exception e, List<? extends CustomerSearchDTO> items) {
        LOGGER.error("onWriteError() is invoked with items of size " + items.size() + " resulted in exception " + e, e);
        for (CustomerSearchDTO item : items) {
            LOGGER.error("Writing to Solr : item ID=" + item.getId() + " failed with exception e=" + e );
        }
        solrCustomerServiceHelper.processFailedCustomerIds(items.stream().map(CustomerSearchDTO::getId).collect(Collectors.toList()));
    }

    @AfterWrite
    public void afterComplete(List<? extends CustomerSearchDTO> items) {
        LOGGER.info("afterComplete() : items size " + items.size());
    }
}
