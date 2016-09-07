package cc.gauto;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by zzl on 16/6/13.
 */
public class MainServer {
    private static final Logger logger = Logger.getLogger(MainServer.class);

    public static void main(String []args) throws Exception {
        //基站和K528的端口一对一
        int hjBasePort = 7071;
        int hjK528Port = 7072;

        ArrayList<Controller> controllers = new ArrayList<>();
//
//        File file = new File("/etc/basestationserver/conf/config.xml");
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        Document doc = builder.parse(file);
//        NodeList nodeList = doc.getElementsByTagName("region");
//        for (int i = 0; i < nodeList.getLength(); ++i) {
//            int basePort = Integer.parseInt(doc.getElementsByTagName("baseport").item(i).getFirstChild().getNodeValue());
//            int k528Port = Integer.parseInt(doc.getElementsByTagName("k528port").item(i).getFirstChild().getNodeValue());
//            Controller controller = new Controller(basePort, k528Port);
//            controllers.add(controller);
//        }

        Controller controller = new Controller();
        controller.start();
        controller.join(3);

    }
}
