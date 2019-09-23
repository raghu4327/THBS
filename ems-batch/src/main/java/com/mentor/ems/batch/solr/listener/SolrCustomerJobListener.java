package com.mentor.ems.batch.solr.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.solr.entity.EntitlementSearchDTO;

@Component
public class SolrCustomerJobListener implements ItemReadListener<List<EntitlementSearchDTO>>,StepExecutionListener,
        JobExecutionListener,
        ChunkListener, SkipListener<String, String>, ItemWriteListener<EntitlementSearchDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCustomerJobListener.class);

    @Override
    public void beforeChunk(ChunkContext context) {
        LOGGER.info("beforeChunk()_ " + context);
    }

    @Override
    public void afterChunk(ChunkContext context) {
        LOGGER.info("afterChunk()_ " + context);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        LOGGER.info("afterChunkError()_ " + context);
    }

    @Override
    public void beforeRead() {
        LOGGER.info("beforeRead()_ " );
    }

    @Override
    public void afterRead(List<EntitlementSearchDTO> item) {
        LOGGER.info("afterRead()_ Items read..item size=" + item.size() );
    }

    @Override
    public void onReadError(Exception ex) {
        LOGGER.info("onReadError()_ exception=" + ex );
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOGGER.info("beforeJob()_ jobExecution=" + jobExecution);
        LOGGER.info("beforeJob()_ jobExecution.getExecutionContext().entrySet()=" + jobExecution.getExecutionContext().entrySet());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LOGGER.info("afterJob()_ jobExecution=" + jobExecution );
    }

    @Override
    public void onSkipInRead(Throwable t) {
        LOGGER.error("onSkipInRead()_  " + " in skip Read.",t);
    }

    @Override
    public void onSkipInWrite(String item, Throwable t) {
        LOGGER.error("onSkipInWrite()_ Item " + item + " in skip write.",t);

    }

    @Override
    public void onSkipInProcess(String item, Throwable t) {
        LOGGER.error("onSkipInProcess()_ Item " + item + " in skip process.",t);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOGGER.info("beforeStep()_ stepExecution=" + stepExecution);
        LOGGER.info("Params = " + stepExecution.getJobParameters().getParameters());

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        LOGGER.info("afterStep()_ stepExecution=" + stepExecution);
        LOGGER.info("Params = " + stepExecution.getJobParameters().getParameters());
        return null;
    }

    @Override
    public void beforeWrite(List<? extends EntitlementSearchDTO> items) {
        if(items != null && !items.isEmpty()) {
            LOGGER.info("beforeWrite()_ items size=" + items.size());
        } else {
            LOGGER.info("beforeWrite()_ items null or zero size");
        }
    }

    @Override
    public void afterWrite(List<? extends EntitlementSearchDTO> items) {
        if(items != null && !items.isEmpty()) {
            LOGGER.info("afterWrite()_ Write succeeded.. items size=" + items.size());
        } else {
            LOGGER.info("afterWrite()_ Items null or zero size");
        }
    }

    @Override
    public void onWriteError(Exception exception, List<? extends EntitlementSearchDTO> items) {
        LOGGER.error("onWriteError()_ Unable to write to Solr. items=" + items + "|exception=" + exception, exception);
        for (EntitlementSearchDTO item: items) {
            LOGGER.error("onWriteError()_ Unable to write to Solr. Entitlement " + item.getId() + "is failed. |Exception=" + exception
                    , exception);
        }

    }
}
