package test.SpringMVC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;

import net.sf.json.JSONArray;
import sun.awt.RepaintArea;
import test.SpringMVC.model.FileInfo;
import test.SpringMVC.model.Project;

@Controller
@RequestMapping("/mvc")
public class mvcController {
	@RequestMapping("/hello")
    public String hello(){
        return "success";
    }
	
	@RequestMapping("/distinguish")
    public String distinguish(){        
        return "distinguish";
    }
    
    /**
     * 跳转到标记页面
     * @return
     */
    @RequestMapping("/getMark")
    public String getMark(){
    	return "mark";
    }
    /**
     * 测试用，以后删除
     * @param req
     * @param response
     */
    @RequestMapping("/getText_1")
    public static void getText_1(HttpServletRequest req,HttpServletResponse response){
    	PrintWriter out;
		Process p;
		try {
			p = Runtime.getRuntime().exec("crf_test -m /home/cz/model /home/cz/learn.txt");
			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			StringBuilder sb = new StringBuilder();
			out = response.getWriter();
			while ((line = br.readLine()) != null) {
				if(line.length() < 1){
					System.out.println("\n");
					sb.append("\n");
					continue;
				}
				System.out.println(line);
				out.write(line+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    
    
    @RequestMapping("/getWelcome")
    public String getWelcome(){
    	return "welcome";
    }
    
    /**
     * 跳转到项目管理模块
     * @return
     */
    @RequestMapping("/getManageMark")
    public String getManageMark(){
    	return "manageMark";
    }
    
    /**
     * 跳转到识别模块
     * @return
     */
    @RequestMapping("/getIdentification")
    public String getIdentification(){
		return "identification";
    }
    /**
     * 跳转到应用模块
     * @return
     */
    @RequestMapping("/getApplication")
    public String getApplication(){
    	return "application";
    }
    
    /**
     * 判断是否是Linux操作系统
     * @return
     */
    private boolean isOSLinux() {
        Properties prop = System.getProperties();

        String os = prop.getProperty("os.name");
        if (os != null && os.toLowerCase().indexOf("linux") > -1) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * 删除选中的文件
     * @param request
     * @param response
     */
    @RequestMapping("/deleteFile")
    public void deleteFile(HttpServletRequest request,HttpServletResponse response){
    	String userId= request.getParameter("userId");
    	String fileName= request.getParameter("fileName");
    	PreparedStatement deleteStatment = null;
    	PreparedStatement queryStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
    	String url = null;
    	try {
    		queryStatment = conn.prepareStatement("select url from file where user_id = ? and file_name= ? ");
			queryStatment.setString(1,userId);
			queryStatment.setString(2, fileName);
			rs = queryStatment.executeQuery();
			while(rs.next()){
	    		url = rs.getString("url");
	    	}
			String filePath = url.substring(0, url.lastIndexOf('/'));
			System.out.println(filePath);
			//linux系统使用如下命令,成功运行
			String cmdLinux[]= {"rm "+filePath+"/"+fileName };
			//windows系统使用如下命令,成功运行
			String cmdWin[] ={ "cmd","/C","del "+filePath+"\\"+ fileName};
			String cmd[] = isOSLinux() ? cmdLinux : cmdWin;
			System.out.println(cmd.length);
			Runtime.getRuntime().exec(cmdWin);
    		deleteStatment = conn.prepareStatement("delete from file where user_id = ? and file_name = ?");
    		deleteStatment.setString(1,userId);
    		deleteStatment.setString(2, fileName);
	    	int result = deleteStatment.executeUpdate();
	    	conn.commit();
	    	if(result > 0){
	    		System.out.println("操作成功"+result);
	    	}else {
	    		System.out.println("操作失败");
	    	}
	    	deleteStatment.close();
	    	conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
    }
    
    /**
     * 将用户输入的文本识别之后返回
     * @param request
     * @param response
     */
    @RequestMapping("/identity")
    public void identity(HttpServletRequest request,HttpServletResponse response){
    	String modelPath = request.getServletContext().getRealPath("/resources/model/");
    	String sourceData = request.getParameter("text");
    	String modelName = request.getParameter("modelName");
    	String targetPath =modelPath + "/"+"temp.data";
    	response.setContentType("text/html;charset=utf-8");
    	File file =new File(targetPath); 
    	PrintWriter pw;
    	try {
			OutputStream out  = new FileOutputStream(file)  ;
			for(int i=0;i<sourceData.length();i++){
				String tmp=String.valueOf(sourceData.charAt(i));
				if(!tmp.equals("\n"))
					tmp+="\n";
				byte [] b= tmp.getBytes();
				out.write(b);
			}
			out.close();
			//linux系统使用如下命令,成功运行
			String cmdLinux= "crf_test -m "+modelPath+"/"+modelName+" "+modelPath+"/temp.data";
			//windows系统使用如下命令,成功运行
			String cmdWin = modelPath+"/crf_test -m "+ modelPath+"/"+modelName+" "+modelPath+"/temp.data";
			String cmd = isOSLinux() ? cmdLinux : cmdWin;
			System.out.println(cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader br  = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			pw = response.getWriter();
			while ((line = br.readLine()) != null) {
				if(line.length() < 1){
					pw.write("\n");
					continue ;
				}
				if(line.charAt(2)=='B')
					pw.write("#");
				pw.write(line.charAt(0));
				if(line.charAt(2)=='E')
					pw.write("#");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 测试生成模型的效果
     * @param request
     * @param response
     */
    @RequestMapping("/test")
    public void test(HttpServletRequest request,HttpServletResponse response){
    	String realPath = request.getServletContext().getRealPath("/resources/");
    	response.setContentType("text/html;charset=utf-8");
    	PrintWriter out;
    	//String cmdString="crf_test -m /home/cz/model /home/cz/learn.txt";
    	String cmd = "ls -l " + realPath+"/";
    	System.out.println(cmd);
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd);
			BufferedReader br  = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			StringBuilder sb = new StringBuilder();
			out = response.getWriter();
			while ((line = br.readLine()) != null) {
				if(line.length() < 1){
					System.out.println("\n");
					sb.append("\n");
					continue ;
				}
				System.out.println(line);
				out.write(line+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    /**
     * 将选择的格式化文件训练生成crf模型
     * @param request
     * @param response
     */
    @RequestMapping("/train")
    public void train(HttpServletRequest request,HttpServletResponse response){
    	String templatePath = request.getServletContext().getRealPath("/resources/formated/")+"/template";
    	String formatedFilePath=request.getParameter("fileUrl");
    	String modelPath = request.getServletContext().getRealPath("/resources/model/")+"/";
    	String modelName = request.getParameter("fileName")+".model";
    	response.setContentType("text/html;charset=utf-8");
    	PrintWriter out;
    	String cmdString="crf_learn -f 2 -c 1.5 -p 14 "+templatePath+" "+formatedFilePath+" "+modelPath+modelName ;
    	System.out.println(cmdString);
		String testCmd="crf_learn -f 2 -c 1.5 -p 14 /home/cz/template_baseline /home/cz/learn.txt /home/cz/txt.model";
    	Process p;
		try {
			p = Runtime.getRuntime().exec(testCmd);
			BufferedReader br  = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			StringBuilder sb = new StringBuilder();
			out = response.getWriter();
			while ((line = br.readLine()) != null) {
				if(line.length() < 1){
					System.out.println("\n");
					sb.append("\n");
					continue ;
				}
				System.out.println(line);
				out.write(line+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    /**
     * 查询所有已存在模型
     * @param req
     * @param rep
     */
    @RequestMapping("/queryAllModel")
    public void queryAllModel(HttpServletRequest req,HttpServletResponse rep){
    	rep.setContentType("text/html;charset=utf-8");//设置响应的编码格式，不然会出现中文乱码现象  
    	PreparedStatement queryStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
    	try {
			queryStatment = conn.prepareStatement("select file_name,url from file where user_id = ? and file_type= ? ORDER BY update_time DESC ");
			queryStatment.setString(1,req.getParameter("user_id"));
			queryStatment.setString(2, "D");
	    	rs = queryStatment.executeQuery();
	    	PrintWriter out = rep.getWriter();
	    	List<FileInfo> list = new ArrayList<FileInfo>();
	    	FileInfo file = null;
	    	while(rs.next()){
	    		String fileName = rs.getString("file_name");
	    		String url = rs.getString("url");
	    		file = new FileInfo();
	    		file.setFileName(fileName);
	    		file.setUrl(url);
	    		list.add(file);
	    	}
	    	JSONArray json = JSONArray.fromObject(list);
	    	out.write(json.toString());  
	        out.flush();  
	        out.close(); 
	    	rs.close();
	    	queryStatment.close();
	    	conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
    }
    /**
     * 查询所有格式化为文件
     * @param req
     * @param rep
     */
    @RequestMapping("/queryAllFormatedFile")
    public void queryAllFormatedFile(HttpServletRequest req,HttpServletResponse rep){
    	rep.setContentType("text/html;charset=utf-8");//设置响应的编码格式，不然会出现中文乱码现象  
    	PreparedStatement queryStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
    	try {
			queryStatment = conn.prepareStatement("select file_name,url from file where user_id = ? and file_type= ? ORDER BY update_time DESC ");
			queryStatment.setString(1,req.getParameter("user_id"));
			queryStatment.setString(2, "F");
	    	rs = queryStatment.executeQuery();
	    	PrintWriter out = rep.getWriter();
	    	List<FileInfo> list = new ArrayList<FileInfo>();
	    	FileInfo file = null;
	    	while(rs.next()){
	    		String fileName = rs.getString("file_name");
	    		String url = rs.getString("url");
	    		file = new FileInfo();
	    		file.setFileName(fileName);
	    		file.setUrl(url);
	    		list.add(file);
	    	}
	    	JSONArray json = JSONArray.fromObject(list);
	    	out.write(json.toString());  
	        out.flush();  
	        out.close(); 
	    	rs.close();
	    	queryStatment.close();
	    	conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
    }
    
    /**
     * 将标记后的文本内容转为crf训练需要的格式
     * @param filePah
     * @param targetPath
     * @return
     */
    private String filter(String filePah, String targetPath){
    	Map<String,String> map = new HashMap<String,String>();
		map.put("主谓关系", "SBV");
		map.put("动宾关系", "VOB");
		map.put("间宾关系", "IOB");
		map.put("前置宾语", "FOB");
		map.put("兼语", "DBL");
		map.put("定中关系", "ATT");
		map.put("状中结构", "ADV");
		map.put("动补结构", "CMP");
		map.put("并列关系", "COO");
		map.put("介宾关系", "POB");
		map.put("左附加关系", "LAD");
		map.put("右附加关系", "RAD");
		map.put("独立结构", "IS");
		map.put("核心关系", "HED");
		map.put("标点符号","BDF");
    	String result = null;
    	try {
            String encoding="UTF-8";
            File file=new File(filePah);
            if(file.isFile() && file.exists()){ //判断文件是否存在
                InputStreamReader read = new InputStreamReader(
                new FileInputStream(file),encoding);//考虑到编码格式
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                boolean startTag = true;
                boolean hasQueue = false;
                boolean hasMarked = false;
                Queue<String> queue = new LinkedList<String>();
                FileWriter fw  = new FileWriter(targetPath,false);
                while((lineTxt = bufferedReader.readLine()) != null){
                	startTag = true;
                	hasQueue = false;
                	int j = 0;
                	hasMarked  = false;
                	while(j<lineTxt.length()){
                		if(lineTxt.charAt(j++)=='#'){
                			hasMarked = true;
                			break;
                		}
                	}
                	if(!hasMarked){
                		continue;
                	}
//                	int i=0;
//                	while(i<lineTxt.length()){
//                		if('#'== lineTxt.charAt(i) && startTag){
//                			i++;
//                			startTag = false;
//                		}else if('#' != lineTxt.charAt(i) && (!startTag)){
//                			queue.offer(lineTxt.charAt(i));
//                			hasQueue = true;
//                			i++;
//                		}else if('#' != lineTxt.charAt(i)&& startTag){
//                			fw.write(lineTxt.charAt(i)+"\t"+"O\n");
//                			i++;
//                		}else if('#' == lineTxt.charAt(i)&& (!startTag)){
//                			startTag = true;
//                			if(hasQueue){//一定不为空
//                				fw.write(queue.poll()+"\t"+"B\n");
//                				while(!queue.isEmpty()){
//                					if(1 != queue.size()){
//                						fw.write(queue.poll()+"\t"+"M\n");
//                					}else{
//                						fw.write(queue.poll()+"\t"+"E\n");
//                					}
//                				}
//                			}
//                			hasQueue = false;
//                			i++;
//                		}
//                	}
                	CoNLLSentence sentence = HanLP.parseDependency(lineTxt);
                	for(CoNLLWord word : sentence){
                		if('#' == word.LEMMA.charAt(0)&&startTag){
                			startTag = false;
                		}else if('#'!= word.LEMMA.charAt(0)&&(!startTag)){
                			for(int p=0;p<word.LEMMA.length();p++){
                				queue.offer(word.LEMMA.charAt(p)+"\t"+word.POSTAG+"\t"+map.get(word.DEPREL));
                			}
                			hasQueue = true;
                		}else if('#'!= word.LEMMA.charAt(0)&& startTag ){
                			for(int p=0;p<word.LEMMA.length();p++){
                				fw.write(word.LEMMA.charAt(p)+"\t"+word.POSTAG+"\t"+map.get(word.DEPREL)+"\t"+"O\n");
                			}
                		}else if('#' == word.LEMMA.charAt(0)&& !startTag ){
                			startTag = true;
                			if(hasQueue){//一定不为空
                				fw.write(queue.poll()+"\t"+"B\n");
                				while(!queue.isEmpty()){
                					if(1 != queue.size()){
                						fw.write(queue.poll()+"\t"+"M\n");
                					}else{
                						fw.write(queue.poll()+"\t"+"E\n");
                					}
                				}
                			}
                			hasQueue = false;
                		}
                	}
                	fw.write("\n");
                }
                read.close();
                fw.close();
                result="格式化成功";
        }else{
            System.out.println("找不到指定的文件");
            result = "找不到指定文件";
        }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
            e.printStackTrace();
            result = "读取文件内容出错";
        }
    	return result;
    }
    
    /**
     * 过滤无标记的句子，将内容格式化为crf训练需要的格式
     * @param request
     * @param response
     */
    @RequestMapping("/formatText")
    public void formatText(HttpServletRequest request,HttpServletResponse response){
    	response.setContentType("text/html;charset=utf-8");
    	String filePath = request.getParameter("fileUrl");
    	String fileName = request.getParameter("fileName");
    	String realPath = request.getServletContext().getRealPath("/resources/formated/");
    	String targetPath =realPath + "/"+"formated_"+fileName;
    	System.out.println(filePath);
    	String result= filter(filePath,targetPath);
    	PreparedStatement insertStatement = null;
    	Connection conn = connectMysql();
    	PrintWriter out;
    	try{
    		 out = response.getWriter();
        	if(result.equals("格式化成功")){
        		
    	    	//保存数据库
    	    	insertStatement  = conn.prepareStatement("insert into " + "file" +   
    					"(user_id,url,file_name,update_time,file_type) values (?,?,?,?,?)");
    			insertStatement.setString(1, "45");
            	insertStatement.setString(2, targetPath);
            	insertStatement.setString(3, "formated_"+request.getParameter("fileName"));
            	Date time = new Date();
                SimpleDateFormat timesf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                String updatetime = timesf.format(time);
                insertStatement.setString(4,updatetime);
            	insertStatement.setString(5, "F");//格式化的文件
                insertStatement.executeUpdate();  
                conn.commit();
                insertStatement.close();
                conn.close();
        	}
        	out.write(result);
        	out.close() ;
    	}catch (Exception e){
    		e.printStackTrace();
    	}
    }
    
    /**
     * 查询目标文件
     * @param userId
     * @param fileType
     * @return
     */
    private JSONArray queryTargetFile(String userId,String fileType){
    	PreparedStatement queryStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
    	JSONArray json=null;
    	try {
			queryStatment = conn.prepareStatement("select file_name,url from file where user_id = ? and file_type= ? ORDER BY update_time DESC ");
			queryStatment.setString(1,userId);
			queryStatment.setString(2, fileType);
	    	rs = queryStatment.executeQuery();
	    	List<FileInfo> list = new ArrayList<FileInfo>();
	    	FileInfo file = null;
	    	while(rs.next()){
	    		String fileName = rs.getString("file_name");
	    		String url = rs.getString("url");
	    		file = new FileInfo();
	    		file.setFileName(fileName);
	    		file.setUrl(url);
	    		list.add(file);
	    	}
	    	json = JSONArray.fromObject(list);
	    	rs.close();
	    	queryStatment.close();
	    	conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
		return json;
    }
    /**
     * 返回所有标注好的文件
     * @param req
     * @param rep
     */
    @RequestMapping("/queryAllMarkedFile")
    public void queryAllMarkedFile(HttpServletRequest req,HttpServletResponse rep){
    	rep.setContentType("text/html;charset=utf-8");//设置响应的编码格式，不然会出现中文乱码现象  
    	PrintWriter out;
		try {
			out = rep.getWriter();
			JSONArray json = queryTargetFile("45","M");
	    	out.write(json.toString());  
	        out.flush();  
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    @RequestMapping("/queryAllOrgFile")
    public void queryAllOrgFile(HttpServletRequest request,HttpServletResponse response){
    	response.setContentType("text/html;charset=utf-8");
    	String userId= request.getParameter("user_id");
    	String fileType = request.getParameter("file_type");
    	PrintWriter out;
		try {
			out = response.getWriter();
			JSONArray json = queryTargetFile(userId,fileType);
	    	out.write(json.toString());  
	        out.flush();  
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    /**
     * 将标注好的数据保存到服务器
     * @param request
     * @param response
     * @return
     * @throws FileNotFoundException
     */
    @RequestMapping("/saveServer")
    public void saveServer(HttpServletRequest request,HttpServletResponse response){
    	String realPath = request.getServletContext().getRealPath("/resources/marked/");
    	String targetPath =realPath + "/"+"marked_"+request.getParameter("fileName");
    	File file =new File(targetPath);  
    	FileWriter fw = null;
    	PreparedStatement insertStatement = null;
    	Connection conn = connectMysql();
    	try {
    		fw  = new FileWriter(file,false);
			String str = request.getParameter("data");  
	    	fw.write(str);
	    	//保存数据库
	    	insertStatement  = conn.prepareStatement("insert into " + "file" +   
					"(user_id,url,file_name,update_time,file_type) values (?,?,?,?,?)");
			insertStatement.setString(1, "45");
        	insertStatement.setString(2, targetPath);
        	insertStatement.setString(3, "marked_"+request.getParameter("fileName"));
        	Date time = new Date();
            SimpleDateFormat timesf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            String updatetime = timesf.format(time);
            insertStatement.setString(4,updatetime);
        	insertStatement.setString(5, "M");
            insertStatement.executeUpdate();  
            conn.commit();
            insertStatement.close();
            conn.close();
	    	
	    	fw.close() ;      // 关闭输出流
	    	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e){
			e.printStackTrace();
		}
    }
    
    /**
     * 获取上传文件的内容
     * @param request
     * @param response
     */
    @RequestMapping("/getText")
    public void getText(HttpServletRequest request,HttpServletResponse response){
    	 FileInputStream fis = null;  
         OutputStream os = null; 
         File file = null;
         response.setContentType("text/html;charset=utf-8");
         try {  
        	 file = new File(request.getParameter("file_url"));
             fis = new FileInputStream(file);  
             os = response.getOutputStream();  
             int count = 0;  
             byte[] buffer = new byte[1024 * 8];  
             while ((count = fis.read(buffer)) != -1) {  
                 os.write(buffer, 0, count);  
                 os.flush();  
             }  
         } catch (Exception e) {  
             e.printStackTrace();  
         }  
         try {  
             fis.close();  
             os.close();  
         } catch (IOException e) {  
             e.printStackTrace();  
         }  
    }
    /**
     * 上传文件
     * @param file
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping(value="/fileUpload", method = RequestMethod.POST)
    public void fileUpLoad(@RequestParam("file") MultipartFile file,HttpServletRequest request, HttpServletResponse response) throws IOException{ 
    	String fileType = new String( request.getParameter("file_type").getBytes("ISO-8859-1"),"utf-8");
    	String realPath = null;
    	String fileFlag = "";
    	if(fileType.equals("待标记文件")){
    		realPath = request.getServletContext().getRealPath("/resources/needMark/");
    		fileFlag = "O";
    	}else if(fileType.equals("训练模板")){
    		realPath = request.getServletContext().getRealPath("/resources/formated/");
    		fileFlag = "T";
    	}else if(fileType.equals("可用模型")){
    		realPath = request.getServletContext().getRealPath("/resources/model/");
    		fileFlag = "D";
    	}else{
    		realPath = request.getServletContext().getRealPath("/resources/formated/");
    		fileFlag = "E";
    	}
    	System.out.println(realPath);
    	String fileName = new String(file.getOriginalFilename().getBytes("ISO-8859-1"),"utf-8");
    	File filePath =new File(realPath);   
    	
    	//如果文件夹不存在则创建    
    	if  (!filePath.exists()  && !filePath.isDirectory())      
    	{ 
    		System.out.println("文件夹不存在");
    	    filePath .mkdir();    
    	} 
    	if(!file.isEmpty()){
    		FileUtils.copyInputStreamToFile(file.getInputStream(), new File(realPath+"/",fileName));
    		insertFileInfo(realPath,fileFlag,fileName);
		}
    }
    /**
     * 将插入文件的信息插入数据库，如果存在， 则更新一下时间
     * @param filePath
     * @param fileFlag
     * @param fileName
     */
    private void insertFileInfo(String filePath,String fileFlag,String fileName){
    	PreparedStatement insertStatement = null;
    	PreparedStatement queryStatment = null;
    	PreparedStatement updateStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
		try {
			queryStatment = conn.prepareStatement("select count(*) record from file where file_name = ? and file_type = ?");
			queryStatment.setString(1,fileName);
			queryStatment.setString(2,fileFlag);
			rs = queryStatment.executeQuery();
			int row = 0;
			while(rs.next()){
	    		row = rs.getInt("record");
	    	}
			Date time = new Date();
            SimpleDateFormat timesf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            String updatetime = timesf.format(time);
			if( row >0){
				updateStatment = conn.prepareStatement("update file set update_time = ? where file_type = ? and file_name = ?");
				updateStatment.setString(1, updatetime);
				updateStatment.setString(2, fileFlag);
				updateStatment.setString(3, fileName);
				updateStatment.executeUpdate();
				conn.commit();
				updateStatment.close();
	            conn.close();
			}else {
				insertStatement  = conn.prepareStatement("insert into " + "file" +   
						"(user_id,url,file_name,update_time,file_type) values (?,?,?,?,?)");
				insertStatement.setString(1, "45");
	        	insertStatement.setString(2, filePath+"/"+fileName);
	        	insertStatement.setString(3, fileName);
                insertStatement.setString(4, updatetime);
                insertStatement.setString(5, fileFlag);
	        	insertStatement.executeUpdate();  
	            conn.commit();
	            insertStatement.close();
	            conn.close();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    /**
     * 查询所有已存在未标记文件文件
     * @param req
     * @param rep
     */
    @RequestMapping("/queryAllFile")
    public void queryAllFile(HttpServletRequest req,HttpServletResponse rep){
    	rep.setContentType("text/html;charset=utf-8");//设置响应的编码格式，不然会出现中文乱码现象  
    	PreparedStatement queryStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
    	try {
			queryStatment = conn.prepareStatement("select file_name,url from file where user_id = ? ORDER BY update_time DESC ");
			queryStatment.setString(1,req.getParameter("user_id"));
	    	rs = queryStatment.executeQuery();
	    	PrintWriter out = rep.getWriter();
	    	List<FileInfo> list = new ArrayList<FileInfo>();
	    	FileInfo file = null;
	    	while(rs.next()){
	    		String fileName = rs.getString("file_name");
	    		String url = rs.getString("url");
	    		file = new FileInfo();
	    		file.setFileName(fileName);
	    		file.setUrl(url);
	    		list.add(file);
	    	}
	    	JSONArray json = JSONArray.fromObject(list);
	    	out.write(json.toString());  
	        out.flush();  
	        out.close(); 
	    	rs.close();
	    	queryStatment.close();
	    	conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
    }
    /**
     * 查询返回所有已存在项目
     * @param req
     * @param pw
     */
    @RequestMapping("/queryAllProject")
    public void queryAllProject(HttpServletRequest req,HttpServletResponse rep){
    	rep.setContentType("text/html;charset=utf-8");//设置响应的编码格式，不然会出现中文乱码现象  
    	PreparedStatement queryStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
    	try {
			queryStatment = conn.prepareStatement("select pro_name from project where user_id = ? ORDER BY update_time DESC ");
			queryStatment.setString(1,req.getParameter("user_id"));
	    	rs = queryStatment.executeQuery();
	    	PrintWriter out = rep.getWriter();
	    	List<Project> list = new ArrayList<Project>();
	    	Project pro = null;
	    	while(rs.next()){
	    		String proName = rs.getString("pro_name");
	    		pro = new Project();
	    		pro.setProName(proName);
	    		list.add(pro);
	    	}
	    	JSONArray json = JSONArray.fromObject(list);
	    	out.write(json.toString());  
	        out.flush();  
	        out.close(); 
	    	rs.close();
	    	queryStatment.close();
	    	conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
    	
    }
    /**
     * 创建项目，遇到重复时会反馈
     * @param req
     * @param pw
     */
    @RequestMapping("/createProject")
    public void createProject(HttpServletRequest req,PrintWriter pw){
    	PreparedStatement insertStatement = null;
    	PreparedStatement queryStatment = null;
    	Connection conn = connectMysql();
    	ResultSet rs =null;
    	boolean isInserted;
    	//创建一个Statement对象  
        try {
        	queryStatment = conn.prepareStatement("select count(*) record from project where pro_name = ?");
        	queryStatment.setString(1,req.getParameter("project_name"));
        	rs = queryStatment.executeQuery();
        	isInserted = false;
        	while(rs.next()){
        		int rowNum = rs.getInt("record");
        		if(rowNum < 1){
        			isInserted = true;
        			insertStatement = conn.prepareStatement("insert into " + "project" +   
        					" (user_id,pro_name,pro_sign,update_time) values (?,?,?,?)");
		        	insertStatement.setString(1, "45");
		        	insertStatement.setString(2, req.getParameter("project_name"));
		        	insertStatement.setString(3, req.getParameter("mark_sign"));
		        	Date time = new Date();
                    SimpleDateFormat timesf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    String updatetime = timesf.format(time);
                    insertStatement.setString(4,updatetime);
		        	insertStatement.executeUpdate();  
		            conn.commit();
		            pw.write("1");
        		}else{
        			pw.write("0");
        		}
        	}
        	//关闭连接，与之前的操作反着来
        	rs.close();
        	if(isInserted){
        		insertStatement.close();
        	}else{
        		queryStatment.close();
        	}
        	conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
    }
    
    /**
     * 连接数据库
     * @return 数据库
     */
    private Connection connectMysql(){
    	try {  
            //调用Class.forName()方法加载驱动程序  
            Class.forName("com.mysql.jdbc.Driver");  
        } catch (ClassNotFoundException e) {  
            e.printStackTrace();  
        } 
    	String hostname = "115.159.40.100";
    	String port = "3306";
    	String databaseName = "mark_system";
    	String url = "jdbc:mysql://" + hostname + ":" + port + "/" + databaseName;
    	String user = "root";
    	String password = "root";
    	Connection conn = null; 
    	try {  
            conn = DriverManager.getConnection(url, user, password);  
            conn.setAutoCommit(false); 

        } catch (SQLException e) {  
            e.printStackTrace();  
            System.exit(1);  
        }  
        return conn;
    }
}
