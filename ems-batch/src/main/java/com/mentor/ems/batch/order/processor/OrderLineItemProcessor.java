package com.mentor.ems.batch.order.processor;

import org.springframework.batch.item.ItemProcessor;

import com.mentor.ems.common.entity.EventProcess;

public class OrderLineItemProcessor implements ItemProcessor<EventProcess, EventProcess> {

	@Override
	public EventProcess process(EventProcess eventProcess) throws Exception {
		return eventProcess;
	}

}
