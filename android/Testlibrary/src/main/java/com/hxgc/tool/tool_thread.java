package com.hxgc.tool;


public class tool_thread 
{
	static public boolean Sleep(long _i_l_time)
	{
		try 
		{
			Thread.sleep(_i_l_time);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	static public long GetCurrentThreadID()
	{
		return Thread.currentThread().getId();
	}
}
