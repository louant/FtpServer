
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
	        /**
	         * 传输参数命令
	         * */
	        //PORT命令，主动模式传输数据
            else if(command.toUpperCase().startsWith("PORT")){
            	logger.info("(" + username +") (" + clientIp + ")> "+command);
            	if(loginStuts){
            		try{
            			str = command.substring(4).trim();
            			port_low = Integer.parseInt(str.substring(str.lastIndexOf(",")+1));
            			port_high = Integer.parseInt(str.substring(0,str.lastIndexOf(",")).substring(str.substring(0,str.lastIndexOf(",")).lastIndexOf(",")));
            			String str1 = str.substring(0,str.substring(0,str.lastIndexOf(",")).lastIndexOf(","));
            			retr_ip = str1.replace(",",".");
            			try{
            				//实例化主动模式下的socket
            				tempsocket = new Socket(retr_ip,port_high * 256 + port_low);
            				pw.println("200 port command successful");
            				pw.flush();
            				logger.info("("+username+") ("+clientIp+")> 200 port command successful");
            			}catch(ConnectException ce){
            				pw.println("425 Can't open data connection");
            				pw.flush();
            				logger.info("("+username+") ("+clientIp+")> 425 Can't open data connection.");
            				logger.error(ce.getMessage());
            				for(StackTraceElement ste : ce.getStackTrace()){
            					logger.error(ste.toString());
            				}
            			}catch(UnknownHostException e){
            				logger.error(e.getMessage());
            				for(StackTraceElement ste : e.getStackTrace()){
            					logger.error(ste.toString());
            				}
            			}catch(IOException e){
            				logger.error(e.getMessage());
            				for(StackTraceElement ste : e.getStackTrace()){
            					logger.error(ste.toString());
            				}
            			}
            		}catch(NumberFormatException e){
            			pw.println("503 Bad sequence of commands.");
            			pw.flush();
            			logger.info("("+username+") ("+clientIp+")> 503 Bad sequence of commands.");
            			logger.error(e.getMessage());
            			for(StackTraceElement ste : e.getStackTrace()){
            				logger.error(ste.toString());
            			}
            		}
            	}else{
            		pw.println(LOGIN_WARNING);
            		pw.flush();
            		logger.info("("+username+") ("+clientIp+")> "+LOGIN_WARNING);
            	}
            }//end PORT
	        //PASV命令，被动模式传输数据
            else if(command.toUpperCase().startsWith("PASV")){
            	logger.info("(" + username +") (" + clientIp +")>" +command);
            	if(loginStuts){
            		ServerSocket ss = null;
            		while(true){
            			//获取服务器空闲端口
            			port_high = 1 + generator.nextInt(20);
            			port_low = 100 + generator.nextInt(1000);
            			try{
            				//服务器绑定端口
            				ss = new ServerSocket(port_high * 256 + port_low);
            				break;
            			}catch(IOException e){
            				continue;
            			}
            		}
            		
            		InetAddress i = null;
            		try{
            			i = InetAddress.getLocalHost();
            		}catch(UnknownHostException e1){
            			e1.printStackTrace();
            		}
            		pw.println("227 Entering Passive Mode ("+i.getHostAddress().replace(".", ",")+","+port_high+","+port_low+")");  
                    pw.flush();  
                    logger.info("("+username+") ("+clientIp+")> 227 Entering Passive Mode ("+i.getHostAddress().replace(".", ",")+","+port_high+","+port_low+")");  
                    try{
                    	//被动模式下的socket
                    	tempsocket = ss.accept();
                    	ss.close();
                    }catch(IOException e){
                    	logger.error(e.getMessage());
                    	for(StackTraceElement ste : e.getStackTrace()){
                    		logger.error(ste.toString());
                    	}
                    }
            	}else{
            		pw.println(LOGIN_WARNING);
            		pw.flush();
            		logger.info("("+username+") ("+clientIp+")> "+LOGIN_WARNING);
            	}
            }//end PASV
	        
	        //RETR命令
            else if(command.toUpperCase().startsWith("RETR")){
            	logger.info("("+username+") ("+clientIp+")> "+command);
            	if(loginStuts){
            		str = command.substring(4).trim();
            		if("".equals(str)){
            			pw.println("501 Syntax error");
            			pw.flush();
            			logger.info("("+username+") ("+clientIp+")> 501 Syntax error");
            		}else{
            			try{
            				pw.println("150 Opening data channel for file transfer");
            				pw.flush();
            				logger.info("("+username+") ("+clientIp+")> 150 Opening data channel for file transfer.");
            				RandomAccessFile outfile = null;//随机读取文件
            				OutputStream outsocket = null;//输出流
            				try{
            					//创建从中读取和向其中写入（可选）的随机访问文件流，该文件具有指定名
            					outfile = new RandomAccessFile(dir+"/"+str,"r");
            					outsocket = tempsocket.getOutputStream();
            				}catch(FileNotFoundException e){
            					logger.error(e.getMessage());
            					for(StackTraceElement ste : e.getStackTrace()){
            						logger.error(ste.toString());
            					}
            				}catch(IOException e){
            					logger.error(e.getMessage());
            					for(StackTraceElement ste : e.getStackTrace()){
            						logger.error(ste.toString());
            					}
            				}
            				byte bytebuffer[] = new byte[1024];//字节类型数组
            				int length;
            				try{
            					while((length = outfile.read(bytebuffer)) != -1){
            						outsocket.write(bytebuffer,0,length);
            					}
            					outsocket.close();
            					outfile.close();
            					tempsocket.close();
            				}catch(IOException e){
            					logger.error(e.getMessage());
            					for(StackTraceElement ste : e.getStackTrace()){
            						logger.error(ste.toString());
            					}
            				}
            				pw.println("226 Transfer ok");
            				pw.flush();
            				logger.info("("+username+") ("+clientIp+")> 226 Transfer OK");
            			}catch(Exception e){
            				pw.println("503 Bad sequnce of commands.");
            				pw.flush();
            				logger.info("("+username+") ("+clientIp+")> 503 Bad sequence of commands.");
            				logger.error(e.getMessage());
            				for(StackTraceElement ste : e.getStackTrace()){
            					logger.error(ste.toString());
            				}
            			}
            		}
            	}else{
            		pw.println(LOGIN_WARNING);
            		pw.flush();
            		logger.info("("+username+") ("+clientIp+")> "+LOGIN_WARNING);
            	}
            }//end RETR
	        
	        //STOR 命令
            else if(command.toUpperCase().startsWith("STOR")){
            	logger.info("("+username+") ("+clientIp+")> "+command);
            	if(loginStuts){
            		str = command.substring(4).trim();
            		if("".equals(str)){//命令为空
            			pw.println("501 Syntax error");
            			pw.flush();
            			logger.info("("+username+") ("+clientIp+")> 501 Syntax error");
            		}else{
            			try{
            				pw.println("150 Opening data channel for file transfer.");
            				pw.flush();
            				logger.info("("+username+") ("+clientIp+")> 150 Opening data channel for file transfer.");
            				RandomAccessFile infile = null;
            				InputStream insocket = null;
            				try{
            					infile = new RandomAccessFile(dir + "/" +str,"rw");
            					insocket = tempsocket.getInputStream();
            				}catch(FileNotFoundException e){
            					logger.error(e.getMessage());
            					for(StackTraceElement ste : e.getStackTrace()){
            						logger.error(ste.toString());
            					}
            				}catch(IOException e){
            					logger.error(e.getMessage());
            					for(StackTraceElement ste : e.getStackTrace()){
            						logger.error(ste.toString());
            					}
            				}
            				
            				byte bytebuffer[] = new byte[1024];
            				int length;
            				try{
            					while((length = insocket.read(bytebuffer)) != -1){
            						infile.write(bytebuffer,0,length);
            					}
            					insocket.close();
            					infile.close();
            					tempsocket.close();
            				}catch(IOException e){
            					logger.error(e.getMessage());
            					for(StackTraceElement ste : e.getStackTrace()){
            						logger.error(ste.toString());
            					}
            				}
            				
            				pw.println("226 Transfer OK");
            				pw.flush();
            				logger.info("("+username+") ("+clientIp+")> 226 Transfer OK");  
            			}catch(Exception e){
            				pw.println("503 Bad sequence of commands.");  
                            pw.flush();  
                            logger.info("("+username+") ("+clientIp+")> 503 Bad sequence of commands.");  
                            logger.error(e.getMessage());  
                            for(StackTraceElement ste : e.getStackTrace()){  
                                logger.error(ste.toString());  
                            }  
            			}
            		}
            	}else{
            		pw.println(LOGIN_WARNING);
            		pw.flush();
            		logger.info("("+username+") ("+clientIp+")> "+LOGIN_WARNING);
            	}
            }//end STOR
	        
	        //NLST命令  
            else if(command.toUpperCase().startsWith("NLST")) {   
                logger.info("("+username+") ("+clientIp+")> "+command);  
                if(loginStuts){  
                    try {  
                        pw.println("150 Opening data channel for directory list.");   
                        pw.flush();  
                        logger.info("("+username+") ("+clientIp+")> 150 Opening data channel for directory list.");  
                        PrintWriter pwr = null;  
                        try {  
                            pwr= new PrintWriter(tempsocket.getOutputStream(),true);  
                        } catch (IOException e1) {  
                            e1.printStackTrace();  
                        }   
                        File file = new File(dir);   
                        String[] dirstructure = new String[10];   
                        dirstructure= file.list();   
                        for(int i=0;i<dirstructure.length;i++){  
                            pwr.println(dirstructure[i]);   
                        }  
                        try {  
                            tempsocket.close();  
                            pwr.close();  
                        } catch (IOException e) {  
                            logger.error(e.getMessage());  
                            for(StackTraceElement ste : e.getStackTrace()){  
                                logger.error(ste.toString());  
                            }  
                        }   
//                      logger.info("用户"+clientIp+"："+username+"执行NLST命令");  
                        pw.println("226 Transfer OK");   
                        pw.flush();  
                        logger.info("("+username+") ("+clientIp+")> 226 Transfer OK");  
                    } catch (Exception e){  
                        pw.println("503 Bad sequence of commands.");  
                        pw.flush();  
                        logger.info("("+username+") ("+clientIp+")> 503 Bad sequence of commands.");  
                        logger.error(e.getMessage());  
                        for(StackTraceElement ste : e.getStackTrace()){  
                            logger.error(ste.toString());  
                        }  
                    }  
                }else{  
                    pw.println(LOGIN_WARNING);  
                    pw.flush();  
                    logger.info("("+username+") ("+clientIp+")> "+LOGIN_WARNING);  
                }  
            } //end NLST  
              
            //LIST命令  
            else if(command.toUpperCase().startsWith("LIST")) {   
                logger.info("("+username+") ("+clientIp+")> "+command);  
                if(loginStuts){  
                    try{  
                        pw.println("150 Opening data channel for directory list.");   
                        pw.flush();  
                        logger.info("("+username+") ("+clientIp+")> 150 Opening data channel for directory list.");  
                        PrintWriter pwr = null;  
                        try {  
                            pwr= new PrintWriter(tempsocket.getOutputStream(),true);  
                        } catch (IOException e) {  
                            logger.error(e.getMessage());  
                            for(StackTraceElement ste : e.getStackTrace()){  
                                logger.error(ste.toString());  
                            }  
                        }   
                        FtpUtil.getDetailList(pwr, dir);  
                        try {  
                            tempsocket.close();  
                            pwr.close();  
                        } catch (IOException e) {  
                            logger.error(e.getMessage());  
                            for(StackTraceElement ste : e.getStackTrace()){  
                                logger.error(ste.toString());  
                            }  
                        }   
//                      logger.info("用户"+clientIp+"："+username+"执行LIST命令");  
                        pw.println("226 Transfer OK");   
                        pw.flush();  
                        logger.info("("+username+") ("+clientIp+")> 226 Transfer OK");  
                    } catch (Exception e){  
                        pw.println("503 Bad sequence of commands.");  
                        pw.flush();  
                        logger.info("("+username+") ("+clientIp+")> 503 Bad sequence of commands.");  
                        logger.error(e.getMessage());  
                        for(StackTraceElement ste : e.getStackTrace()){  
                            logger.error(ste.toString());  
                        }  
                    }  
                } else {  
                    pw.println(LOGIN_WARNING);  
                    pw.flush();  
                    logger.info("("+username+") ("+clientIp+")> "+LOGIN_WARNING);  
                }  
            } //end LIST
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
