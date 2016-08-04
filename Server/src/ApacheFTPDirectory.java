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
 * FTP�ϴ������ļ��У���������Ŀ¼
 * */
public class ApacheFTPDirectory {
	//�½�����ʼ����֤����֤��FTP���Ӷ���
	public FTPSClient ftpClient = new FTPSClient(true);
	//public FTPClent ftpClient = new FTPClient();//����֤����֤��FTP���Ӷ���
	
	private String localFileFullName = "";//��Ҫ�ϴ���Ŀ¼
	
	public ApacheFTPDirectory(){
		//��������ʹ�õ����������������̨
		FTPClientConfig config = new FTPClientConfig();
	
		this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}
	
	/**
	 * ���ӵ�FTP������
	 * @param hostname ������
	 * @param port �˿�
	 * @param username �û���
	 * @param password ����
	 * @return �Ƿ����ӳɹ�
	 * @throws IOException IO�쳣
	 * */
	public  boolean connect(String hostname,int port,String username,String password) throws IOException{
		ftpClient.connect(hostname.trim(),port);
		ftpClient.setBufferSize(1024*1024*100);//����FTP������ 100M
		ftpClient.setControlEncoding("GBK");
		ftpClient.setDataTimeout(10000); //���ô��䳬ʱʱ��10��
		ftpClient.setConnectTimeout(10000); //�������ӳ�ʱ10��
		
		int reply = ftpClient.getReplyCode();
		
		if(FTPReply.isPositiveCompletion(reply)){
			boolean loginResult = ftpClient.login(username, password);
			int retrunCode = ftpClient.getReplyCode();
			
			if(loginResult && FTPReply.isPositiveCompletion(retrunCode)){
				System.out.println("���ӳɹ�");
				return true;
			}			
		}else{//����ʧ��
			//�Ͽ�����
			disconnect();
		}
		
		return true;
	}
	
	/**
	 * �Ͽ��������������
	 * @throws IOException
	 * */
	public void disconnect() throws IOException{
		//�ж�FTPClient�Ƿ����ӣ�������ر�
		if(ftpClient.isConnected()){
			System.out.println("FTP server refused connection.");
			ftpClient.disconnect();
			System.exit(1);
		}
	}
	
	/**
	 * �����ļ��У��������ļ���
	 * @param dir �ļ���·��
	 * @param ftpClient FTPClient����
	 * */
	private  void createDir(String dir) throws Exception{
		connect("127.0.0.1",990,"ftptest","123456");
		StringTokenizer st = new StringTokenizer(dir,"/");
		st.countTokens();
		String pathName = "";
		//ѭ������dir��ַ���Ƿ����ļ���
		while(st.hasMoreTokens()){
			pathName = pathName + "/" +(String)st.nextElement();
			try{
				//�����ļ���
				if(!ftpClient.changeWorkingDirectory(pathName)){
					//����Ŀ¼
					if(ftpClient.makeDirectory(pathName)){
						ftpClient.changeWorkingDirectory(pathName);//��ת��ָ���ļ���
					}else{
						System.out.println("����Ŀ¼ʧ��");
						break;
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ����ļ����Ƿ����
	 * @param dir �ļ���·��
	 * @return ���ؽ���������ļ���true��������false
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
	 * �ϴ������ļ�
	 * @param source �����ļ�
	 * @throws Exception
	 * */
	private void processFile(File source) throws Exception{
		if(source.exists()){//����ļ��Ƿ����
			String dir = source.getPath().substring(localFileFullName.length()).replace("\\", "/");
			if(source.isDirectory()){//����ļ��Ƿ�Ϊ�ļ��У������Դ����ļ���
				if(!isDirExist(dir)){//�Ƿ�����ļ��У��������򴴽�
					createDir(dir);
				}
				
				//��ȡ�ļ��б�
				File sourceFile[] = source.listFiles();
				for(int i = 0; i < sourceFile.length;i++){
					if(sourceFile[i].exists()){//����ļ��Ƿ����
						if(sourceFile[i].isDirectory()){//����Ƿ����ļ��У�����ݹ���ñ���
							this.processFile(sourceFile[i]);							
						}else{//�ϴ��ļ�
							//����PassiveMode���䣬����ģʽ���Բ��ÿ��ǿͻ��˵ķ���ǽ���
							ftpClient.enterLocalPassiveMode();
							//�����Զ��������ķ�ʽ����
							ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
							ftpClient.setControlEncoding("GBK");
							
							boolean status = false;
							//���ϴ��ļ�����
							String fileName = sourceFile[i].toString();
							File f = sourceFile[i];
							//��ȡ������������ϴ��ļ�ͬ�����ļ���������ڸ��ļ�����С�ڱ����ļ���ϵ����������������ϴ�
							FTPFile[] files = ftpClient.listFiles(new String(fileName.getBytes("GBK"),"iso-8859-1"));
							if(files.length == 1){//�ļ��Ѵ���
								//���������ļ���С
								long remoteSize = files[0].getSize();
								
								//�����ļ���С
								long localSize = f.length();
								if(remoteSize == localSize){
									System.out.println("�ļ��Ѵ���");
								}else if(remoteSize > localSize){
									System.out.println("�������ļ����ڱ����ļ�");
								}
								
								
								status = uploadFile(fileName,f,ftpClient,remoteSize);
								
								//����ϵ�����û�гɹ�����ɾ�����������ļ��������ϴ�
								if(!status){
									if(!ftpClient.deleteFile(fileName)){
										System.out.println("�ϵ��������ɹ����ļ�ɾ��ʧ��");
									}
									//�����ϴ�
									status = uploadFile(fileName, f, ftpClient, 0);
								}
							}else{//�ļ�������
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
	 * �ϴ��ļ��������������ϴ��Ͷϵ�����
	 * @param remoteFile Զ���ļ��������ϴ�֮ǰ�Ѿ�������������Ŀ¼���˸ı�
	 * @param localFile �����ļ�File���������·��
	 * @param ftpClient FTPClient����
	 * @param remoteSize Զ���ļ���С
	 * @return �ļ��ϴ�״̬���ɹ�true��ʧ��false
	 * @param IOException
	 * */
	public boolean uploadFile(String remoteFile,File localFile,FTPClient ftpClient,long remoteSize) throws IOException{
		boolean status;
		//�ļ��ϴ����ȼ�����λ
		long step = localFile.length() / 100;
		//�ϴ�����
		long process = 0;
		long localreadybytes = 0L;
		//��ȡ�����ļ�
		RandomAccessFile raf = new RandomAccessFile(localFile,"r");
		OutputStream out = ftpClient.appendFileStream(new String(remoteFile.getBytes("GBK"),"iso-8859-1"));
		
		//�ϵ�����
		if(remoteSize >0){
			ftpClient.setRestartOffset(remoteSize);
			process =remoteSize / step;
			//�����ļ���¼
			raf.seek(remoteSize);
			localreadybytes = remoteSize;
		}
		
		byte[] bytes = new byte[1];
		int c;
		
		//�ϴ�
		while((c = raf.read(bytes)) != -1){
			out.write(bytes,0,c);
			localreadybytes += c;
			long nowProcess = localreadybytes / step;
			if(nowProcess != process){
				process = nowProcess;
				System.out.println("�ϴ����ȣ�" + process);
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
