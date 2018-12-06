package core;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class AlarmTimer {
	private Mongo mongo;
	private Timer timer;
	
	//Map<nsTypeId ,Map<AlarmId, AlarmTimerTask>>
	private Map<String, Map<String,AlarmTimerTask>> timerMap = Collections.synchronizedMap(
			new HashMap<String, Map<String, AlarmTimerTask>>());

	public AlarmTimer() {
		System.out.println("!!!!!! alarm timer initiates");
		this.mongo = new Mongo();
		this.timer = new Timer();
	}
	
	public void addTimer(AlarmFormat alarmFormat) {
		AlarmTimerTask alarmTimerTask = new AlarmTimerTask(alarmFormat, mongo);
		this.timerMap.get(alarmFormat.getNsTypeId()).put(alarmFormat.getAlarmId(), alarmTimerTask);

		String interval = alarmFormat.getInterval();
		timer.schedule(alarmTimerTask, 0, Long.valueOf(interval));
	}
	
	public void deleteTimer(String nsTypeId, String alarmId) {
		AlarmTimerTask alarmTimerTask = this.timerMap.get(nsTypeId).get(alarmId);
		alarmTimerTask.cancel();
		this.timerMap.get(nsTypeId).remove(alarmId);
	}
	
	public void stopAllTimers() {
		this.timer.cancel();
	}
	
	public boolean isEmpty() {
		return this.timerMap.isEmpty();
	}
}
