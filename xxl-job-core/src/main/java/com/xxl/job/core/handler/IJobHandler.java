package com.xxl.job.core.handler;

/**
 * job handler
 * 任务执行器
 *
 * @author xuxueli 2015-12-19 19:06:38
 */
public abstract class IJobHandler {


	/**
	 * 执行处理程序，在执行程序接收调度请求时调用
	 * execute handler, invoked when executor receives a scheduling request
	 *
	 * @throws Exception
	 */
	public abstract void execute() throws Exception;


	/*@Deprecated
	public abstract ReturnT<String> execute(String param) throws Exception;*/

	/**
	 * 初始化执行器
	 * init handler, invoked when JobThread init
	 */
	public void init() throws Exception {
		// do something
	}


	/**
	 * 销毁执行器
	 * destroy handler, invoked when JobThread destroy
	 */
	public void destroy() throws Exception {
		// do something
	}


}
