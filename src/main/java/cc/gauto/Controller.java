package cc.gauto;

import cc.gauto.beans.CarDataBean;
import cc.gauto.tcpserver.K528Server;
import cc.gauto.tcpserver.PadServer;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-27 23:26
 */
public class Controller extends Thread {
    private final static Logger logger = Logger.getLogger(cc.gauto.Controller.class.getName());
    private final static double INFINITE = 99999999.9;

    private boolean isRunning;
    private final static int NUM_OF_CARS = 5;

    private ChannelGroup browserClients;

    private PadServer padServer;
    private final static int padServerPort = 2333;
    private ChannelGroup padClients;
    private HashMap<Channel, String> posData = new HashMap<>();
    //TODO: channel-id映射表
    private HashMap<Channel, String> channelId = new HashMap<>();

    public Controller() {
        isRunning = true;
        this.padClients = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
        this.browserClients = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

        padServer = new PadServer(padServerPort, padClients, posData);
    }

    @Override
    public void run() {
        padServer.start();
        while (this.isRunning) {
            if (this.dataQueue.size() >= MAX_SIZE) {
                dataQueue.clear();
            }
            try {
                String data = (String) dataQueue.poll(2, TimeUnit.SECONDS);
                if (data != null) {
                    logger.info("send: " + data + "groupsize + " + this.clientGroup.size());
                    this.clientGroup.writeAndFlush(getSendByteBuf(data));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //计算最近的点
    private void mamimamihong() {
        ArrayList<CarDataBean> carsData = new ArrayList<>();
        //把数据给遍历出来
        ArrayList<Channel> srcChannels = new ArrayList<>();
        for (Map.Entry<Channel, String> entry : this.posData.entrySet()) {
            Channel channel = entry.getKey();
            String data = entry.getValue();
            if (data == null) {
                continue;
            }
            data = data.replaceAll("\n", ""); //去掉换行符
            String[] splitStr = data.split(";");
            if (splitStr.length != 3) {
                logger.error("Device " + channelId.get(channel) + " sent an invalid message");
                continue;
            }
            //位置
            String[] pos = splitStr[0].split(",");
            if (pos.length != 2) {
                logger.error("Device " + channelId.get(channel) + " sent an invalid pos message");
                continue;
            }
            double posX = Double.parseDouble(pos[0]);
            double posY = Double.parseDouble(pos[1]);
            double speed = Double.parseDouble(splitStr[1]);
            double angle = Double.parseDouble(splitStr[2]);
            CarDataBean carDataBean = new CarDataBean(channel, posX, posY, speed, angle);
            carsData.add(carDataBean);
            srcChannels.add(channel);
        }

        //计算每两点间的距离
        DistanceTable distanceTable = new DistanceTable(srcChannels);
        int size = carsData.size();
        for (int i = 0; i < size; ++i) {
            CarDataBean srcCar = carsData.get(i);
            //distanceTable.addRecord(c);
            for (int j = i+1; j < size; ++j) {
                CarDataBean desCar = carsData.get(j);
                double distance = Math.sqrt((srcCar.posX - desCar.posX) * (srcCar.posX - desCar.posX) + (srcCar.posY - desCar.posY) * (srcCar.posY - desCar.posY));
                distanceTable.addRecord(srcCar.channel, desCar.channel, distance);
                distanceTable.addRecord(desCar.channel, srcCar.channel, distance);
            }
        }
        distanceTable.sortByMin();
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
