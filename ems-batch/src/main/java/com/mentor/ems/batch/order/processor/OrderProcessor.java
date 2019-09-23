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
import com.mentor.ems.entitlement.dto.JobPayloadDTO;
import com.mentor.ems.entitlement.dto.SalesOrderDTO;

@Component
public class OrderProcessor implements ItemProcessor<EmsJob, JobPayloadDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessor.class);

	@Override
	public JobPayloadDTO process(EmsJob emsOrderJob) throws Exception {
		LOGGER.info("Reading EmsJob from EMS_JOB table " + emsOrderJob);
		return getSalesOrderDTO(emsOrderJob);
	}

	private JobPayloadDTO getSalesOrderDTO(EmsJob emsOrderJob) {
		JobPayloadDTO payload = new JobPayloadDTO();
		payload.setEmsJob(emsOrderJob);
		payload.setRequestDTO(getSalesOrderDTOFromJobParams(emsOrderJob.getEmsJobParams()));
		return payload;
	}

	private SalesOrderDTO getSalesOrderDTOFromJobParams(List<EmsJobParam> emsJobParams) {
		SalesOrderDTO salesOrderDTO = null;
		for (EmsJobParam emsJobParams1 : emsJobParams) {
			LOGGER.info("EmsJobParam emsJobParams1 =" + emsJobParams1);
			if (emsJobParams1.getJobParamName().equals(EMSJobParamNameType.ORDER_MESSAGE.getName())) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				try {
					LOGGER.debug("The BLOB String read from the database is = " + emsJobParams1.getBlobValue());
					salesOrderDTO = mapper.readValue(emsJobParams1.getBlobValue(), SalesOrderDTO.class);
					LOGGER.debug("Retrieved salesOrderDTO from the Database is " + salesOrderDTO);

				} catch (IOException ioe) {
					LOGGER.error("IOException occurred, unable to construct salesOrderDTO from JSON "
							+ Arrays.toString(emsJobParams1.getBlobValue()), ioe);
				}
			}
		}

		return salesOrderDTO;
	}
}
