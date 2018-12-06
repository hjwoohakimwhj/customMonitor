package customDriver;
import java.util.Map;
import java.util.TimerTask;
import org.apache.commons.lang.StringUtils;
import common.SSHHelper;

public class CustomTimerTask extends TimerTask{
	private String itemId;
	private CustomRepo driverRepo;
	private int count;
	public CustomTimerTask(String itemId, CustomRepo driverRepo) {
		this.itemId = itemId;
		this.driverRepo = driverRepo;
		this.count = 0;
	}

	@Override
	public void run() {
		count++;
		String ip = this.driverRepo.getIp(this.itemId);
		Map<String, String> itemInfo = this.driverRepo.getItemInfo(this.itemId);
		String fileName = itemInfo.get("fileName");
		String command = itemInfo.get("command");
		SSHHelper ssh = new SSHHelper(ip);
		if(count == 1) {
			String cmd = this.createExtractCmd(fileName);
			ssh.execute(cmd);
		}
		String commandResultRaw = ssh.execute(command);
		String commandResult = StringUtils.substring(commandResultRaw, 1, commandResultRaw.length()-1);
		this.driverRepo.updateItemValue(this.itemId,commandResult);
		System.out.println("count is " + count);
	}
	
	public String createExtractCmd(String fileName) {
		String cmd = null;
		if(fileName.endsWith(".tar")) {
			cmd = "tar -xvf " + "/tmp/" + fileName + " -C /tmp/";
		}
		if(fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
			cmd = "tar -xzvf " + "/tmp/" +  fileName + " -C /tmp/";
		}
		return cmd;
	}
}
