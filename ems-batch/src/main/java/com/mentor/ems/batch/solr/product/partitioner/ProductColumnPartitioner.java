package com.mentor.ems.batch.solr.product.partitioner;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

public class ProductColumnPartitioner implements Partitioner {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductColumnPartitioner.class);

	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		Map<String, ExecutionContext> result = new HashMap<>();
		for(int i=1; i <= gridSize; i++) {
			ExecutionContext value = new ExecutionContext();
			value.putInt("numBlock", i);
			result.put("Partition-" + i, value);
		}
		LOGGER.info("Partition " +result);
		return result;
	}

}
