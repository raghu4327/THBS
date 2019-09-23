package com.mentor.ems.batch.solr.entitlement.writer;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.entity.EmsJob;
import com.mentor.ems.common.solr.entity.EntitlementSearchDTO;
import com.mentor.ems.common.solr.service.EntitlementIndexService;
import com.mentor.ems.common.types.JobStatusType;
import com.mentor.ems.entitlement.dao.EntitlementViewDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Component - Writer class for - Solr Training Entitlement - Delta processing
 */
@Component
public class SolrTrainingEntitlementOngoingWriter implements  ItemWriter<EmsJob> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrTrainingEntitlementOngoingWriter.class);

    @Autowired
    EntitlementIndexService entitlementIndexService;

    @Autowired
    EntitlementViewDAO entitlementViewDAO;

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public void write(List<? extends EmsJob> items) throws Exception {
        for ( EmsJob emsJob : items ) {
            try {
            	Long startTime = System.currentTimeMillis();
                LOGGER.info("Processing EmsJob.job_key=" + emsJob.getJobKey());
                List<EntitlementSearchDTO> entitlementSearchDTOList = entitlementViewDAO.getTrainingEntitlementsFromJobId(emsJob.getJobKey());
                LOGGER.info("EmsJob.job_key=" + emsJob.getJobKey() + " Read complete ");
                
                LOGGER.info("EmsJob.job_key=" + emsJob.getJobKey() + " Solr Entitlement Processing complete");
                entitlementIndexService.addToIndex(entitlementSearchDTOList);

                emsJob.setJobStatus(JobStatusType.COMPLETED.getStatus());
                emsJob.setModifyTs(new Timestamp(new Date().getTime()));
                Long endTime = System.currentTimeMillis();
                LOGGER.error(
						"PERFORMANCE_METRICS:: Call to [[" + "Process  Entitlement  Ongoing having job key " + emsJob.getJobKey()+"]] took " + (endTime - startTime) + " ms");

               
                LOGGER.info("Processing complete for EmsJob.job_key=" + emsJob.getJobKey());
            } catch (Exception e) {
                LOGGER.error("Exception while processing EmsJobs " + items, e);
                emsJob.setJobStatus(EMSCommonConstants.FAILED_RETRY_STATUS);
                emsJob.setModifyTs(new Timestamp(new Date().getTime()));
            }
        }
    }

    /**
     * Update the job status as completed in EMS_JOB_T
     *
     * @param jobs
     */
    @AfterWrite
    public void afterComplete(List<? extends EmsJob> jobs) {
        updateEmsJobs(jobs);
        LOGGER.info("afterComplete() completed");
    }

    /**
     * Update the job status as completed in EMS_JOB_T
     *
     * @param jobs
     */
    @OnWriteError
    public void onWriteError(Exception e, List<? extends EmsJob> jobs) {
        LOGGER.error("onWriteError()_ exception = " + e, e);
        updateEmsJobs(jobs);
        LOGGER.info("onWriteError() completed");
    }

    private void updateEmsJobs( List<? extends EmsJob> jobs) {
        if(jobs != null && !jobs.isEmpty()) {
            for (EmsJob job : jobs) {
                entityManager.merge(job);
            }
        }
    }

}
