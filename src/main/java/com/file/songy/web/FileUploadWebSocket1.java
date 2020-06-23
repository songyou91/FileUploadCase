package com.file.songy.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

@ServerEndpoint("/fileUploadStart")
public class FileUploadWebSocket1 {

	//表示连接websocket数量
	private static int onlineCount = 0;
	private Session session = null;
	//引入log4j日志
	private static Log log = LogFactory.getLog(FileUploadWebSocket1.class);
	private static CopyOnWriteArraySet<FileUploadWebSocket1> fileList = new CopyOnWriteArraySet<FileUploadWebSocket1>();

	//文件分块大小
	private static final long BLOCK_SIZE=1024L*7;
	
	//文件上传临时路径
    private static String SERVER_TEMP_PATH = "";
    
    //文件上传存放真实路径
    private static String SERVER_REAL_PATH = "";
    //重复文件上传覆盖
    private static int coverFile = 0;
    //定义一个map，存放文件名称、文件大小
    private static Map<String,String> mapSession = new HashMap<String,String>();
	@OnOpen
	public void OnOpen(Session session){
		this.session = session;
		fileList.add(this);
		// 判断上传路径是否为空，为空时从配置文件中获取
		if(SERVER_TEMP_PATH == null || "".equals(SERVER_TEMP_PATH) || 
				SERVER_REAL_PATH == null || "".equals(SERVER_REAL_PATH)){
			try {
				InputStream inputstream = new BufferedInputStream(
						new FileInputStream(Thread.currentThread()
								.getContextClassLoader().getResource(
										"file.properties").getPath()));
				// .. 属性文件工具类
				Properties proper = new Properties();
				// .. 加载 ..
				proper.load(inputstream);
				inputstream.close();
				Object obj = (String) proper.get("fileTempPath");
				if(obj != null){
					SERVER_TEMP_PATH = obj.toString();
				}
				Object obj1 = (String) proper.get("fileRealPath");
				if(obj1 != null){
					SERVER_REAL_PATH = obj1.toString();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				log.error("读取配置文件失败,文件未发现!");
			} catch (IOException e) {
				e.printStackTrace();
				log.error("读取配置文件失败,读取文件流错误!");
			}
			if(SERVER_TEMP_PATH == null || SERVER_REAL_PATH == null){
				log.info("上传文件的临时目录或真实目录为空!");
			}else if(SERVER_TEMP_PATH.equals(SERVER_REAL_PATH)){
				log.info("上传文件的临时目录与文件存放真实目录相同,请修改!");
			}else{
				File fileTemp = new File(SERVER_TEMP_PATH);
				if(!fileTemp.exists()){
					fileTemp.mkdirs();
				}
				File fileReal = new File(SERVER_REAL_PATH);
				if(!fileReal.exists()){
					fileReal.mkdirs();
				}
			}
		}else{
			File fileTemp = new File(SERVER_TEMP_PATH);
			if(!fileTemp.exists()){
				fileTemp.mkdirs();
			}
			File fileReal = new File(SERVER_REAL_PATH);
			if(!fileReal.exists()){
				fileReal.mkdirs();
			}
		}
		addOnlineCount();
		log.info("webSocket服务端有新客户端接入！"+getOnlineCount());
	}
	@OnMessage
	public void OnMessage(Session session,String msg) throws IOException{
		log.info("客户端发来消息:"+msg);
		if(coverFile == 1){
			coverFile = 0;
		}
		msg = msg.replaceAll("\"", "\'");
		JSONObject jso = JSON.parseObject(msg);
		JSONObject obj = new JSONObject();
		long fileSize = jso.getLong("size");
		String fileName = jso.getString("name");
		//将当前websocket连接id、上传文件名放入map
		mapSession.put(session.getId(), fileName);
		//将要上传的文件大小保存到map中，以备后面使用，同时解决多个客户端同时上传文件问题
		mapSession.put(session.getId()+fileName, Long.toString(fileSize));
		File file = new File(SERVER_TEMP_PATH + File.separator +fileName);
		//文件上传二进制流开始位置
	    long startByte=0;
	    //文件上传二进制流结束位置
	    long stopByte=0;
		/*
		 * status 0表示正在上传，1表示上传文件已存在，2表示上传文件完成
		 */
		if(file.exists()){
			if(fileSize == file.length()){
				if(BLOCK_SIZE > fileSize){
					stopByte = fileSize;
				}else{
					stopByte = BLOCK_SIZE;
				}
				obj.put("stopFile", stopByte);
				obj.put("startFile", startByte);
				obj.put("status", 1);
				coverFile = 1;
				session.getBasicRemote().sendText(obj.toString());
			}else{
				startByte = file.length();
				stopByte = startByte+BLOCK_SIZE;
				obj.put("stopFile", stopByte);
				obj.put("startFile", startByte);
				obj.put("status", 0);
				session.getBasicRemote().sendText(obj.toString());
			}
		}else{
			if(BLOCK_SIZE > fileSize){
				stopByte = fileSize;
			}else{
				stopByte = BLOCK_SIZE;
			}
			obj.put("stopFile", stopByte);
			obj.put("startFile", startByte);
			obj.put("status", 0);
			session.getBasicRemote().sendText(obj.toString());
		}
	}
	@OnMessage
	public void uploadFileList(byte[] buf,Session session) throws IOException{
		saveFileFunc(buf,session);
	}
	@OnError
	public void OnError(Session session,Throwable error) throws IOException{
		error.printStackTrace();
		session.getBasicRemote().sendText("服务端操作出错,请检测");
		log.info("服务端webSocket操作出错!");
	}
	@OnClose
	public void OnClose(){
		fileList.remove(this);
		subOnlineCount();
		log.info("客户端断开了连接!");
	}
	//获取当前webSocket连接数
	public static synchronized int getOnlineCount(){
		return onlineCount;
	}
	//有新客户端接入时增加连接数
	public static synchronized int addOnlineCount(){
		return onlineCount++;
	}
	//客户端断开连接时减少连接数
	public static synchronized int subOnlineCount(){
		return onlineCount--;
	}
	
	public void saveFileFunc(byte[] b,Session session) throws IOException{
		String sessionID = session.getId();
		//根据wobsocket连接id获取当前上传文件名
		String fileName = mapSession.get(sessionID);
		File file = new File(SERVER_TEMP_PATH + File.separator + fileName);
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
			if(coverFile == 0){
				raf.seek(file.length());  				
			}else{
				raf.seek(0);
			}
			raf.write(b);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(raf != null){
				raf.close();
			}
		}
		long startByte = file.length();
		long stopByte = startByte+BLOCK_SIZE;
		JSONObject obj = new JSONObject();
		String fileSizeTemp = mapSession.get(sessionID+fileName);
		long fileSize = 0l;
		if(fileSizeTemp != null && !"".equals(fileSizeTemp)){
			fileSize = Long.valueOf(fileSizeTemp);
		}
		if(stopByte > fileSize){
			if(startByte == fileSize){
				obj.put("stopFile", 0);
				obj.put("startFile", 0);
				obj.put("status", 2);
				//文件上传完成后删除map中的数据
				mapSession.remove(sessionID);
				mapSession.remove(sessionID+fileName);
				fileMove(fileName);
			}else{
				obj.put("stopFile", fileSize);
				obj.put("startFile", startByte);
				obj.put("status", 0);
			}
		}else{
			obj.put("stopFile", stopByte);
			obj.put("startFile", startByte);
			obj.put("status", 0);
		}
		session.getBasicRemote().sendText(obj.toString());
	}
	//文件上传成功后移动文件到指定目录下
	public static void fileMove(String fileTempName){
		File file = new File(SERVER_TEMP_PATH + File.separator + fileTempName);
		//移动文件原始路径
		String sourceFilePath = SERVER_TEMP_PATH + File.separator + fileTempName;
		//移动文件目标路径
		String targetFilePath = SERVER_REAL_PATH +File.separator + fileTempName;
		if(file.exists()){
			BufferedInputStream inBuff = null;
			BufferedOutputStream outBuff = null;
			try{
				inBuff = new BufferedInputStream(new FileInputStream(
						new File(sourceFilePath)));
				outBuff = new BufferedOutputStream(new FileOutputStream(
						new File(targetFilePath)));
				int len;
				byte[] b = new byte[1024 * 5];
				while ((len = inBuff.read(b)) != -1) {
					outBuff.write(b, 0, len);
				}
				outBuff.flush();
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(inBuff != null){
					try {
						inBuff.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(outBuff != null){
					try {
						outBuff.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				file.delete();
			}
		}
	}
}
