package com.mentor.ems.batch.solr.entitlement.processor;

import com.mentor.ems.common.solr.constants.SolrConstants;
import com.mentor.ems.common.solr.entity.EntitlementSearchDTO;
import com.mentor.ems.common.util.StringUtil;
import com.mentor.ems.entitlement.dao.EntitlementDerivedAttributesDAO;
import com.mentor.ems.entitlement.dao.EntitlementCommentsDAO;

import com.mentor.ems.entitlement.dto.EntitlementCommentsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SolrEntitlementProcessor implements ItemProcessor<EntitlementSearchDTO,EntitlementSearchDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrEntitlementProcessor.class);

    @Autowired
    private EntitlementDerivedAttributesDAO entitlementDerivedAttributesDAO;

    @Autowired
    private EntitlementCommentsDAO entitlementCommentsDAO;

    @Override
    public EntitlementSearchDTO process(EntitlementSearchDTO item) {
        try {
            updateEntitlementRecord(item);
        } catch (Exception e) {
            LOGGER.error("process() method failed to process EntitlementSearchDTO. Entitlement Number=" + item.getEntitlementNbr(),e);
        }
        return item;
    }

    private EntitlementSearchDTO updateEntitlementRecord(EntitlementSearchDTO item) {
        if(SolrConstants.ENTITLEMENT_TYPE_NAME_PRODUCT.equalsIgnoreCase(item.getEntitlementTypeName()))
        {
            updateExpDateMultiple(item);
        } else {
            LOGGER.warn("Update EXT_DATE_MULTIPLE for entitlement skipped for Entitlement item.getEntitlementTypeName()=" +
                    item.getEntitlementTypeName());
        }
        return updateEntitlementComments(item);
    }

    private EntitlementSearchDTO updateEntitlementComments(EntitlementSearchDTO item) {
        String installedItemId = item.getInstalledItemId();
        List<EntitlementCommentsDTO> entitlementCommentsDTOList;
        if(!StringUtil.isNullOrEmpty(installedItemId)) {
            entitlementCommentsDTOList = entitlementCommentsDAO.getInstalledItemCommentsDTO(installedItemId);
            List<String> installedItemCommentsList = new ArrayList<>();
            Date installedItemLastModifiedDate = null;

            if (entitlementCommentsDTOList != null && !entitlementCommentsDTOList.isEmpty()) {
                installedItemCommentsList = entitlementCommentsDTOList.stream().
                        map(EntitlementCommentsDTO::getComment).
                        collect(Collectors.toList());
                installedItemLastModifiedDate = entitlementCommentsDTOList.stream().map(EntitlementCommentsDTO::getCommentCreateTimestamp).
                        max(Date::compareTo).get();
            } else {
                LOGGER.warn("No Installed Item comments found for " + installedItemId + " with ENTITLEMENT_NBR " + item.getEntitlementNbr());
            }
            item.setInstalledItemComments(installedItemCommentsList);
            item.setInstalledItemCommentLastModifiedDate(installedItemLastModifiedDate);

        } else {
            LOGGER.warn("installedItemId is blank or empty for ENTITLEMENT_NBR " + item.getEntitlementNbr());
        }
        return  item;
    }

    private EntitlementSearchDTO updateExpDateMultiple(EntitlementSearchDTO item) {
        item.setLicExpDateMultiple(entitlementDerivedAttributesDAO.isEntitlementLicenseExpiryDateMultiple(item.getEntitlementNbr()));
        return item;
    }

}
