package customDriver;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class CustomTimer {
	private Timer timer;
	//itemId , timerTask
	private Map<String, CustomTimerTask> timerMap = new HashMap<String, CustomTimerTask>();

	public CustomTimer(long gap) {
		this.timer = new Timer();
	}
	
	public void addTimer(String itemId,  CustomRepo customRepo, String updateTime) {
		CustomTimerTask customTimerTask = new CustomTimerTask(itemId, customRepo);
		this.timerMap.put(itemId, customTimerTask);
		timer.schedule(customTimerTask, 0, Long.valueOf(updateTime));
	}
	
	public void deleteTimer(String serverId) {
		CustomTimerTask customTimerTask = this.timerMap.get(serverId);
		customTimerTask.cancel();
		this.timerMap.remove(serverId);
	}
	
	public void stopAllTimers() {
		this.timer.cancel();
	}
	
	public boolean isEmpty() {
		return this.timerMap.isEmpty();
	}
}
