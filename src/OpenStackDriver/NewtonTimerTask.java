package OpenStackDriver;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import org.apache.commons.lang.StringUtils;
import common.SSHHelper;

public class NewtonTimerTask extends TimerTask{
	private String serverId;
	private NewtonHttpClient httpClient;
	private DriverRepo driverRepo;
	private int count;
	public NewtonTimerTask(String serverId, NewtonHttpClient httpClient, DriverRepo driverRepo) {
		this.serverId = serverId;
		this.httpClient = httpClient;
		this.driverRepo = driverRepo;
		this.count = 0;
	}

	@Override
	public void run() {
		count++;
		Map<String, Map<String, String>> result = this.httpClient.doGet(this.serverId);
		String ip = this.driverRepo.getServerIp(this.serverId);
		Map<String, Map<String,String>> commands = this.driverRepo.getItemMap();
		SSHHelper ssh = new SSHHelper(ip);
		for(String type : commands.keySet()) {
			Map<String, String> typeEntity = new HashMap<String, String>();
			for(String item : commands.get(type).keySet()) {
				String command = commands.get(type).get(item);
				String commandResultRaw = ssh.execute(command);
				String commandResult = StringUtils.substring(commandResultRaw, 1, commandResultRaw.length()-1);
				typeEntity.put(item, commandResult);
			}
			result.put(type, typeEntity);
		}
		this.driverRepo.refresh(this.serverId,result);
		System.out.println("count is " + count);
	}
}
