package cc.gauto.utils;

import cc.gauto.beans.CarDistanceInfo;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @Author Zhilong Zheng
 * @Email zhengzl0715@163.com
 * @Date 2016-06-28 21:46
 */
//类似矩阵的一个table，用作存储距离
public class DistanceTable {
    private final static Logger logger = Logger.getLogger(DistanceTable.class.getName());
    private Map<Channel, ArrayList<CarDistanceInfo>> table = new HashMap<>();

    //使用表项初始化
    public DistanceTable(List<Channel> keys) {
        for (Channel channel : keys) {
            ArrayList<CarDistanceInfo> infos = new ArrayList<>();
            table.put(channel, infos);
        }
    }

    //添加一项数据
    public void addRecord(Channel src, Channel des, double distance) {
        CarDistanceInfo info = new CarDistanceInfo(des, distance);
        if (this.table.containsKey(src)) {
            this.table.get(src).add(info);
        } else {
            logger.warn("Src channel does not exist");
        }
    }

    //对表中每行数据按距离从大到大排序
    public void sortByMin() {
        for (Map.Entry<Channel, ArrayList<CarDistanceInfo>> entry : this.table.entrySet()) {
            Collections.sort(entry.getValue());
        }
    }

    public Map<Channel, ArrayList<CarDistanceInfo>> getTable() {
        return this.table;
    }
}
