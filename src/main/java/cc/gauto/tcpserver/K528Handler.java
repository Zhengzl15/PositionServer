package cc.gauto.tcpserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-13 21:58
 */
public class K528Handler extends SimpleChannelInboundHandler {
    private static final Logger logger = Logger.getLogger(K528Handler.class.getName());
    private ChannelGroup group;

    public K528Handler() {
        super();
    }

    public K528Handler(ChannelGroup group) {
        super();
        this.group = group;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
    }

    /*
      * 新的client链接
    */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("A new k528 client connected: " + ctx.channel().remoteAddress());
        super.channelRegistered(ctx);
        group.add(ctx.channel());
    }

    /*
     * client断开链接
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("A k528 client disconnected: " + ctx.channel().localAddress());
        super.channelUnregistered(ctx);
        group.remove(ctx.channel());
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
