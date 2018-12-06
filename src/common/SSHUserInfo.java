package common;
import com.jcraft.jsch.UserInfo;

public class SSHUserInfo implements UserInfo {
  
  @Override
  public String getPassphrase() {
    System.out.println("MyUserInfo.getPassphrase()");
    return null;
  }
  
  @Override
  public String getPassword() {
    System.out.println("MyUserInfo.getPassword()");
    return null;
  }
  
  @Override
  public boolean promptPassphrase(String arg0) {
    System.out.println("MyUserInfo.promptPassphrase()");
    System.out.println(arg0);
    return false;
  }
  
  @Override
  public boolean promptPassword(String arg0) {
    System.out.println("MyUserInfo.promptPassword()"); 
    System.out.println(arg0);
    return false;
  }
  
  @Override
  public boolean promptYesNo(String arg0) {
     System.out.println("MyUserInfo.promptYesNo()"); 
     System.out.println(arg0); 
     if (arg0.contains("The authenticity of host")) { 
       return true; 
     } 
    return true;
  }
  
  @Override
  public void showMessage(String arg0) {
    System.out.println("MyUserInfo.showMessage()"); 
  }
}