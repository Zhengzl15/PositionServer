package cc.gauto.tcpserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-14 19:39
 */
public class Controller extends Thread {
    private final static Logger logger = Logger.getLogger(Controller.class.getName());

    private boolean isRunning;

    private ChannelGroup padClients;
    private ChannelGroup browserClients;

    private PadServer padServer;

    public Controller() {
        isRunning = true;
        this.padClients = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
        this.browserClients = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

        basestationServer = new PadServer(basestationPort, dataQueue);
        k528Server = new K528Server(k528Port, clientGroup);
    }

    @Override
    public void run() {
        basestationServer.start();
        k528Server.start();
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

    public void terminate() throws InterruptedException {
        this.isRunning = false;
        basestationServer.join(3);
        k528Server.join(3);
    }

    private ByteBuf getSendByteBuf(String message)
            throws UnsupportedEncodingException {

        byte[] req = message.getBytes("UTF-8");
        ByteBuf pingMessage = Unpooled.buffer();
        pingMessage.writeBytes(req);

        return pingMessage;
    }
}
