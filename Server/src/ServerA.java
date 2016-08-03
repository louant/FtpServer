import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

/**
 * 角色-服务器A
 * */
public class ServerA {
	
	public static void main(String[] args){
		final String F_DIR = "c:/test/"; //跟路径
		final int PORT = 2200;//listen the port
		Logger.getRootLogger();//创建日志记录器——复制出来日志记录的大部分操作
		Logger logger = Logger.getLogger("com");
//		System.setProperty("log4j.configuration", "file:/C:/workspace/COM_tests/log4j.properties"); 
		
		try{
			ServerSocket s = new ServerSocket(PORT);//创建服务的socket套接字
			logger.info("Connecting to server A ...");
			logger.info("Connected Successful! Local Port:"+s.getLocalPort() + ". Default Directory:'"+F_DIR+"'.");
			
			//开启服务
			while(true){
				//接受客户端请求
				Socket client = s.accept();//创建客户端接收套接字
				//创建服务线程
				new ClientThread(client,F_DIR).run();
			}
		}catch(Exception e){
			logger.error(e.getMessage());
			for(StackTraceElement ste : e.getStackTrace()){
				logger.error(ste.toString());
			}
		}
	}
}
