package core;
import java.util.TimerTask;

import net.sf.json.JSONObject;

public class AlarmTimerTask extends TimerTask{
	private AlarmFormat alarmFormat;
	private Mongo mongo;
	public AlarmTimerTask(AlarmFormat alarmFormat, Mongo mongo) {
		System.out.println("!!!!!! alarmTimerTask initiates , the alarm id is " + 
				alarmFormat.getAlarmId() + "\n");
		this.alarmFormat = alarmFormat;
		this.mongo = mongo;
	}

	@Override
	public void run() {
		JSONObject vnfsMonitor = new JSONObject();
		for(String vnf : this.alarmFormat.getVnfSet()) {
			JSONObject vnfMonitor = this.mongo.getVnfMonitorTarget(this.alarmFormat.getNsTypeId(), vnf);
			vnfsMonitor.put(vnf, vnfMonitor);
		}
		this.alarmFormat.copy(vnfsMonitor);
	}
}
