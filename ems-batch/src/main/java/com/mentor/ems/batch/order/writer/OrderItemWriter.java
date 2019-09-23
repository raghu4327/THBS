/**
 * 
 */
package com.mentor.ems.batch.order.writer;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.dao.EventDao;
import com.mentor.ems.common.entity.Event;
import com.mentor.ems.common.entity.EventParam;
import com.mentor.ems.common.exception.DataAccessException;
import com.mentor.ems.entitlement.service.ProductOrderService;

/**
 * @author ysingh
 *Class is used to  process Sales Order
 */
@Service
public class OrderItemWriter implements ItemWriter<Event> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderItemWriter.class);

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private ProductOrderService productOrderService;

	@Autowired
	private EventDao eventDao;

	@Override
	public void write(List<? extends Event> failedEventList) throws Exception {
		LOGGER.info("Enter into FailedEventWriter having FailedEventList " + failedEventList + "and size is "
				+ failedEventList.size());
		LOGGER.debug("Failed Event List "+failedEventList);
		Long eventId = null;
		Date failedEventTimeStamp = null;
		Date recentCompletedTimeStamp = null;
		Event recentCompletedOrderEvent = null;
		try {

			for (Event failedEvent : failedEventList) {
				eventId = failedEvent.getEventKey();
				LOGGER.info("Processing for failed event with eventid start::" + eventId);
				// get Order Number
				String salesDocumentId = getSalesDocumentId(failedEvent.getEventParams());
				// get Recent Completed Order Event
				recentCompletedOrderEvent = eventDao.fetchRecentCompletedOrderEvent(salesDocumentId);
				if(null != recentCompletedOrderEvent){
				// get recent event completed Time Stamp
				recentCompletedTimeStamp = getTimeStamp(recentCompletedOrderEvent.getEventParams());
				// get failed event timestamp				
				}
				failedEventTimeStamp = getTimeStamp(failedEvent.getEventParams());
				LOGGER.error("TimeStamp for failed event is ::" + failedEventTimeStamp);
				if (null == recentCompletedTimeStamp || checkTimeStamp(failedEventTimeStamp, recentCompletedTimeStamp)) {
					productOrderService.processOrder(failedEvent);
				} else {
					eventDao.updateEventStatus(eventId, EMSCommonConstants.IGNORED);

				}

			}

		} catch (DataAccessException e) {
			LOGGER.error("Exception while updating failed event with eventId ", eventId, e);
		} catch (Exception e) {
			LOGGER.error("Exception while updating failed event with eventId ", eventId, e);

		}
		LOGGER.info("Exit from FailedEventWriter");

	}

	private String getSalesDocumentId(List<EventParam> eventParams) {
		String orderNumber = null;
		for (EventParam params : eventParams) {
			if (EMSCommonConstants.SALES_DOCUMENT_ID.equals(params.getEventParamName())) {
				orderNumber = params.getStringValue();
				LOGGER.info("Order Number for failed event is ::" + orderNumber);
				break;
			}

		}
		return orderNumber;

	}

	private Date getTimeStamp(List<EventParam> paramList) {
		Date timeStamp = null;
		for (EventParam params : paramList) {
			if (EMSCommonConstants.SALES_DOCUMENT_LAST_MODIFY_DATE.equals(params.getEventParamName())) {
				timeStamp = params.getDateValue();
				LOGGER.error("TimeStamp for event is ::" + timeStamp);
				break;
			}

		}
		return timeStamp;
	}

	private boolean checkTimeStamp(Date failedTimeStamp, Date completedTimeStamp) throws ParseException {
		boolean value = false;
		if (failedTimeStamp.after(completedTimeStamp)) {
			value = true;
		}
		return value;
	}

}
