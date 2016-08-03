import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

/**
 * ��ɫ-������A
 * */
public class ServerA {
	
	public static void main(String[] args){
		final String F_DIR = "c:/test/"; //��·��
		final int PORT = 2200;//listen the port
		Logger.getRootLogger();//������־��¼���������Ƴ�����־��¼�Ĵ󲿷ֲ���
		Logger logger = Logger.getLogger("com");
//		System.setProperty("log4j.configuration", "file:/C:/workspace/COM_tests/log4j.properties"); 
		
		try{
			ServerSocket s = new ServerSocket(PORT);//���������socket�׽���
			logger.info("Connecting to server A ...");
			logger.info("Connected Successful! Local Port:"+s.getLocalPort() + ". Default Directory:'"+F_DIR+"'.");
			
			//��������
			while(true){
				//���ܿͻ�������
				Socket client = s.accept();//�����ͻ��˽����׽���
				//���������߳�
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
