package jp.co.aeoncredit.coupon.batch.common;

import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

public abstract class BatchMainBase implements Batchlet{

	BatchConfigfileLoader conf ;
	public BatchConfigfileLoader getConfigLoader(String jobid) {
		if (conf  != null)  {
			return conf;
		}
		conf = new BatchConfigfileLoader(jobid);
		return conf;
	}
	@Inject
	JobContext jobContext;
	/**
	 * Get Job Context
	 * @return
	 */
	public JobContext getJobContext() {
		return this.jobContext;
	}
	/**
	 * Set Exit Status
	 * @param exitStatus
	 * @return
	 */
	public String setExitStatus(String exitStatus) {
		this.jobContext.setExitStatus(exitStatus);
		return exitStatus;
	}
	
	@Override
	public void stop() throws Exception {
		
	}
	
	/**
	 * 
	 * @return
	 */
	protected Logger getLogger() {
		Logger logger = LoggerFactory.getInstance().getLogger(this);
		return logger;
	}
}
