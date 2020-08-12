package framework;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;

public class RPCHttpServer {

    public void start(String hostName, Integer port){
        Tomcat tomcat = new Tomcat();

        Server server = tomcat.getServer();
        Service service = server.findService("Tomcat");

        Connector connector = new Connector();
        connector.setPort(port);

        StandardEngine engine = new StandardEngine();
        engine.setDefaultHost(hostName);

        StandardHost host = new StandardHost();
        host.setName(hostName);

        String contextPath = "";
        StandardContext context = new StandardContext();
        context.setPath(contextPath);
        context.addLifecycleListener(new Tomcat.FixContextListener());

        host.addChild(context);
        engine.addChild(host);

        service.setContainer(engine);
        service.addConnector(connector);

        tomcat.addServlet(contextPath, "dispatcher", new DispatcherServlet());
        context.addServletMappingDecoded("/*", "dispatcher");

        try {
            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }
    class DispatcherServlet extends HttpServlet {
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) {
            new HttpServerHandler().handler(req, resp);
        }
    }

    class HttpServerHandler {
        public void handler(HttpServletRequest req, HttpServletResponse resp){
            //处理请求,反射，如果是spring应该是直接在ioc获取实体类
            try {
                InputStream inputStream = req.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                Invocation invocation = (Invocation) objectInputStream.readObject();
//                Class aClass = LocalRegister.get(invocation.getInterfaceName());
                Class aClass = null;
                Method method = aClass.getMethod(invocation.getMethodName(), invocation.getParamTypes());
                String invoke = (String) method.invoke(aClass.newInstance(), invocation.getParams());
                IOUtils.write(invoke, resp.getOutputStream());

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
