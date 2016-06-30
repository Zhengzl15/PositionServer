package cc.gauto.beans;

import io.netty.channel.Channel;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-28 22:13
 */
public class CarDistanceInfo implements Comparable{
    public Channel channel;
    public double distance;

    public CarDistanceInfo(Channel channel, double distance) {
        this.channel = channel;
        this.distance = distance;
    }

    @Override
    public int compareTo(Object o1) {
        CarDistanceInfo car1 = (CarDistanceInfo)o1;
        if (this.distance < car1.distance) {
            return 1;
        } else {
            return -1;
        }
    }
}
