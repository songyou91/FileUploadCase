<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
	<meta charset="utf-8"/>
	<title>文件上传案例</title>
	<script type="text/javascript">
		function skipFileUploadPage(val){
			var url = '';
			if(val == 0){
				url = 'fileUpload.jsp';
			}else{
				url = 'fileUploadPoint.jsp';
			}
			window.location = url;
		}
	</script>
	<style type="text/css">
		.content {
			width:100%;
			height:100%;
			margin-top: 200px;
   		 	margin-left: 41%;			
		}
		.button {
			width: 145px;
    		height: 33px;
		}
	</style>
</head>
<body>
	<div class="content">
		<input type="button" class="button" value="整数进度上传文件" onclick="skipFileUploadPage(0)">
		<input type="button" class="button" value="小数进度上传文件" onclick="skipFileUploadPage(1)">
	</div>
</body>
</html>
