package com.file.songy.web;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

@ServerEndpoint("/fileUpload")
public class FileUploadWebSocket {

	//标示连接websocket数量
	private static int onlineCount = 0;
	private Session session = null;
	private static CopyOnWriteArraySet<FileUploadWebSocket> fileList = new CopyOnWriteArraySet<FileUploadWebSocket>();
	private static String fileName=null;

	private static final long BLOCK_SIZE=1024L*7;  
    private static final int BYTE_BUFFER_SIZE=1024*1024*10;
    private static final String SERVER_SAVE_PATH = "d:/";
    
    private long startByte=0;  
    private long stopByte=0;
    private static Long fileSize=0L;
	@OnOpen
	public void OnOpen(Session session){
		this.session = session;
		fileList.add(this);
		addOnlineCount();
		System.out.println("webSocket服务端有新客户端接入！"+getOnlineCount());
	}
	@OnMessage
	public void OnMessage(Session session,String msg) throws IOException{
		System.out.println("客户端发来消息:"+msg);
		//fileName = msg;
		msg = msg.replaceAll("\"", "\'");
		JSONObject jso = JSON.parseObject(msg);
		JSONObject obj = new JSONObject();
		fileSize = jso.getLong("size");
		fileName = jso.getString("name");
		File file = new File(SERVER_SAVE_PATH+fileName);
		String fileExist = jso.getString("fileExist");
		/*执行断点续传时，判断文件是否存在，不存在则进行提示，询问是否直接上传
		 * fileExist 0表示新文件上传，1表示断点续传
		 * status 0表示正在上传，1表示续传文件不存在，2表示续传文件已完成，3表示上传的文件已经存在，4表示上传文件完成
		 */
		if("1".equals(fileExist)){
			if(file.exists()){
				if(fileSize == file.length()){
					obj.put("status", 2);
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
				obj.put("status", 1);
				session.getBasicRemote().sendText(obj.toString());
			}
		}else{
			if(file.exists() && fileSize == file.length()){
				obj.put("status", 3);
				session.getBasicRemote().sendText(obj.toString());
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
	}
	@OnMessage
	public void uploadFileList(byte[] buf,Session session) throws IOException{
		saveFileFunc(buf,session);
	}
	@OnError
	public void OnError(Session session,Throwable error) throws IOException{
		error.printStackTrace();
		session.getBasicRemote().sendText("服务端操作出错,请检测");
		System.out.println("服务端webSocket操作出错!");
	}
	@OnClose
	public void OnClose(){
		fileList.remove(this);
		subOnlineCount();
		System.out.println("客户端断开了连接!");
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
		File file = new File(SERVER_SAVE_PATH+fileName);
		BufferedOutputStream bos = null;
		RandomAccessFile raf;
		try {
			//FileOutputStream fos = new FileOutputStream(file);
			//bos = new BufferedOutputStream(fos);
			raf = new RandomAccessFile(file, "rw");
			raf.seek(file.length());  
			raf.write(b);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(bos != null){
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		startByte = file.length();
		stopByte = startByte+BLOCK_SIZE;
		JSONObject obj = new JSONObject();
		if(stopByte > fileSize){
			if(startByte == fileSize){
				obj.put("stopFile", 0);
				obj.put("startFile", 0);
				obj.put("status", 4);
				obj.put("proccess", 100);
			}else{
				obj.put("stopFile", fileSize);
				obj.put("startFile", startByte);
				obj.put("status", 0);
				obj.put("proccess", startByte/fileSize*100);
			}
		}else{
			obj.put("stopFile", stopByte);
			obj.put("startFile", startByte);
			obj.put("status", 0);
			obj.put("proccess", startByte/fileSize*100);
		}
		session.getBasicRemote().sendText(obj.toString());
	}
	public static void main(String[] args) {
		//String str = "{'opcode':1,'name':'test.docx','size':10732,'lastModifiedDate':'2016-08-02T06:33:14.931Z'}";
		//JSONObject obj = JSON.parseObject(str);
		//System.out.println(obj.get("name"));
		File file = new File("d:/11.txt");
		System.out.println(file.exists());
	}
}
