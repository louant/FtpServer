import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPClientConfig;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import java.util.StringTokenizer;

/**
 * FTP上传整个文件夹，包括整个目录
 * */
public class ApacheFTPDirectory {
	//新建并初始化带证书验证的FTP连接对象
	public FTPSClient ftpClient = new FTPSClient(true);
	//public FTPClent ftpClient = new FTPClient();//不带证书验证的FTP连接对象
	
	private String localFileFullName = "";//需要上传的目录
	
	public ApacheFTPDirectory(){
		//将过程中使用到的命令输出到控制台
		FTPClientConfig config = new FTPClientConfig();
	
		this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}
	
	/**
	 * 连接到FTP服务器
	 * @param hostname 主机名
	 * @param port 端口
	 * @param username 用户名
	 * @param password 密码
	 * @return 是否连接成功
	 * @throws IOException IO异常
	 * */
	public  boolean connect(String hostname,int port,String username,String password) throws IOException{
		ftpClient.connect(hostname.trim(),port);
		ftpClient.setBufferSize(1024*1024*100);//设置FTP缓冲区 100M
		ftpClient.setControlEncoding("GBK");
		ftpClient.setDataTimeout(10000); //设置传输超时时间10秒
		ftpClient.setConnectTimeout(10000); //设置连接超时10秒
		
		int reply = ftpClient.getReplyCode();
		
		if(FTPReply.isPositiveCompletion(reply)){
			boolean loginResult = ftpClient.login(username, password);
			int retrunCode = ftpClient.getReplyCode();
			
			if(loginResult && FTPReply.isPositiveCompletion(retrunCode)){
				System.out.println("连接成功");
				return true;
			}			
		}else{//连接失败
			//断开连接
			disconnect();
		}
		
		return true;
	}
	
	/**
	 * 断开与服务器的连接
	 * @throws IOException
	 * */
	public void disconnect() throws IOException{
		//判断FTPClient是否连接，连接则关闭
		if(ftpClient.isConnected()){
			System.out.println("FTP server refused connection.");
			ftpClient.disconnect();
			System.exit(1);
		}
	}
	
	/**
	 * 创建文件夹，包括子文件夹
	 * @param dir 文件夹路径
	 * @param ftpClient FTPClient对象
	 * */
	private  void createDir(String dir) throws Exception{
		connect("127.0.0.1",990,"ftptest","123456");
		StringTokenizer st = new StringTokenizer(dir,"/");
		st.countTokens();
		String pathName = "";
		//循环遍历dir地址中是否含有文件夹
		while(st.hasMoreTokens()){
			pathName = pathName + "/" +(String)st.nextElement();
			try{
				//生成文件夹
				if(!ftpClient.changeWorkingDirectory(pathName)){
					//创建目录
					if(ftpClient.makeDirectory(pathName)){
						ftpClient.changeWorkingDirectory(pathName);//跳转到指定文件夹
					}else{
						System.out.println("创建目录失败");
						break;
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 检查文件夹是否存在
	 * @param dir 文件夹路径
	 * @return 返回结果，存在文件夹true，不存在false
	 * */
	private boolean isDirExist(String dir){
		boolean result = false;
		try{
			result =  ftpClient.changeWorkingDirectory(dir);
		}catch(Exception e){
			e.printStackTrace();			
		}
		return result;
	}
	
	/**
	 * 上传单个文件
	 * @param source 具体文件
	 * @throws Exception
	 * */
	private void processFile(File source) throws Exception{
		if(source.exists()){//检查文件是否存在
			String dir = source.getPath().substring(localFileFullName.length()).replace("\\", "/");
			if(source.isDirectory()){//检查文件是否为文件夹，是则尝试创建文件夹
				if(!isDirExist(dir)){//是否存在文件夹，不存在则创建
					createDir(dir);
				}
				
				//获取文件列表
				File sourceFile[] = source.listFiles();
				for(int i = 0; i < sourceFile.length;i++){
					if(sourceFile[i].exists()){//检查文件是否存在
						if(sourceFile[i].isDirectory()){//检查是否问文件夹，是则递归调用本放
							this.processFile(sourceFile[i]);							
						}else{//上传文件
							//设置PassiveMode传输，被动模式可以不用考虑客户端的防火墙情况
							ftpClient.enterLocalPassiveMode();
							//设置以二进制流的方式传输
							ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
							ftpClient.setControlEncoding("GBK");
							
							boolean status = false;
							//待上传文件名称
							String fileName = sourceFile[i].toString();
							File f = sourceFile[i];
							//获取服务器中与待上传文件同名的文件，如果存在该文件，且小于本地文件则断点续传，否则重新上传
							FTPFile[] files = ftpClient.listFiles(new String(fileName.getBytes("GBK"),"iso-8859-1"));
							if(files.length == 1){//文件已存在
								//服务器中文件大小
								long remoteSize = files[0].getSize();
								
								//本地文件大小
								long localSize = f.length();
								if(remoteSize == localSize){
									System.out.println("文件已存在");
								}else if(remoteSize > localSize){
									System.out.println("服务器文件大于本地文件");
								}
								
								
								status = uploadFile(fileName,f,ftpClient,remoteSize);
								
								//如果断点续传没有成功，则删除服务器上文件，重新上传
								if(!status){
									if(!ftpClient.deleteFile(fileName)){
										System.out.println("断点续传不成功后，文件删除失败");
									}
									//重新上传
									status = uploadFile(fileName, f, ftpClient, 0);
								}
							}else{//文件不存在
								status = uploadFile(fileName, f, ftpClient, 0);
							}
						}
					}
				}
			}
		}else{
			
		}
	}
	
	/**
	 * 上传文件到服务器，新上传和断点续传
	 * @param remoteFile 远程文件名，在上传之前已经将服务器工作目录做了改变
	 * @param localFile 本地文件File句柄，绝对路径
	 * @param ftpClient FTPClient引用
	 * @param remoteSize 远程文件大小
	 * @return 文件上传状态，成功true，失败false
	 * @param IOException
	 * */
	public boolean uploadFile(String remoteFile,File localFile,FTPClient ftpClient,long remoteSize) throws IOException{
		boolean status;
		//文件上传进度计数单位
		long step = localFile.length() / 100;
		//上传进度
		long process = 0;
		long localreadybytes = 0L;
		//读取本地文件
		RandomAccessFile raf = new RandomAccessFile(localFile,"r");
		OutputStream out = ftpClient.appendFileStream(new String(remoteFile.getBytes("GBK"),"iso-8859-1"));
		
		//断点续传
		if(remoteSize >0){
			ftpClient.setRestartOffset(remoteSize);
			process =remoteSize / step;
			//访问文件记录
			raf.seek(remoteSize);
			localreadybytes = remoteSize;
		}
		
		byte[] bytes = new byte[1];
		int c;
		
		//上传
		while((c = raf.read(bytes)) != -1){
			out.write(bytes,0,c);
			localreadybytes += c;
			long nowProcess = localreadybytes / step;
			if(nowProcess != process){
				process = nowProcess;
				System.out.println("上传进度：" + process);
			}
		}//end uploadfile
		out.flush();
		raf.close();
		out.close();
		boolean result = ftpClient.completePendingCommand();
		if(result){
			status = true;
		}else{
			status = false;
		}
		
		return status;
	}
	
	public static void main(String[] args){
		ApacheFTPDirectory testFtp = new ApacheFTPDirectory();		
		try{
			testFtp.connect("127.0.0.1", 990, "ftptest", "12456");
			testFtp.processFile(new File("/text"));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
