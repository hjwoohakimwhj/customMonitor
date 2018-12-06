package OpenStackDriver;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class NewtonTimer {
	private Timer timer;
	private long gap;
	//serverId , timerTask
	private Map<String, NewtonTimerTask> timerMap = new HashMap<String, NewtonTimerTask>();

	public NewtonTimer(long gap) {
		this.timer = new Timer();
		this.gap = gap;
	}
	
	public void addTimer(String serverId, NewtonHttpClient httpClient, DriverRepo driverRepo) {
		NewtonTimerTask newtonTimerTask = new NewtonTimerTask(serverId, httpClient, driverRepo);
		this.timerMap.put(serverId, newtonTimerTask);
		timer.schedule(newtonTimerTask, 0, this.gap);
	}
	
	public void deleteTimer(String serverId) {
		NewtonTimerTask newtonTimerTask = this.timerMap.get(serverId);
		newtonTimerTask.cancel();
		this.timerMap.remove(serverId);
	}
	
	public void stopAllTimers() {
		this.timer.cancel();
	}
	
	public boolean isEmpty() {
		return this.timerMap.isEmpty();
	}
}
