<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <base href="<%=basePath%>">
    
    <title>WebSocket文件上传</title>
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	<!--
	<link rel="stylesheet" type="text/css" href="styles.css">
	-->
  </head>
  <script type="text/javascript">
  	var ws = null;
  	var fileCount = 0;
  	//使用文件拖拽功能时存放文件变量
  	var fileListTemps = null;
  	//标示上传文件方式，0表示点击按钮上传，1表示拖拽文件上传
  	var uploadType = 0;
  	//上传文件时先打开socket通道在进行文件上传
	function openSocket(){
		if('WebSocket' in window){
			var fileListTemp = document.getElementById("fileName");
			var fileConll = document.getElementById("fileConll");
			if((uploadType == 0 && fileListTemp.files.length > 0) || (uploadType == 1 && fileListTemps.length > 0)){
				ws = new WebSocket("ws://127.0.0.1:8081/FileUploadCase/fileUploadStart");				
			}else{
				showMsg("请添加文件后在进行上传操作!");
			}
		}else{
			alert("该浏览器暂时不支持WebSocket");
		}
		if(ws != null){
			//webSocket连接成功回调方法
			ws.onopen = function(){
				showMsg("webSocket连接成功,可以进行通信!");
				startUpload(fileCount);
			}
			//webSocket接收服务端返回的数据信息
			ws.onmessage = function(event){
				showReturnMsg(event.data);
			}
			//webSocket连接关闭回调方法
			ws.onclose = function(){
				showMsg("webSocket连接关闭");
			}
			//webSocket连接出错回调方法
			ws.onerror = function(){
				showMsg("webSocket连接出错!");
			}
		}
	}
	//手动关闭webSocket连接
	function closeWebSocket(){
		if(ws != null){
			ws.close();
			ws = null;
		}
	}
	//页面关闭时自动关闭webSocket连接
	function onbeforeunload(){
		if(ws != null){
			ws.close();
			ws = null;
		}
	}
	//显示系统消息
	function showMsg(msg){
		document.getElementById("msg").innerHTML = msg;
	}
	//服务端返回消息显示
	function showReturnMsg(msg){
		document.getElementById("returnContent").innerHTML = msg; 
	}
	//fileCount文件数量
  	function startUpload(fileCount){
  		showReturnMsg("");
  		var fileTemp = document.getElementById("fileName");
  		var fileList = fileTemp.files;
  		if(fileList.length <= 0){
  			fileList = fileListTemps;
  		}
  		makeFileInfo(fileList[fileCount],fileCount,fileList.length);
  	}
  	//组装文件信息，如果是多个文件同时上传，则按照添加顺序进行上传
  	function makeFileInfo(files,fileIndex,fileTatalCount){
  		var buffer = null;
  		var processId = "";
		var fileName = files.name;
		var fileSize = files.size;
		var fileInfo={  
          'opcode':1,  
          'name':fileName,  
          'size':fileSize,  
          'startFile':0,
          'stopFile':0 
	    };
	    if(ws != null){
	    	ws.send(JSON.stringify(fileInfo));
		    ws.onmessage = function(event){
		  		var json=JSON.parse(event.data);
		  		//status 0表示正在上传，1表示上传文件已存在，2表示上传文件完成
		  		if(json.status == 2 ||　json.status == 3){
		  			showReturnMsg("文件上传完成");
		  			document.getElementById("fileProcess"+fileIndex).innerHTML = "100%";
		  			fileIndex = fileIndex+1;
		  			if(fileIndex < fileTatalCount){
		  				startUpload(fileIndex);
		  			}else{
		  				//文件上传完成后关闭socket通道
		  				closeWebSocket();
		  			}
		  		}else if(json.status == 1){
		  			if(confirm("上传文件已存在,是否重新上传覆盖已有文件？")){
						uploadBigFile(json.startFile,json.stopFile,files);
						appendProcess(json.startFile,files.size,fileIndex);  				
					}else{
						document.getElementById("fileProcess"+fileIndex).innerHTML = "100%";
						fileIndex = fileIndex+1;
			  			if(fileIndex < fileTatalCount){
			  				startUpload(fileIndex);
			  			}else{
			  				//文件上传完成后关闭socket通道
			  				closeWebSocket();
			  			}
					}
		  		}else{
		  			uploadBigFile(json.startFile,json.stopFile,files);
		  			appendProcess(json.startFile,files.size,fileIndex);
		  		}	
		  	}		
	    }
  	}
  	//显示文件上传进度processSize已经上传的大小，totalSize上传文件总大小，fileIndex多文件标示
  	function appendProcess(processSize,totalSize,fileIndex){
  		processSize = parseFloat(processSize);
  		totalSize = parseFloat(totalSize);
  		var percentComplete = (processSize*100 / totalSize).toFixed(2);
		processId = "fileProcess"+fileIndex;
     	document.getElementById(processId).innerHTML = percentComplete.toString() + '%';
  	}
  	//按照二进制分块的开始位置和结束位置对二进制文件进行分块并上传到服务器端
  	function uploadBigFile(startSize,stopSize,fileTemp){
  		var reader = new FileReader();
		var rawData = new ArrayBuffer();
        buffer = fileTemp.slice(startSize, stopSize);
        reader.readAsArrayBuffer(buffer);
        reader.onload = function(){
			rawData = reader.result;
			if(ws != null){
				ws.send(rawData);				
			}
		}
  	}
  	//暂停上传功能
  	function stopUpload(){
  		ws.close();
  	}
  	//添加文件后显示文件信息
  	function addFiles(fileType,fileTemps){
  		var fileConll = document.getElementById("fileConll");
  		var fileList = "";
  		if(fileType == 0){
  			var fileTemp = document.getElementById("fileName");
  			fileList = fileTemp.files;
  		}else{
  			fileList = fileTemps;
  		}
  		var str = "";
  		fileConll.innerHTML = "";
		for(var i=0;i<fileList.length;i++){
			var fileSize = "";
			if(fileList[i].size > 1024*1024*1024){
				fileSize = Math.round(fileList[i].size/(1024*1024*1024))+"GB";
			}else if(fileList[i].size > 1024*1024){
				fileSize = Math.round(fileList[i].size/(1024*1024))+"MB";
			}else{
				fileSize = Math.round(fileList[i].size/1024)+"KB";
			}
			
			var divt = document.createElement("div");
			divt.style.marginTop = "5px";
			divt.style.backgroundColor = "bisque";
			divt.style.fontSize = "18px";
			divt.style.fontWeight = "500";
			var spanFileName = document.createElement("span");
			spanFileName.innerHTML = "文件名称:"+fileList[i].name;
			divt.appendChild(spanFileName);
			var spanFileSize = document.createElement("span");
			spanFileSize.innerHTML = "文件大小:"+fileSize;
			spanFileSize.style.marginLeft = "20px";
			spanFileSize.style.fontSize = "18px";
			spanFileSize.style.fontWeight = "500";
			divt.appendChild(spanFileSize);
			var spanProccess = document.createElement("span");
			spanProccess.setAttribute("id","fileProcess"+i);
			spanProccess.style.marginLeft = "20px";
			spanProccess.style.color = "red";
			spanProccess.style.fontWeight = "bolder";
			spanProccess.innerHTML = "0%";
			divt.appendChild(spanProccess);
			/*var fileSize = "";
			if(fileList[i].size > 1024*1024*1024){
				fileSize = Math.round(fileList[i].size/(1024*1024*1024))+"GB";
			}else if(fileList[i].size > 1024*1024){
				fileSize = Math.round(fileList[i].size/(1024*1024))+"MB";
			}else{
				fileSize = Math.round(fileList[i].size/1024)+"KB";
			}
			str += "<div style='margin-top: 5px;background-color: bisque;font-size: 18px;font-weight: 500;'>
			<span>文件名称:"+fileList[i].name+"</span>
			<span style='margin-left: 20px;font-size: 18px;font-weight: 500;'>
			文件大小:"+fileSize+"</span><span id='fileProcess"+i+"' style=
			'margin-left: 20px;color: red;font-weight: bolder;'>0%</span></div>";*/
			fileConll.appendChild(divt);			
		}
		//fileConll.innerHTML = str;
  	}
  	/*window.onload = function(){
  		var dropbox = document.getElementById("fileMove");    
	    dropbox.addEventListener("dragenter", dragenter, false);    
	    dropbox.addEventListener("dragover", dragover, false);    
	    dropbox.addEventListener("drop", drop, false);
	    function dragenter(e) {    
	        e.stopPropagation();    
	        e.preventDefault();    
	    }    
	  
	    function dragover(e) {    
	        e.stopPropagation();    
	        e.preventDefault();    
	    }  
	  
	    function drop(e) {    
	        e.stopPropagation();    
	        e.preventDefault();     
	        var dt = e.dataTransfer;    
	        fileListTemps = dt.files;  
	        addFiles(1,fileListTemps);
	    }
  	}*/
  </script>
  <body>
   <h1>系统消息:</h1>
   <span style="color: red;" id="msg"></span><br>
      文件:<input type="file" id="fileName" multiple onchange="addFiles(0);">
    <div id="fileConll" style="margin-top: 5px;margin-bottom: 5px;"></div>
   <div id="progressNumber"></div>
   <input type="button" onclick="openSocket()" value="上传文件">
   <input type="button" onclick="stopUpload()" value="暂停上传"><br><br>
   	返回信息:<span id="returnContent">
   <!-- <h1>文件拖拽区</h1>
   <div id="fileMove" style="width: 500px;height: 100px;border: 1px;background-color: beige;"></div>
   <div id="showFile"></div> -->
  </body>
</html>
