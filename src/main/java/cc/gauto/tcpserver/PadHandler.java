package cc.gauto.tcpserver;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-13 20:47
 */
import cc.gauto.beans.CarDataBean;
import cc.gauto.utils.Constrains;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class PadHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(PadHandler.class.getName());
    private ChannelGroup padClients;
    private HashMap<String, HashMap<Channel, CarDataBean> > totalData;   //是全局的

    public PadHandler() {
        super();
    }

    public PadHandler(ChannelGroup channels, HashMap<String, HashMap<Channel, CarDataBean> > data) {
        this.padClients = channels;
        this.totalData = data;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state() == IdleState.READER_IDLE) {
                //很长时间未进行读操作，前面设置的那个超时时间，则可能异常断线了。
                ctx.writeAndFlush(getSendByteBuf("ping"));
                logger.warn("It may disconnected");
            } else if (event.state() == IdleState.WRITER_IDLE) {
                //ctx.channel().writeAndFlush("ping");
                //ctx.channel().writeAndFlush(getSendByteBuf("ping"));
            } else if (event.state() == IdleState.ALL_IDLE) {
                logger.warn("It may disconnected all idle");
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //该数据包含examid,位置，速度，角度
        //数据格式为:deviceid,examid,x,y,speed,angle\n
        String body = (String)msg;
        logger.info("Recved: " + body);
        if (msg.equals("ping")) {  //心跳
            logger.info("A heartbeat message from " + ctx.channel().remoteAddress());
            //ctx.writeAndFlush(getSendByteBuf("ping"));
        }
        //logger.info("recv " + body.length() + " on " + ctx.channel().localAddress());
        //解析数据
        Channel channel = ctx.channel();
        String data = body;
        if (data == null) {
            return;
        }
        data = data.replaceAll("\n", ""); //去掉换行符
        String[] splitStr = data.split(",");
        if (splitStr.length != Constrains.RECORD_NUM) {
            logger.error("Device sent an invalid message");
            return;
        }
        String deviceId = splitStr[0];
        long examId = Long.parseLong(splitStr[1]);
        double posX = Double.parseDouble(splitStr[2]);
        double posY = Double.parseDouble(splitStr[3]);
        double speed = Double.parseDouble(splitStr[4]);
        double angle = Double.parseDouble(splitStr[5]);
        CarDataBean carDataBean = new CarDataBean(channel, examId, deviceId, posX, posY, speed, angle);
        String key = deviceId.substring(0, Constrains.SUB_NUM);
        if (totalData.containsKey(key)) {
            totalData.get(key).put(channel, carDataBean);
        }
    }

    /*
         * 新的client链接
         */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("A new pad client connected: " + ctx.channel().remoteAddress());
        //ctx.writeAndFlush(getSendByteBuf("hello"));
        super.channelRegistered(ctx);
        padClients.add(ctx.channel());
        //posData.put(ctx.channel(), null);
    }

    /*
     * client断开链接
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("A pad client disconnected: " + ctx.channel().localAddress());
        super.channelUnregistered(ctx);
        padClients.remove(ctx.channel());
        //posData.remove(ctx.channel());
        //解注册
        for (Map.Entry<String, HashMap<Channel, CarDataBean>> entry : totalData.entrySet()) {
            entry.getValue().remove(ctx.channel());

        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Unexpected exception from channel. " + cause.getMessage());
        ctx.close();
    }

    private ByteBuf getSendByteBuf(String message)
            throws UnsupportedEncodingException {

        byte[] req = message.getBytes("UTF-8");
        ByteBuf pingMessage = Unpooled.buffer();
        pingMessage.writeBytes(req);

        return pingMessage;
    }
}