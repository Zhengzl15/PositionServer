package cc.gauto;

import cc.gauto.beans.CarDataBean;
import cc.gauto.beans.CarDistanceInfo;
import cc.gauto.tcpserver.PadServer;
import cc.gauto.utils.Constrains;
import cc.gauto.utils.DistanceTable;
import com.sun.deploy.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-27 23:26
 */
public class Controller extends Thread {
    private final static Logger logger = Logger.getLogger(cc.gauto.Controller.class.getName());
    private final static double INFINITE = 99999999.9;
    private final static int PERRIOD = 1000; //300ms向pad端发送一次数据

    private boolean isRunning;
    private final static int NUM_OF_CARS = 5;

    private ChannelGroup browserClients;

    private PadServer padServer;
    private final static int padServerPort = 2333;
    private ChannelGroup padClients;
    private ArrayList<String> schoolConf = new ArrayList<>();
    //private HashMap<Channel, String> posData = new HashMap<>();
    private HashMap<String, HashMap<Channel, CarDataBean> > totalData = new HashMap<String, HashMap<Channel, CarDataBean> >();
    //TODO: channel-id映射表
    private HashMap<Channel, String> channelId = new HashMap<>();

    public Controller() {
        isRunning = true;
        this.padClients = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
        this.browserClients = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

        //Test
        schoolConf.add("GTHD");
        schoolConf.add("GTZZ");
        schoolConf.add("GTCX");
        schoolConf.add("GTZJ");

        for (String school : schoolConf) {
            totalData.put(school, new HashMap<Channel, CarDataBean>());
        }

        padServer = new PadServer(padServerPort, padClients, totalData);
    }

    @Override
    public void run() {
        padServer.start();
        //while (this.isRunning) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (padClients.size() == 0) {
                        logger.info("There is no client connected");
                    } else {
                        channelId.clear();  //清空，以防重连造成内存泄露
                        sendToPad();
                    }
                }
            }, PERRIOD, PERRIOD);
        //}
    }

    public void handleNewData(String msg) {

    }

    //计算最近的点
    private Map<Channel, ArrayList<CarDistanceInfo>> mamimamihong() {
        ArrayList<CarDataBean> carsData = new ArrayList<>();
        //Map<Channel, CarDataBean> channelData = new HashMap<>(); //channel到其信息的映射，这里和上面有重合，不过为了方便就这样吧
        //把数据给遍历出来
        //每个驾校里面的车的距离信息等
        Map<Channel, ArrayList<CarDistanceInfo> >  ret = new HashMap<Channel, ArrayList<CarDistanceInfo> >();
        for (Map.Entry<String, HashMap<Channel, CarDataBean>> entry : totalData.entrySet()) {
            carsData.clear();
            String key = entry.getKey();
            HashMap<Channel, CarDataBean> value = entry.getValue();
            ArrayList<Channel> srcChannels = new ArrayList<>();
            logger.info("number of " + key + "\'s clients is " + value.size());
            for (Map.Entry<Channel, CarDataBean> entry1 : value.entrySet()) {
                carsData.add(entry1.getValue());
                srcChannels.add(entry1.getKey());
                channelId.put(entry1.getKey(), entry1.getValue().deviceId);

            }

            //计算每两点间的距离
            DistanceTable distanceTable = new DistanceTable(srcChannels);
            int size = carsData.size();
            for (int i = 0; i < size; ++i) {
                CarDataBean srcCar = carsData.get(i);
                //distanceTable.addRecord(c);
                for (int j = i + 1; j < size; ++j) {
                    CarDataBean desCar = carsData.get(j);
                    double distance = Math.sqrt((srcCar.posX - desCar.posX) * (srcCar.posX - desCar.posX) + (srcCar.posY - desCar.posY) * (srcCar.posY - desCar.posY));
                    distanceTable.addRecord(srcCar.channel, desCar.channel, distance);
                    distanceTable.addRecord(desCar.channel, srcCar.channel, distance);
                }
            }
            distanceTable.sortByMin();
            //返回距离
            Map<Channel, ArrayList<CarDistanceInfo>> tmp = distanceTable.getTable();
            ret.putAll(tmp);
        }
        return ret;
    }

    //向pad发送最近m个点的信息，根据距离
    private void sendToPad() {
        Map<Channel, ArrayList<CarDistanceInfo>> sortedDisstanceTable = mamimamihong();
        for (Map.Entry<Channel, ArrayList<CarDistanceInfo>> entry : sortedDisstanceTable.entrySet()) {
            //在这里规定发送到pad端的数据格式
            //deviceId1:x,y,speed,angle;deviceid2:distance,speed,angle...
            Channel src = entry.getKey();
            if (!channelId.containsKey(src)) {
                continue;
            }
            String srcDeviceId = channelId.get(src);
            String schoolKey = srcDeviceId.substring(0, Constrains.SUB_NUM);
            ArrayList<CarDistanceInfo> infos = entry.getValue();
            int _size = infos.size();
            if (_size >= NUM_OF_CARS) {  //当前连接的车已经有足够多，即超过返回的数值
                _size = NUM_OF_CARS;
            }
            String sendMsg = "";
            for (int i = 0; i < _size; ++i) {
                CarDistanceInfo info = infos.get(i);
                Channel des = info.channel;
                if (!channelId.containsKey(des)) {
                    continue;
                }
                //String deviceId = channelId.get(des);
                //double distance = info.distance;
                CarDataBean carDataBean = totalData.get(schoolKey).get(des);
                //数据格式为:deviceid,examid,x,y,speed,angle;\n

                double posX = carDataBean.posX;
                double posY = carDataBean.posY;
                double speed = carDataBean.speed;
                double angle = carDataBean.angle;
                DecimalFormat decimalFormat=new DecimalFormat(".000");

                String posInfo = carDataBean.deviceId + "," + carDataBean.examId + "," + decimalFormat.format(posX) + ","
                        + decimalFormat.format(posY) + "," + decimalFormat.format(speed) + "," + decimalFormat.format(angle);

                sendMsg += posInfo+";";
            }
            sendMsg += "\n";
            try {
                //sendMsg = "hello\n";
                byte[] bytes = sendMsg.getBytes("UTF-8");
//                src.writeAndFlush(getSendByteBuf(sendMsg));
                src.writeAndFlush(bytes);
                logger.info("Send to " + channelId.get(src) + " content " + sendMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void terminate() throws InterruptedException {
        this.isRunning = false;
        padServer.join(3);
    }

    private ByteBuf getSendByteBuf(String message)
            throws UnsupportedEncodingException {

        byte[] req = message.getBytes("UTF-8");
        ByteBuf pingMessage = Unpooled.buffer();
        pingMessage.writeBytes(req);

        return pingMessage;
    }

}
