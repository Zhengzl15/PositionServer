package cc.gauto.tcpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-13 22:00
 */
public class K528Server extends Thread {
    private static final Logger logger = Logger.getLogger(K528Server.class.getName());
    private int port;
    private ChannelGroup clients;

    public K528Server(int port, ChannelGroup group) {
        this.port = port;
        this.clients = group;
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1 /* number of threads */ );
        EventLoopGroup workerGroup = new NioEventLoopGroup(2 /* number of threads */);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            //new LineBasedFrameDecoder(1024),
                            new StringDecoder(),
                            new K528Handler(clients));
                }
            });
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
            b.option(ChannelOption.SO_BACKLOG, 1024);

            // Bind and start to accept incoming connections.
            Channel ch = b.bind(port).sync().channel();
            logger.info("K528 Server started on port [" + port + "]");

            ch.closeFuture().sync();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}