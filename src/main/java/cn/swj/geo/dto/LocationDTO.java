package cn.swj.geo.dto;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * @Author swj
 * @Date 2023/6/8 11:18
 * @Description: TODO
 * @Version 1.0
 */
@Data
public class LocationDTO {

    public LocationDTO() {

    }

    public LocationDTO(String lng, String lat) {
        this.lng = lng;
        this.lat = lat;
    }

    //经度
    private String lng;

    //纬度
    private String lat;

    public void check() {
        if (StrUtil.isEmpty(lng) || StrUtil.isEmpty(lat)) {
            // x 经度  y 纬度
            throw new RuntimeException("x or y is empty");
        }
    }

}
