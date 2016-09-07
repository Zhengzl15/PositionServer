package cc.gauto.tcpserver;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-13 20:37
 */

//向pad端开放的，用于向pad发送指令以及接收pad发送的数据
import cc.gauto.beans.CarDataBean;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class PadServer extends Thread {
    private static final Logger logger = Logger.getLogger(PadServer.class.getName());
    private int port;
    private ChannelGroup padClients;
    private HashMap<String, HashMap<Channel, CarDataBean> > totalData;

    public PadServer(int port, ChannelGroup channels, HashMap<String, HashMap<Channel, CarDataBean> > data) {
        this.port = port;
        this.padClients = channels;
        this.totalData = data;
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1 /* number of threads */ );
        EventLoopGroup workerGroup = new NioEventLoopGroup(2 /* number of threads */);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);   //修改为长连接方式
            b.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("ping", new IdleStateHandler(5, 5, 0, TimeUnit.SECONDS));
                    ch.pipeline().addLast(
                            new LineBasedFrameDecoder(1024),
                            new StringDecoder(),
                            new StringEncoder(),
                            new ByteArrayDecoder(),
                            new ByteArrayEncoder(),
                            new PadHandler(padClients, totalData));

                }
            });
            //b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
            // Bind and start to accept incoming connections.
            Channel ch = b.bind(port).sync().channel();
            logger.info("Pad Server started on port [" + port + "]");

            ch.closeFuture().sync();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
