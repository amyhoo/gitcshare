package example;
import java.io.*; 
import java.net.URL; 
import java.net.URLConnection; 
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;  
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class UrlTest { 
        public static void main(String[] args) throws IOException {         	
            URL url;  
            setProxy();
            test3();
//            try {  
//                 url = new URL("https://www.baidu.com");  
//                 HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
//                 InputStreamReader insr = new InputStreamReader(httpsConn.getInputStream());
//                 int respInt = insr.read();
//                 while (respInt != -1) {
//                 System.out.print((char) respInt);
//                 respInt = insr.read();
//                 }
//                 System.out.println("连接�?�用");  
//            } catch (Exception e1) {  
//                 System.out.println("连接打�?开!");  
//                 url = null;  
//            } 
//            System.exit(0);
        } 
        static class MyAuthenticator extends Authenticator {
            private String user = "";
            private String password = "";
      
            public MyAuthenticator(String user, String password) {
                this.user = user;
                this.password = password;
            }
      
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password.toCharArray());
            }
        }
             
        private static void setProxy(){
//        	InetSocketAddress addr = new InetSocketAddress("isaproxy.int.corp.sun", 89);     
//        	Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
//        	Authenticator.setDefault(new MyAuthenticator("U391812", "Xumin&06"));
        	System.setProperty("http.proxySet", "true"); 
        	System.setProperty("http.proxyHost", "isaproxy.int.corp.sun"); 
        	System.setProperty("http.proxyPort", "89");
        	System.setProperty("http.proxyUser", ""); 
        	System.setProperty("http.proxyPassword", "");        	
        }
        public static String getHTML(String key) throws IOException  
        {  
            StringBuilder sb=new StringBuilder();  
            String path="http://www.baidu.com/s?tn=ichuner&lm=-1&word="+URLEncoder.encode(key,"gb2312")+"&rn=100";  
            URL url=new URL(path);  
            //conn = (HttpURLConnection) url.openConnection();
            InputStream is=url.openStream();
            BufferedReader breader=new BufferedReader(new InputStreamReader(is));  
            String line=null;  
            while((line=breader.readLine())!=null)  
            {  
                sb.append(line);  
            }  
            return sb.toString();  
        }  
        /** 
         * 获�?�URL指定的资�?。 
         * 
         * @throws IOException 
         */ 
        public static void test4() throws IOException { 
                URL url = new URL("http://lavasoft.blog.51cto.com/attachment/200811/200811271227767778082.jpg"); 
                //获得此 URL 的内容。 
                Object obj = url.getContent(); 
                System.out.println(obj.getClass().getName()); 
        } 

        /** 
         * 获�?�URL指定的资�? 
         * 
         * @throws IOException 
         */ 
        public static void test3() throws IOException { 
                
                try {
                	StringBuffer html = new StringBuffer(); 
                	URL url = new URL("http://www.baidu.com"); //根�?� String 表示形�?创建 URL 对象。        
                    InputStream is=url.openStream();
                    BufferedReader breader=new BufferedReader(new InputStreamReader(is));                 	

                	String temp;
                	while ((temp = breader.readLine()) != null) { //按行读�?�输出�?
                	if(!temp.trim().equals("")){
                	html.append(temp).append("\n"); //读完�?行�?��?�行
                	}
                	}
                	breader.close(); //关闭
                	is.close(); //关闭
                	 //返回此�?列中数�?�的字符串表示形�?。
                	} catch (Exception e) {
                	e.printStackTrace();
                	
                	}
        } 

        /** 
         * 读�?�URL指定的网页内容 
         * 
         * @throws IOException 
         */ 
        public static void test2() throws IOException { 
                URL url = new URL("http://www.hrtsea.com/down/soft/45.htm"); 
                //打开到此 URL 的连接并返回一个用于从该连接读入的 InputStream。 
                Reader reader = new InputStreamReader(new BufferedInputStream(url.openStream())); 
                int c; 
                while ((c = reader.read()) != -1) { 
                        System.out.print((char) c); 
                } 
                reader.close(); 
        } 

        /** 
         * 获�?�URL的输入�?，并输出 
         * 
         * @throws IOException 
         */ 
        public static void test() throws IOException { 
                URL url = new URL("http://lavasoft.blog.51cto.com/62575/120430"); 
                //打开到此 URL 的连接并返回一个用于从该连接读入的 InputStream。 
                InputStream in = url.openStream(); 
                int c; 
                while ((c = in.read()) != -1) 
                        System.out.print(c); 
                in.close(); 
        } 
}