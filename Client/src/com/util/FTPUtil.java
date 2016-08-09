package com.util;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPClientConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * FTP�����࣬�ṩFTP�����ӡ��ļ��ϴ���Ŀ¼����
 * */
public class FTPUtil {
	/** �½���֤����֤��FTP���Ӷ���*/
	public FTPSClient ftpClient;
	/** ���ϴ���ԴĿ¼*/
	private String localFileFullName;
	private boolean is_connected;
	/** FTP���� */
	private String host;
	/** FTP���Ӷ˿ں� */
	private int port;
	/** FTP�����û��� */
	private String username;
	/** FTP�������� */
	private String password;
	
	/**
	 * FTP�����๹�캯��
	 * @param host FTP������ַ
	 * @param  port FTP���Ӷ˿ں�
	 * @param username FTP�����û���
	 * @param password FTP��������
	 * */
	public FTPUtil(String host, int port, String username, String password){
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		is_connected = false;
	}
	
	public void connect(){
		/** FTP���Ӷ������� */
		ftpClient
	}
	
}
