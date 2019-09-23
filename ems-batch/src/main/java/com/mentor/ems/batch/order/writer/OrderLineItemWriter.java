package com.mentor.ems.batch.order.writer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.entity.Event;
import com.mentor.ems.common.entity.EventProcess;
import com.mentor.ems.common.entity.EventProcessErrorDtls;
import com.mentor.ems.common.entity.EventProcessParam;
import com.mentor.ems.common.util.DateUtil;
import com.mentor.ems.common.util.StringUtil;
import com.mentor.ems.entitlement.dao.EventProcessDAO;
import com.mentor.ems.entitlement.event.EntitlementEvent;
import com.mentor.ems.entitlement.event.STDSalesOrderEntitlementEventImpl;

public class OrderLineItemWriter implements ItemWriter<EventProcess> {

	@Autowired
	@Qualifier("STDSalesOrderEntitlementEventImpl")
	EntitlementEvent entitlementEvent;

	@Autowired
	private EventProcessDAO eventProcessDAO;

	@Override
	public void write(List<? extends EventProcess> eventProcesses) throws Exception {
		List<EventProcess> eventProcessList = new ArrayList<>();
		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String tDay = formatter.format(today);
		Date d=formatter.parse(tDay);
		for (EventProcess eventProcess : eventProcesses) {
			Date requestedDeliveryDate = getRequestedDeliveryDate(eventProcess);
			if (d.compareTo(requestedDeliveryDate)==0) {
				eventProcessList.add(eventProcess);
			}
		}
		List<EventProcess> orderHeaderEventProcess = new ArrayList<>();
		Event event = null;
		if (!eventProcessList.isEmpty()) {
			event = eventProcesses.get(0).getEvent();
			List<EventProcess> eventProces = eventProcessDAO.getEventProcessesByEventKey(event.getEventKey());
			if (!eventProces.isEmpty()) {
				for (EventProcess evp : eventProces) {
					if (EMSCommonConstants.ORDER_HEADER.equals(evp.getPayloadName())) {
						orderHeaderEventProcess.add(evp);
					}
				}
			}
		}
		List<EventProcessErrorDtls> errorList = new ArrayList<>();
		if (!eventProcessList.isEmpty()) {
			for (EventProcess eventProcess : eventProcessList) {
				entitlementEvent.validateWFEventProcess(eventProcess, errorList, orderHeaderEventProcess);
			}
		}
	}

	/**
	 * This method will give the requestedDeliveryDate from an EventProcess
	 * 
	 * @param event
	 */
	private Date getRequestedDeliveryDate(EventProcess eventProcess) {
		List<EventProcessParam> eventProcessParams = eventProcess.getEventProcessParams();
		Date requestedDeliveryDate = null;
		for (EventProcessParam eventProcessParam : eventProcessParams) {
			if (eventProcessParam.getParamName().equalsIgnoreCase(EMSCommonConstants.REQUESTED_DELIVERY_DATE)) {
				requestedDeliveryDate = eventProcessParam.getDateValue();
			}
		}
		return requestedDeliveryDate;
	}

}
