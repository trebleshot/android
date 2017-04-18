package com.genonbeta.TrebleShot.service;

import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * Created by: veli
 * Date: 4/15/17 12:15 AM
 */

public class JobSchedulerService extends JobService
{
	@Override
	public boolean onStartJob(JobParameters jobParameters)
	{
		return false;
	}

	@Override
	public boolean onStopJob(JobParameters jobParameters)
	{
		return false;
	}
}
