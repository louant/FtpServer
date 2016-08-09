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
 * FTP工具类，提供FTP的连接、文件上传、目录创建
 * */
public class FTPUtil {
	/** 新建带证书验证的FTP连接对象*/
	public FTPSClient ftpClient;
	/** 需上传的源目录*/
	private String localFileFullName;
	private boolean is_connected;
	/** FTP主机 */
	private String host;
	/** FTP连接端口号 */
	private int port;
	/** FTP连接用户名 */
	private String username;
	/** FTP连接密码 */
	private String password;
	
	/**
	 * FTP工具类构造函数
	 * @param host FTP主机地址
	 * @param  port FTP连接端口号
	 * @param username FTP连接用户名
	 * @param password FTP连接密码
	 * */
	public FTPUtil(String host, int port, String username, String password){
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		is_connected = false;
	}
	
	public void connect(){
		/** FTP连接对象配置 */
		ftpClient
	}
	
}
