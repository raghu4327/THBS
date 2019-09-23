/**
 * 
 */
package com.mentor.ems.batch.solr.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;

import com.mentor.ems.common.entity.EmsJob;

import com.mentor.ems.entitlement.dto.JobPayloadDTO;

/**
 * @author ysingh
 *
 */
public class OrderJobListener implements ItemReadListener<JobPayloadDTO>, ItemProcessListener<EmsJob, JobPayloadDTO>,
StepExecutionListener, JobExecutionListener, ChunkListener, SkipListener<EmsJob, JobPayloadDTO>,
ItemWriteListener<JobPayloadDTO>{
	
	private static int  varaible =0;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderJobListener.class);

	@Override
	public void afterWrite(List<? extends JobPayloadDTO> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeWrite(List<? extends JobPayloadDTO> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWriteError(Exception arg0, List<? extends JobPayloadDTO> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSkipInProcess(EmsJob arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSkipInRead(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSkipInWrite(JobPayloadDTO arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterChunk(ChunkContext arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterChunkError(ChunkContext arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeChunk(ChunkContext arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterJob(JobExecution arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeJob(JobExecution arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ExitStatus afterStep(StepExecution arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void beforeStep(StepExecution arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterProcess(EmsJob arg0, JobPayloadDTO arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeProcess(EmsJob arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProcessError(EmsJob arg0, Exception arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterRead(JobPayloadDTO arg0) {
		LOGGER.info("After Reading job" + arg0.getEmsJob() );
		varaible += 1;
		LOGGER.info(" Total number of jobs   "+ varaible);
	}

	@Override
	public void beforeRead() {
		LOGGER.info("Before Reading job");
		
	}

	@Override
	public void onReadError(Exception e) {
		LOGGER.info(" Exception occured during read  " +e);
		
	}

} 
