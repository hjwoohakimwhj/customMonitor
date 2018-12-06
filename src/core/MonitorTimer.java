package core;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class MonitorTimer {
	private Mongo mongo;
	private Timer timer;
	private MonitorThreads monitorThreads;
	
	//Map<nsTypeId ,Map<vnfId, Map<monitorTarget, monitorTimerTask>>>
	private Map<String, Map<String,Map<String,MonitorTimerTask>>> timerMap = Collections.synchronizedMap(
			new HashMap<String, Map<String, Map<String,MonitorTimerTask>>>());

	public MonitorTimer(MonitorThreads monitorThreads) {
		System.out.println("!!!!!! monitor timer initiates");
		this.mongo = new Mongo();
		this.timer = new Timer();
		this.monitorThreads = monitorThreads;
	}
	
	public void addTimer(MonitorFormat format) {
		MonitorTimerTask monitorTimerTask = new MonitorTimerTask(format, mongo, this.monitorThreads);
		
		String nsTypeId = format.getNsTypeId();
		String vnfNodeId = format.getVnfNodeId();
		String monitorTargetId = format.getMonitorTarget();
		if(!this.timerMap.containsKey(nsTypeId)){
			Map<String, MonitorTimerTask> monitorTarget = new HashMap<String, MonitorTimerTask>();
			monitorTarget.put(monitorTargetId, monitorTimerTask);
			Map<String,Map<String, MonitorTimerTask>> monitorVNF = new HashMap<String,
					Map<String, MonitorTimerTask>>();
			monitorVNF.put(vnfNodeId, monitorTarget);
			this.timerMap.put(nsTypeId, monitorVNF);
		}else if(!this.timerMap.get(nsTypeId).containsKey(vnfNodeId)) {
			Map<String, MonitorTimerTask> monitorTarget = new HashMap<String, MonitorTimerTask>();
			monitorTarget.put(monitorTargetId, monitorTimerTask);
			this.timerMap.get(nsTypeId).put(vnfNodeId, monitorTarget);
		}else {
			this.timerMap.get(nsTypeId).get(vnfNodeId).put(monitorTargetId, monitorTimerTask);
		}	
		String interval = format.getInterval();
		timer.schedule(monitorTimerTask, 0, Long.valueOf(interval));
	}
	
	public void deleteTimer(String nsTypeId, String vnf, String monitorTarget) {
		MonitorTimerTask alarmTimerTask = this.timerMap.get(nsTypeId).get(vnf).get(monitorTarget);
		alarmTimerTask.cancel();
		this.timerMap.get(nsTypeId).get(vnf).remove(monitorTarget);
	}
	
	public void stopAllTimers() {
		this.timer.cancel();
	}
	
	public boolean isEmpty() {
		return this.timerMap.isEmpty();
	}
}
