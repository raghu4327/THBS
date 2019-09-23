package com.mentor.ems.batch.solr.customer.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.constants.EMSCommonConstants;
import com.mentor.ems.common.solr.constants.SolrConstants;
import com.mentor.ems.common.solr.entity.CustomerSearchDTO;
import com.mentor.ems.common.util.StringUtil;
import com.mentor.ems.customer.dao.CustomerSiteCommentsDAO;
import com.mentor.ems.customer.dao.SiteDao;
import com.mentor.ems.customer.dto.ExternalContactsDTO;
import com.mentor.ems.customer.dto.SiteCommentsDTO;
import com.mentor.ems.customer.service.ContactService;
import com.mentor.ems.customer.util.ContactUtils;

/**
 * Processor component - Solr Customer - CustomerSearchDTO
 * Updates CustomerSearchDTO with derived and External Contacts API info
 *
 */
@Component
public class SolrCustomerProcessor implements ItemProcessor<CustomerSearchDTO,CustomerSearchDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCustomerProcessor.class);

    @Autowired
    private ContactService contactService;

    @Autowired
    private CustomerSiteCommentsDAO customerSiteCommentsDAO;
    
    @Autowired
    private SiteDao siteDao;

    @Override
    public CustomerSearchDTO process(CustomerSearchDTO item) {
        try {
            updateSiteContacts(item);
        } catch (Exception e) {
            LOGGER.error("process() method failed to process CustomerSearchDTO. Id=" + item.getId(),e);
        }
        return item;
    }

    private CustomerSearchDTO updateSiteContacts(CustomerSearchDTO customerSearchDTO) {
        Integer siteNumber = null;
        try {
            siteNumber = new Integer(customerSearchDTO.getSiteNbrNumeric());
            updateSiteComments(siteNumber,customerSearchDTO);
            updateInternalContacts(siteNumber,customerSearchDTO);
            updateExternalContacts(siteNumber,customerSearchDTO);
        } catch (NumberFormatException nbe) {
            LOGGER.error("Unable to get Integer from " + customerSearchDTO.getSiteNbrNumeric(), nbe);
            LOGGER.error("Internal/External Contact contacts will be skipped for siteNumber : " + customerSearchDTO.getSiteNbrNumeric());
        }
        return customerSearchDTO;
    }

    private void updateSiteComments(Integer siteId, CustomerSearchDTO customerSearchDTO) {
        List<SiteCommentsDTO> siteComments = customerSiteCommentsDAO.getSiteCommentsForSite(siteId);
        List<String> siteCommentsList = new ArrayList<>();
        if(siteComments!= null && !siteComments.isEmpty()) {
            LOGGER.info("siteComments=" + siteComments);
            siteCommentsList = siteComments.stream().
                    map(SiteCommentsDTO::getSiteComment).
                    collect(Collectors.toList());
        } else {
            LOGGER.warn("No SiteComments found for siteID " + siteId);
        }
        customerSearchDTO.setSiteComments(siteCommentsList);
    }

    private void updateExternalContacts(Integer siteId, CustomerSearchDTO customerSearchDTO) {

        List<ExternalContactsDTO> externalContactsDTOS = new ArrayList<>();

        try {
            externalContactsDTOS = contactService.getExternalContacts(siteId, SolrConstants.MULE_CONTACT_API_SITE);
            LOGGER.info("siteId=" + siteId + " externalContactsDTOS=[[" + externalContactsDTOS + "]]");

            if (externalContactsDTOS!= null && !externalContactsDTOS.isEmpty()) {
                //Update the name to firstName + ' ' + lastName
                externalContactsDTOS.stream().
                        filter(e -> !StringUtil.isNullOrEmpty(e.getContactFirstName()) ||
                                !StringUtil.isNullOrEmpty(e.getContactLastName()) ).
                        forEach(e -> e.setContactName(
                                ContactUtils.getNameFromFirstLastName( e.getContactFirstName() , e.getContactLastName() )
                                )
                        );
            } else {
                LOGGER.warn("No External Contacts for SiteId "  + siteId);
            }

        } catch (Exception e) {
            LOGGER.error("Error retrieving External Contacts for SiteId "  + siteId, e);
            return;
        }
        ContactUtils.updateExternalContact(customerSearchDTO, externalContactsDTOS);
    }

    /**
	 * @param siteNumber
	 * @param customerSearchDTO
	 * @return
	 */
	private CustomerSearchDTO updateInternalContacts(Integer siteNumber, CustomerSearchDTO customerSearchDTO) {
		
		try {
			Object[] object = siteDao.getInternalContactsBySite(siteNumber);
			if (null != object) {
				populateInternalContacts(customerSearchDTO, object);
			} else {
				LOGGER.warn("No Internal Contacts for SiteId " + siteNumber);
			}
		} catch (Exception e) {
			LOGGER.error("Error retrieving Internal Contacts for SiteId " + siteNumber, e);
		}
		return customerSearchDTO;
	}
	
	/**
	 * @param customerSearchDTO
	 * @param object
	 */
	private void populateInternalContacts(CustomerSearchDTO customerSearchDTO, Object[] object) {
		customerSearchDTO.setSiteOwnerContactFirstName((String) object[0]);
		customerSearchDTO.setSiteOwnerContactLastName((String) object[2]);
		customerSearchDTO.setSiteOwnerContactID((String) object[1]);
		customerSearchDTO.setSiteOwnerContactName((String) object[0] + (String) object[2]);
		customerSearchDTO.setSiteOwnerContactType(EMSCommonConstants.ACCTOWNER_TYPE);
		customerSearchDTO.setSiteOwnerEmail((String) object[3]);
		customerSearchDTO.setSiteOwnerFax((String) object[4]);
		customerSearchDTO.setSiteOwnerPhone1((String) object[6]);
		customerSearchDTO.setSiteOwnerPhone2((String) (String) object[7]);

		customerSearchDTO.setSiteRSRContactFirstName((String) object[9]);
		customerSearchDTO.setSiteRSRContactLastName((String) object[11]);
		customerSearchDTO.setSiteRSRContactID((String) object[10]);
		customerSearchDTO.setSiteRSRContactName((String) object[9] + (String) object[11]);
		customerSearchDTO.setSiteRSRContactType(EMSCommonConstants.SMR_TYPE);
		customerSearchDTO.setSiteRSREmail((String) object[12]);
		customerSearchDTO.setSiteRSRFax((String) object[13]);
		customerSearchDTO.setSiteRSRPhone1((String) object[15]);
		customerSearchDTO.setSiteRSRPhone2((String) (String) object[16]);

		customerSearchDTO.setSiteSPOCContactFirstName((String) object[17]);
		customerSearchDTO.setSiteSPOCContactLastName((String) object[19]);
		customerSearchDTO.setSiteSPOCContactID((String) object[18]);
		customerSearchDTO.setSiteSPOCContactName((String) object[17] + (String) object[19]);
		customerSearchDTO.setSiteSPOCContactType(EMSCommonConstants.SPOC_TYPE);
		customerSearchDTO.setSiteSPOCEmail((String) object[20]);
		customerSearchDTO.setSiteSPOCFax((String) object[21]);
		customerSearchDTO.setSiteSPOCPhone1((String) object[23]);
		customerSearchDTO.setSiteSPOCPhone2((String) (String) object[24]);
		
		customerSearchDTO.setSiteSecSPOCContactFirstName((String) object[25]);
		customerSearchDTO.setSiteSecSPOCContactLastName((String) object[27]);
		customerSearchDTO.setSiteSecSPOCContactID((String) object[26]);
		customerSearchDTO.setSiteSecSPOCContactName((String) object[25] + (String) object[27]);
		customerSearchDTO.setSiteSecSPOCContactType(EMSCommonConstants.ALTERNATE_SPOC_TYPE);
		customerSearchDTO.setSiteSecSPOCEmail((String) object[28]);
		customerSearchDTO.setSiteSecSPOCFax((String) object[29]);
		customerSearchDTO.setSiteSecSPOCPhone1((String) object[30]);
		customerSearchDTO.setSiteSecSPOCPhone2((String) (String) object[31]);
	}


   
}