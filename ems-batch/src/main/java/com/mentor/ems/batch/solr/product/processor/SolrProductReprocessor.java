/**
 * 
 */
package com.mentor.ems.batch.solr.product.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mentor.ems.common.entity.EmsJobParam;
import com.mentor.ems.common.exception.ServiceException;
import com.mentor.ems.common.solr.entity.ProductSearchDTO;
import com.mentor.ems.entitlement.dto.ProductJobPayloadDTO;
import com.mentor.ems.product.service.SolrProductService;

/**
 * @author M1028004
 *
 */
@Component
public class SolrProductReprocessor implements ItemProcessor<EmsJobParam, ProductJobPayloadDTO>{

private static final Logger LOGGER = LoggerFactory.getLogger(SolrProductReprocessor.class);
	
	@Autowired
	private SolrProductService productService;

	@Override
	public ProductJobPayloadDTO process(EmsJobParam emsJobParam){
		LOGGER.info("Reading Part Numbers from EMS_JOB_PARAM table  having job_param _key  " + emsJobParam.getJobParamKey()+" and Job Param Name"+"__"+emsJobParam.getJobParamName());
		return getProductJobRequestDTO(emsJobParam);
	}

	private ProductJobPayloadDTO getProductJobRequestDTO(EmsJobParam emsJobParam) {
		ProductJobPayloadDTO payload = new ProductJobPayloadDTO();
		payload.setEmsJobParam(emsJobParam);
		payload.setRequestDTO(getProductSearchDTO(emsJobParam));
		return payload;
	}

	private ProductSearchDTO getProductSearchDTO(EmsJobParam emsJobParam) {
		ProductSearchDTO productSearch = null;
			ProductSearchDTO productSearchDTO = new ProductSearchDTO();		
			productSearchDTO.setId(emsJobParam.getStringValue());
			productSearchDTO.setPartNumber(emsJobParam.getStringValue());
			productSearchDTO.setCreateTs(emsJobParam.getDateValue());
          try {
        	  productSearch = productService.updateProduct(productSearchDTO);
		} catch (ServiceException e) {
			LOGGER.info("Error in SolrProductReprocessor class -getProductSearchDTO to read part Numbers",e);
		}
		
		return productSearch;
	
	}

}
