package com.mentor.ems.batch.autorenewal.eldnotification.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.entity.Event;
import com.mentor.ems.entitlement.dto.ELDNotificationDTO;

@Component
public class EldAutoRenewalNotificationHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(EldAutoRenewalNotificationHelper.class);

	public void prepareELDPayload(ELDNotificationDTO notificationDTO, Event event) throws Exception{
		//if processType is AUTO_RENEWAL,based on siteNbr, fetch Site ELD contacts
		//prepare payload with siteNbr and SiteContacts.
		//Marshall the request object into XML.
		//post the request message into RabbitMQ Topic.
		}

}
