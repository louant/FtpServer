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
 * FTP�����࣬�������ӡ��ļ��ϴ����ļ�����
 * */
public class ApacheFTP {
	//�½�����ʼ����֤����֤��FTP���Ӷ���
	public FTPSClient ftpClient = new FTPSClient(true);
	//public FTPClent ftpClient = new FTPClient();//����֤����֤��FTP���Ӷ���
	
	
	/**
	 * ���캯��
	 * */
	public ApacheFTP(){
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
	public boolean connect(String hostname,int port,String username,String password) throws IOException{
		ftpClient.connect(hostname.trim(),port);
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
	 * ��FTP�������������ļ���֧�ֶϵ��������ϴ��ٷְٻ㱨
	 * @param remote Զ���ļ�·��
	 * @param local �����ļ�·��
	 * @return ����״̬  true�ɹ� falseʧ��
	 * @throws IOException
	 * */
	public String downLoad(String remote,String local) throws IOException{		
		//���ñ���ģʽ
		ftpClient.enterLocalPassiveMode();
		//�����Զ����Ʒ�ʽ����
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		//���ؽ�� 
		String result = "";
		//Զ���ļ��б� 
		FTPFile[] files = ftpClient.listFiles(new String(remote.getBytes("GBK"),"ISO-8859-1"));
		
		if(files.length != 1){
			System.out.println("Զ���ļ�������");
			result = "Զ���ļ�������"; 
		}		
		
		//Զ���ļ���С 
		long lRemoteSize = files[0].getSize();
		File f = new File(local);
		
		//���ش����ļ������жϵ�����
		if(f.exists()){
			long localSize = f.length();
			//�жϱ����ļ���С�Ƿ����Զ���ļ���С
			if(localSize >= lRemoteSize){
				result = "�����ļ���ԺԶ���ļ���������ֹ";
			}
			
			/**
			 *���жϵ�����������¼״̬
			 * */
			//�ѱ����ļ�ת�����ļ������
			FileOutputStream out = new FileOutputStream(f,true);
			//���öϵ������ı����ļ���С
			ftpClient.setRestartOffset(localSize);
			InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes("GBK"),"iso-8859-1"));
			byte[] bytes = new byte[1];
			//�ļ����ؽ��ȼ�����λ
			long step = lRemoteSize / 100;
			//����Զ���ļ���step�黮�֣����õ��ܷ��������ܽ��ȣ�
			long process = localSize / step;
			int c;
			//�ļ����أ�����¼���ؽ���
			while((c = in.read(bytes)) != -1){
				out.write(bytes, 0, c);
				localSize += c;
				//���嵱ǰ����
				long nowProcess = localSize / step;
				if(nowProcess > process){
					process = nowProcess;
//					if(process % 10 == 0){
						System.out.println("���ؽ��ȣ�" + process);
//					}
				}
			}
			//�ر�������
			in.close();
			//�ر������
			out.close();
			boolean isDo = ftpClient.completePendingCommand();
			//��֤ftp�����Ƿ���ȷִ��
			if(isDo){
				result = "FTP�������ļ��ϵ��������سɹ�";
			}else{
				result = "FTP�������ļ��ϵ���������ʧ��";
			}
		}else{//�����ļ������ڣ�ֱ������
			OutputStream out = new FileOutputStream(f);
			InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes("GBK"),"iso-8859-1"));
			byte[] bytes = new byte[1];
			//�ļ����ؽ��ȼ�����λ
			long step = lRemoteSize / 100;
			//���ؽ���
			long process = 0;
			//�����ļ���С
			long localSize = 0L;
			//��ǰ���ص��ļ���С
			int c;
			while((c = in.read(bytes)) != -1){
				out.write(bytes,0,c);
				localSize += c;
				long nowProcess = localSize / step;
				if(nowProcess > process){
					process = nowProcess;
//					if(process % 10 ==0){
						System.out.println("���ؽ��ȣ�"+process);
//					}
				}
			}//end while
			in.close();
			out.close();
			boolean upNewStatus = ftpClient.completePendingCommand();
			if(upNewStatus){
				result = "FTP�������ļ����سɹ�";
			}else{
				result = "FTP�������ļ�����ʧ��";
			}
		}
		
		return result;
	} 
	
	/**
	 * �ϴ��ļ���FTP��������֧�ֶϵ�����
	 * @param local �����ļ����ƣ�����·��
	 * @param remote Զ���ļ�·����֧�ֶ༶Ŀ¼��֧�ֵݹ鴴�������ڵ�Ŀ¼�ṹ
	 * @return �ϴ����
	 * @throws IOException
	 * */
	public String upload(String local,String remote) throws IOException{
		String result = "";
		//����PassiveMode���䣬����ģʽ���Բ��ÿ��ǿͻ��˵ķ���ǽ���
		ftpClient.enterLocalPassiveMode();
		//�����Զ��������ķ�ʽ����
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		ftpClient.setControlEncoding("GBK");
		
		/** ��Զ��Ŀ¼�Ĵ��� */
		String remoteFileName = remote;
		//create directory
		if(remoteFileName.contains("/")){
			remoteFileName = remote.substring(remote.lastIndexOf("/") + 1);
			//����������Զ��Ŀ¼�ṹ������ʧ��ֱ�ӷ���
			if(CreateDirectory(remote,ftpClient)){
				return "�ϴ��ļ������У�����Ŀ¼�ṹʧ��";
			}
		}//end  create directory
		
		boolean status = false;
		//���Զ���Ƿ�����ļ�
		FTPFile[] files = ftpClient.listFiles(new String(remoteFileName.getBytes("GBK"),"iso-8859-1"));
		if(files.length == 1){//�ļ��Ѵ���
			long remoteSize = files[0].getSize();
			File f = new File(local);
			//�����ļ���С
			long localSize = f.length();
			if(remoteSize == localSize){
				result = "�ļ��Ѵ���";
			}else if(remoteSize > localSize){
				result = "�������ļ����ڱ����ļ�";
			}
			
			
			status = uploadFile(remoteFileName,f,ftpClient,remoteSize);
			
			//����ϵ�����û�гɹ�����ɾ�����������ļ��������ϴ�
			if(!status){
				if(!ftpClient.deleteFile(remoteFileName)){
					result = "�ϵ��������ɹ����ļ�ɾ��ʧ��";
				}
				//�����ϴ�
				status = uploadFile(remoteFileName, f, ftpClient, 0);
			}
		}else{//�ļ�������
			status = uploadFile(remoteFileName, new File(local), ftpClient, 0);
		}
		if(status){
			result = "�ļ��ϴ��ɹ�";
		}else{
			result = "�ļ��ϴ�ʧ��";
		}
		return result;
	}
	
	/**
	 * �ݹ鴴��Զ�̷�����Ŀ¼
	 * @param remote Զ�̷������ļ�����·��
	 * @param ftpClient FTPClient����
	 * @return Ŀ¼�����Ƿ�ɹ� �ɹ�true��ʧ��false
	 * @throws IOException
	 * */
	public boolean CreateDirectory(String remote,FTPClient ftpClient) throws IOException{
		boolean status = true;
		//Զ�̷������ļ�·��
		String directory = remote.substring(0,remote.lastIndexOf("/") + 1);
		//���Զ��Ŀ¼�����ڣ���ιܴ���Զ�̷�����Ŀ¼
		if(!directory.equalsIgnoreCase("/") && !ftpClient.changeWorkingDirectory(new String(directory.getBytes("GBK"),"iso-8859-1"))){
			int start = 0;
			int end = 0;
			if(directory.startsWith("/")){
				start = 1;
			}else{
				start = 0;
			}
			end = directory.indexOf("/",start);
			while(true){
				//��Ŀ¼����
				String subDirectory = new String(remote.substring(start,end).getBytes("GBK"),"iso-8859-1");
				if(!ftpClient.changeWorkingDirectory(subDirectory)){
					//����Ŀ¼
					if(ftpClient.makeDirectory(subDirectory)){
						ftpClient.changeWorkingDirectory(subDirectory);
					}else{
						System.out.println("����Ŀ¼ʧ��");
						return false;
					}
				}
				
				start = end + 1;
				end = directory.indexOf("/",start);
				
				//�������Ŀ¼�Ƿ񴴽����
				if(end <= start){
					break;
				}
			}
		}//end while
		return status;
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
	/**
	 * ������
	 * */
	public static void main(String[] args){
		ApacheFTP myFtp = new ApacheFTP();
		try{
			System.err.println(myFtp.connect("127.0.0.1", 990, "ftptest", "123456"));
//			myFtp.connect("127.0.0.1", 990, "ftptest", "123456");
//			System.out.println(myFtp.downLoad("./HA-FileZillaServer.rar","F:/text/HA-FileZillaServer.rar"));
			System.out.println(myFtp.upload("f:/text/1/sss.txt", "/1/sss.txt"));
			System.exit(0);
//			myFtp.disconnect();
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("FTP���Ӵ���"+ e.getMessage());
		}
	}
}

class DownloadStatus{
	
}
