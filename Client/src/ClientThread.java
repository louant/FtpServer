
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * 客户端子线程
 * */
public class ClientThread  extends Thread{
	private Socket socketClient;	//客户端socket
	private Logger logger;		//日志对象
	private String dir;	//绝对路径
	private String pdir = "/";		//相对路径
	private final static Random generator = new Random();//随机数
	
	/**
	 * 类的构造方法
	 * */
	public ClientThread(Socket client, String F_DIR){
		this.socketClient = client;
		this.dir = F_DIR;
	}
	public static void main(String[] args){
		run();
	}
	
	
	public void run(){

		Logger.getRootLogger();
		logger = Logger.getLogger("com");
		
		InputStream is = null;		//输入流
		OutputStream os = null;		//输出流
		
		/**
		 * 获取输出输入流
		 * */
		try{
			is = socketClient.getInputStream();		//获取客户端输入流
			os = socketClient.getOutputStream();		//获取客户端输出流
		}catch(IOException e){
			logger.error(e.getMessage());		//记录错误日志
			for(StackTraceElement ste : e.getStackTrace()){
				logger.error(ste.toString());				
			}
		}
		
		/**
		 * 缓冲读取字符
		 */
		BufferedReader br = new BufferedReader(new InputStreamReader(is,Charset.forName("utf-8")));
		PrintWriter pw = new PrintWriter(os);  //创建打印流
		String clientIp = socketClient.getInetAddress().toString().substring(1);	//记录客户端IP
		
		String username = "not logged in";	//用户名
		String password = "";	//口令
		String command = "";	//命令
		boolean loginStuts = false; 	//登录状态
		final String LOGIN_WARNING = "530 Please log in with USER and PASS first";
		String str = "";	//命令内容字符串
		int port_high = 0;
		int port_low = 0;
		String retr_ip = "";	//接收文件的IP地址
		
		Socket tempsocket = null;
		
		pw.println("220-FTP Server A version 1.0");
		pw.flush();//刷新打印流
		logger.info("("+username+") ("+clientIp + ")> Connected, sending welcome message...");
		logger.info("(" + username + ") (" + clientIp + ")> 220-FTP Server A verion 1.0");
		
	     boolean b = true;  
	        while ( b ){  
	            try {  
	                //获取用户输入的命令  
	                command = br.readLine();  
	                if(null == command) break;  
	            } catch (IOException e) {  
	                pw.println("331 Failed to get command");  
	                pw.flush();  
	                logger.info("("+username+") ("+clientIp+")> 331 Failed to get command");  
	                logger.error(e.getMessage());  
	                for(StackTraceElement ste : e.getStackTrace()){  
	                    logger.error(ste.toString());  
	                }  
	                b = false;  
	            }  	        
	        
	        /**
	         *访问控制命令
	         * */
	        //USER 命令
	        if(command.toUpperCase().startsWith("USER")){
	        	logger.info("(not logged in) (" + clientIp +")>" + command);
	        	username = command.substring(4).trim();
	        	if("".equals(username)){
	        		pw.println("501 Syntax error");
	        		pw.flush();//刷新
	        		logger.info("(not logged in) (" + clientIp +")> 501 Syntax error");
	        		username = "not logged in";
	        	}else{
	        		pw.println("331 Password required for " +username);
	        		pw.flush();
	        		logger.info("(not logged in) ("+clientIp+")> 331 Password required for " + username);
	        	}
	        	loginStuts = false;  
	        }//end USER
	        
	        //PASS命令
	        else if(command.toUpperCase().startsWith("PASS")){
	        	logger.info("(not logged in) ("+clientIp+")> "+command);
	        	password = command.substring(4).trim();
	        	if(username.equals("root") && password.equals("root")){
	        		pw.println("230 Logged on");
	        		pw.flush();
	        		logger.info("("+username+") ("+clientIp+")> 230 Logged on");  
	        		 loginStuts = true;  
	        	}else{  
	        		pw.println("530 Login or password incorrect!");
	        		pw.flush();
	        		logger.info("(not logged in) ("+clientIp+")> 530 Login or password incorrect!");
	        		username = "not logged in";
	        	}
	        }//end PASS
	        // PWD命令  
            else if(command.toUpperCase().startsWith("PWD")){  
                logger.info("("+username+") ("+clientIp+")> "+command);  
                if(loginStuts){  
                    pw.println("257\""+pdir+"\" is current directory");  
                    pw.flush();  
                    logger.info("("+username+") ("+clientIp+")> 257 \""+pdir+"\" is current directory");  
                }else{  
                    pw.println(LOGIN_WARNING);  
                    pw.flush();  
                    logger.info("("+username+") ("+clientIp+")> "+LOGIN_WARNING);  
                }  
            } //end PWD  
	     // CWD命令  
            else if(command.toUpperCase().startsWith("CWD")){
            	logger.info("("+username+") ("+clientIp+")> "+command);
            	if(loginStuts){
            		str = command.substring(3).trim();
            		if("".equals(str)){
            			pw.println("250 Broken client detected, missing argument to CWD. \""+pdir+"\" is current directory.");
            			pw.flush();
            			logger.info("("+username+") ("+clientIp+")> 250 Broken client detected, missing argument to CWD. \""+pdir+"\" is current directory.");
            		}else{
            			//判断目录是否存在
            			String tmpDir = dir + "/" + str;
            			File file = new File(tmpDir);
            			if(file.exists()){//目录存在
            				dir = dir + "/" + str;
            			}
            			 if("/".equals(pdir)){  
                             pdir = pdir + str;  
                         }else{  
                             pdir = pdir + "/" + str;  
                         }  
            			 pw.println("250 CWD successful. \""+pdir+"\" is current directory");  
                         pw.flush();  
                         logger.info("("+username+") ("+clientIp+")> 250 CWD successful.\""+pdir+"\" is current directory");
            		}
            	}
            }
	        //QUIT
            else if(command.toUpperCase().startsWith("QUIT")){
            	logger.info("("+username+") ("+clientIp+")> "+command);
            	b = false;
            	pw.println("221 Goodbye");
            	pw.flush();
            	logger.info("("+username+") ("+clientIp+")> 221 Goodbye");
            	try{
            		Thread.currentThread();
            		Thread.sleep(1000);
            	}catch(InterruptedException e){
            		logger.error(e.getMessage());
            		for(StackTraceElement ste : e.getStackTrace()){
            			logger.error(ste.toString());
            		}
            	}
            }//end QUIT
	        //输入非法命令
            else{
            	logger.info("("+username+") ("+clientIp+")> "+command);  
                pw.println("500 Syntax error, command unrecognized.");  
                pw.flush(); 
                logger.info("("+username+") ("+clientIp+")> 500 Syntax error, command unrecognized.");  
            }
	        
	}//end while
	
	try{
		logger.info("("+username+") ("+clientIp+")> disconnected.");
		br.close();//关闭缓冲读取流
		socketClient.close();//关闭客户端socket连接
		pw.close();//关闭打印流
		if(null != tempsocket){
			tempsocket.close();
		}
	}catch(IOException e){
		logger.error(e.getMessage());
		for(StackTraceElement ste : e.getStackTrace()){
			logger.error(ste.toString());
		}
	}
	}

	
}
