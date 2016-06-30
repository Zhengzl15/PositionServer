package cc.gauto.tcpserver;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-13 20:47
 */
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class PadHandler extends SimpleChannelInboundHandler {
    private static final Logger logger = Logger.getLogger(PadHandler.class.getName());
    private ChannelGroup padClients;
    private HashMap<Channel, String> posData;

    public PadHandler() {
        super();
    }

    public PadHandler(ChannelGroup channels, HashMap<Channel, String> data) {
        this.padClients = channels;
        this.posData = data;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //该数据包含位置，速度，角度
        //数据格式为:
        String body = (String)msg;
        logger.info("recv " + body.length() + " on " + ctx.channel().localAddress());
        posData.put(ctx.channel(), body);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
    }

    /*
         * 新的client链接
         */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("A new pad client connected: " + ctx.channel().remoteAddress());
        super.channelRegistered(ctx);
        padClients.add(ctx.channel());
        posData.put(ctx.channel(), null);
    }

    /*
     * client断开链接
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("A pad client disconnected: " + ctx.channel().localAddress());
        super.channelUnregistered(ctx);
        padClients.remove(ctx.channel());
        posData.remove(ctx.channel());
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
}