package common;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
  
public class SSHHelper {
	//ipAddress
	private String ip;
	//login userName
	private String username;
	//login password
	private String password;
	//remote port
	public static final int DEFAULT_SSH_PORT = 22; 
	//output
	private ArrayList<String> stdout;
  
	public SSHHelper(final String ip, final String username, final String password) {
		this.ip = ip;
		this.username = username;
		this.password = password;
		stdout = new ArrayList<String>();
	}
	
	public SSHHelper(final String ip) {
		this.ip = ip;
		this.username = "root";
		this.password = "123456";
		stdout = new ArrayList<String>();
	}
  

	public String execute(final String command) {
		JSch jsch = new JSch();
		SSHUserInfo userInfo = new SSHUserInfo();
  
		try {
			//创建session并且打开连接，因为创建session之后要主动打开连接
			Session session = jsch.getSession(username, ip, DEFAULT_SSH_PORT);
			session.setPassword(password);
			session.setUserInfo(userInfo);
			session.connect();
  
			//打开通道，设置通道类型，和执行的命令
			Channel channel = session.openChannel("exec");
			ChannelExec channelExec = (ChannelExec)channel;
			channelExec.setCommand(command);
  
			channelExec.setInputStream(null);
			BufferedReader input = new BufferedReader(new InputStreamReader
					(channelExec.getInputStream()));
  
			channelExec.connect();
			System.out.println("The remote command is :" + command);
  
			//接收远程服务器执行命令的结果
			String line;
			while ((line = input.readLine()) != null) { 
				System.out.println("line is " + line);
				stdout.add(line); 
			} 
			System.out.print("out the while and command is" + command);
			input.close(); 
  
			// 关闭通道
			channelExec.disconnect();
			//关闭session
			session.disconnect();
  
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this.stdout.toString();
	}

  
/*	public static void main(final String [] args) { 
		SSHHelper shell = new SSHHelper("192.168.0.121", "root", "123456");
		String result = shell.execute("fdisk -l |grep Disk |head -n 1 | awk -F \" \" '{print $3$4}' | awk -F \",\" '{print $1}'");
		result = StringUtils.substring(result, 1, result.length()-1);
		System.out.println(result);
	} */
}