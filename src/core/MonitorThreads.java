package core;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import common.NotifyThreadFactory;
import common.TrackingExecutorService;
import net.sf.json.JSONObject;
import common.CommonHttpClient;

public class MonitorThreads {
	private final NotifyThreadFactory notifyThreadFactory;
	private final TrackingExecutorService service;
	private final LinkedBlockingQueue<Runnable> workQueue;
	private CommonHttpClient httpToZabbix;
	private static final AtomicInteger taskNumber = new AtomicInteger();
	
	/**
	 * the number of thread is limited ,and the size of the queue is also fixed
	 * when the queue is fulled , we'll drop the task
	 */
	public MonitorThreads(int threadNum, int capacity, CommonHttpClient httpToZabbix) {
		System.out.println("!!!!!! monitor threads initiates");
		notifyThreadFactory =  new NotifyThreadFactory();
	    workQueue = new LinkedBlockingQueue<Runnable>(capacity);
		service = new TrackingExecutorService(new ThreadPoolExecutor(threadNum, threadNum, 0L, 
				TimeUnit.SECONDS, workQueue, notifyThreadFactory, new ThreadPoolExecutor.AbortPolicy()));
		this.httpToZabbix = httpToZabbix;
	}
	/*
	 * flag = 0 -> hostRegister
	 * flag = 1 -> itemCreate
	 */
	public void handler(JSONObject params,HashMap<String,String> map, int flag) {
		try {
			switch(flag) {
				case  0 :
					service.execute(new Runnable() {
						public void run() {
							String vnfcNodeId = String.valueOf(params.get("vnfcNodeId"));
							JSONObject request = new JSONObject();
							request.put("type", "registerHost");
							request.put("body", params);
							String hostId = httpToZabbix.doPost(request.toString());
							map.put(vnfcNodeId,hostId);
						}
					});
					break;
				case 1 :
					service.execute(new Runnable() {
						public void run() {
							String monitorConfigId = String.valueOf(params.get("itemName"));
							JSONObject request = new JSONObject();
							request.put("type", "createItem");
							request.put("body", params);
							String itemId = httpToZabbix.doPost(request.toString());
							System.out.println("itemId is " + itemId);
							map.put(monitorConfigId,itemId);
						}
					});
					break;
				case 2 :
					service.execute(new Runnable() {
						public void run() {
							System.out.println("in the execute\n");
							System.out.println(params);
							String itemId = String.valueOf(params.get("itemids"));
							JSONObject request = new JSONObject();
							request.put("type", "getHistory");
							request.put("body", params);
							String itemValue = httpToZabbix.doPost(request.toString());
							System.out.println("itemValue is " + itemValue);
							map.put(itemId,itemValue);
						}
					});			
					break;
			}
		}catch(RejectedExecutionException e) {
			int taskNumLocal = taskNumber.incrementAndGet();
			//the work queue is full , and the task will be drop
			System.out.println("the queue is blocked, task " + taskNumLocal + " has been dropped");
		}
	}
	/**
	 * waiting for the task which is in queue or running to finish 
	 */
	public void slowStop() {
		try {
			service.shutdown();
			service.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS);
		}
		finally {
			System.out.println("Service Finish");
		}
	}
	
	
    /**
     * cancel all running tasks and clear the queue	
     */
	public void quickStop() {
		try {
			System.out.println("quickStop begin");
			int tasksInQueue = service.shutdownNow().size();
			System.out.println("the number of the tasks in queue has been cancelled is " + tasksInQueue);
			int tasksAtRun = service.getTaskCancel().size();
			System.out.println("the number of the tasks at run has been cancelled is " + tasksAtRun);
		}catch(IllegalStateException e) {
			System.out.println("quickStop error");
		}
		finally {
			System.out.println("quickStop finish");
		}
	}
	
}
