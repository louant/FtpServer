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

/**
 * FTP传输类，包括连接、文件上传、文件下载
 * */
public class ApacheFTP {
	//新建并初始化带证书验证的FTP连接对象
	public FTPSClient ftpClient = new FTPSClient(true);
	//public FTPClent ftpClient = new FTPClient();//不带证书验证的FTP连接对象
	
	
	/**
	 * 构造函数
	 * */
	public ApacheFTP(){
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
	public boolean connect(String hostname,int port,String username,String password) throws IOException{
		ftpClient.connect(hostname.trim(),port);
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
	 * 从FTP服务器上下载文件，支持断点续传，上传百分百汇报
	 * @param remote 远程文件路径
	 * @param local 本地文件路径
	 * @return 下载状态  true成功 false失败
	 * @throws IOException
	 * */
	public String downLoad(String remote,String local) throws IOException{		
		//设置被动模式
		ftpClient.enterLocalPassiveMode();
		//设置以二进制方式传输
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		//返回结果 
		String result = "";
		//远程文件列表 
		FTPFile[] files = ftpClient.listFiles(new String(remote.getBytes("GBK"),"ISO-8859-1"));
		
		if(files.length != 1){
			System.out.println("远程文件不存在");
			result = "远程文件不存在"; 
		}		
		
		//远程文件大小 
		long lRemoteSize = files[0].getSize();
		File f = new File(local);
		
		//本地存在文件，进行断点下载
		if(f.exists()){
			long localSize = f.length();
			//判断本地文件大小是否大于远程文件大小
			if(localSize >= lRemoteSize){
				result = "本地文件大院远程文件，下载中止";
			}
			
			/**
			 *进行断点续传，并记录状态
			 * */
			//把本地文件转换成文件输出流
			FileOutputStream out = new FileOutputStream(f,true);
			//设置断点续传的本地文件大小
			ftpClient.setRestartOffset(localSize);
			InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes("GBK"),"iso-8859-1"));
			byte[] bytes = new byte[1];
			//文件下载进度计数单位
			long step = lRemoteSize / 100;
			//定义远程文件以step块划分，所得的总份数（即总进度）
			long process = localSize / step;
			int c;
			//文件下载，并记录下载进度
			while((c = in.read(bytes)) != -1){
				out.write(bytes, 0, c);
				localSize += c;
				//定义当前进度
				long nowProcess = localSize / step;
				if(nowProcess > process){
					process = nowProcess;
//					if(process % 10 == 0){
						System.out.println("下载进度：" + process);
//					}
				}
			}
			//关闭输入流
			in.close();
			//关闭输出流
			out.close();
			boolean isDo = ftpClient.completePendingCommand();
			//验证ftp命令是否正确执行
			if(isDo){
				result = "FTP服务器文件断点续传下载成功";
			}else{
				result = "FTP服务器文件断点续传下载失败";
			}
		}else{//本地文件不存在，直接下载
			OutputStream out = new FileOutputStream(f);
			InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes("GBK"),"iso-8859-1"));
			byte[] bytes = new byte[1];
			//文件下载进度计数单位
			long step = lRemoteSize / 100;
			//下载进度
			long process = 0;
			//本地文件大小
			long localSize = 0L;
			//当前下载的文件大小
			int c;
			while((c = in.read(bytes)) != -1){
				out.write(bytes,0,c);
				localSize += c;
				long nowProcess = localSize / step;
				if(nowProcess > process){
					process = nowProcess;
//					if(process % 10 ==0){
						System.out.println("下载进度："+process);
//					}
				}
			}//end while
			in.close();
			out.close();
			boolean upNewStatus = ftpClient.completePendingCommand();
			if(upNewStatus){
				result = "FTP服务器文件下载成功";
			}else{
				result = "FTP服务器文件下载失败";
			}
		}
		
		return result;
	} 
	
	/**
	 * 上传文件到FTP服务器，支持断点续传
	 * @param local 本地文件名称，绝对路径
	 * @param remote 远程文件路径，支持多级目录，支持递归创建不存在的目录结构
	 * @return 上传结果
	 * @throws IOException
	 * */
	public String upload(String local,String remote) throws IOException{
		String result = "";
		//设置PassiveMode传输，被动模式可以不用考虑客户端的防火墙情况
		ftpClient.enterLocalPassiveMode();
		//设置以二进制流的方式传输
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		ftpClient.setControlEncoding("GBK");
		
		/** 对远程目录的处理 */
		String remoteFileName = remote;
		if(remoteFileName.contains("/")){
			remoteFileName = remote.substring(remote.lastIndexOf("/") + 1);
			//
		}
		return result;
	}
	
	/**
	 * 递归创建远程服务器目录
	 * @param remote 远程服务器文件绝对路径
	 * @param ftpClient FTPClient对象
	 * @return 目录创建是否成功 成功true，失败false
	 * @throws IOException
	 * */
	public boolean CreateDirectory(String remote,FTPClient ftpClient) throws IOException{
		boolean status = true;
		//远程服务器文件路径
		String directory = remote.substring(0,remote.lastIndexOf("/") + 1);
		if(!directory.equalsIgnoreCase("/") && !ftpClient.changeWorkingDirectory(new String(directory.getBytes("GBK"),"iso-8859-1"))){
			
		}
		return status;
	}
	
	/**
	 * 主函数
	 * */
	public static void main(String[] args){
		ApacheFTP myFtp = new ApacheFTP();
		try{
			System.err.println(myFtp.connect("127.0.0.1", 990, "ftptest", "123456"));
//			myFtp.connect("127.0.0.1", 990, "ftptest", "123456");
			System.out.println(myFtp.downLoad("./HA-FileZillaServer.rar","F:/text/HA-FileZillaServer.rar"));
			System.exit(0);
//			myFtp.disconnect();
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("FTP连接错误："+ e.getMessage());
		}
	}
}

class DownloadStatus{
	
}
