package com.mentor.ems.batch.order.processor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentor.ems.common.entity.EmsJob;
import com.mentor.ems.common.entity.EmsJobParam;
import com.mentor.ems.common.types.EMSJobParamNameType;
import com.mentor.ems.entitlement.dto.EntitlementJobRequestDTO;
import com.mentor.ems.entitlement.dto.JobPayloadDTO;

@Component
public class EntitlementProcessor implements ItemProcessor<EmsJob, JobPayloadDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementProcessor.class);

	@Override
	public JobPayloadDTO process(EmsJob emsOrderJob) throws Exception {
		LOGGER.info("Reading EmsJob from EMS_JOB table for job_name:"+emsOrderJob.getJobName()+ "with Job Details" + emsOrderJob);
		return getEntitlementJobRequestDTO(emsOrderJob);
	}

	private JobPayloadDTO getEntitlementJobRequestDTO(EmsJob emsOrderJob) {
		JobPayloadDTO payload = new JobPayloadDTO();
		payload.setEmsJob(emsOrderJob);
		payload.setRequestDTO(getEntitlementJobRequestDTOJobParams(emsOrderJob.getEmsJobParams()));
		return payload;
	}

	private EntitlementJobRequestDTO getEntitlementJobRequestDTOJobParams(List<EmsJobParam> emsJobParams) {
		EntitlementJobRequestDTO entitlementJobRequestDTO = null;
		for (EmsJobParam emsJobParams1 : emsJobParams) {
			LOGGER.info("EmsJobParam emsJobParams1 =" + emsJobParams1);
			if (emsJobParams1.getJobParamName().equals(EMSJobParamNameType.ENTITLEMENT_JOB_REQUEST_DTO.getName())) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				try {
					LOGGER.debug("The BLOB String read from the database is = " + emsJobParams1.getBlobValue());
					entitlementJobRequestDTO = mapper.readValue(emsJobParams1.getBlobValue(),
							EntitlementJobRequestDTO.class);
					LOGGER.debug("Retrieved EntitlementJobRequestDTO from the Database is " + entitlementJobRequestDTO);
					
				} catch (IOException ioe) {
					LOGGER.error("IOException occurred, unable to construct EntitlementJobRequestDTO from JSON "
							+ Arrays.toString(emsJobParams1.getBlobValue()), ioe);
				}
			}
		}

		return entitlementJobRequestDTO;
	}
}
