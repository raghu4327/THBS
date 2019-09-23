package com.mentor.ems.batch.order.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.entity.OrderDetail;
import com.mentor.ems.entitlement.domain.type.EntitlementType;
import com.mentor.ems.entitlement.dto.EntitlementJobRequestDTO;
import com.mentor.ems.entitlement.dto.JobPayloadDTO;
import com.mentor.ems.entitlement.utility.EntitlementUtil;

@Component
public class ConsultingEntitlementDeltaProcessor implements ItemProcessor<OrderDetail, JobPayloadDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsultingEntitlementDeltaProcessor.class);

	@Autowired
	EntitlementUtil entitlementUtil;

	@Override
	public JobPayloadDTO process(OrderDetail orderDetail) throws Exception {
		LOGGER.info("Reading EmsJob from EMS_JOB table " + orderDetail+" __"+orderDetail.getOrderDetailKey()+" __"+orderDetail.getOrderHeader().getOrderHdrKey());
		return getEntitlementJobRequestDTO(orderDetail);
	}

	private JobPayloadDTO getEntitlementJobRequestDTO(OrderDetail orderDetail) {
		JobPayloadDTO payload = new JobPayloadDTO();
		payload.setEmsJob(null);
		payload.setRequestDTO(getEntitlementJobRequestDTOJobParams(orderDetail));
		return payload;
	}

	private EntitlementJobRequestDTO getEntitlementJobRequestDTOJobParams(OrderDetail orderDetail) {
		return entitlementUtil.getEntitlementDetlaRequest(orderDetail,EntitlementType.CONSULTING);
	}
}
