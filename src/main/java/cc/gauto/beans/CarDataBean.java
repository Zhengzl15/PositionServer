package cc.gauto.beans;

import io.netty.channel.Channel;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-28 20:54
 */
public class CarDataBean {
    public Channel channel;
    public long examId;
    public String deviceId;
    public double posX;
    public double posY;
    public double speed;
    public double angle;

    public CarDataBean(Channel c, long e, String d, double x, double y, double s, double a) {
        this.channel = c;
        this.examId = e;
        this.deviceId = d;
        this.posX = x;
        this.posY = y;
        this.speed = s;
        this.angle = a;
    }
}
