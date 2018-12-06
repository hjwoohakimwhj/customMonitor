package core;
import java.util.TimerTask;

public class MonitorTimerTask extends TimerTask{
	private MonitorFormat monitorFormat;
	private Mongo mongo;
	private MonitorThreads monitorThreads;
	public MonitorTimerTask(MonitorFormat monitorFormat, Mongo mongo, MonitorThreads monitorThreads) {
		System.out.println("!!!!!! monitor formate initiates, monitor target name is" +
				monitorFormat.getMonitorTarget() + "\n" );
		this.monitorFormat = monitorFormat;
		this.mongo = mongo;
		this.monitorThreads = monitorThreads;
	}

	@Override
	public void run() {
		String monitorTargetValue = this.monitorFormat.request(this.monitorThreads);
		this.mongo.putMonitorTarget(this.monitorFormat.getNsTypeId(),
				this.monitorFormat.getVnfNodeId(), this.monitorFormat.getMonitorTarget(), monitorTargetValue);
	}
}
