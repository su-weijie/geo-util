package cn.swj.geo.dto;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Entity;

/**
 * @Author swj
 * @Date 2023/6/8 11:18
 * @Description: 坐标DTO
 * @Version 1.0
 */
@Data
@ApiModel(value = "经纬度对象")
public class LocationDTO {

    public LocationDTO() {

    }

    public LocationDTO(String lng, String lat) {
        Double.valueOf(lng);
        Double.valueOf(lat);
        this.lng = lng;
        this.lat = lat;
    }

    /**
     * 经度*
     */
    @ApiModelProperty(value = "经度", notes = "小数")
    private String lng;

    /**
     * 纬度*
     */
    @ApiModelProperty(value = "纬度", notes = "小数")
    private String lat;

    public void check() {
        if (!StrUtil.isAllNotBlank(lng, lat)) {
            // x 经度  y 纬度
            throw new RuntimeException("x or y is empty");
        }
    }

    /**
     * 校验位置是否一致*
     *
     * @param o
     * @return
     */
    public boolean equalsObj(Object o) {
        return equalsObj(o, null, null);
    }


    /**
     * 校验位置是否一致*
     *
     * @param o
     * @return
     */
    public boolean equalsObj(Object o, String lngField, String latField) {
        if (!StrUtil.isAllNotBlank(lngField, latField)) {
            lngField = "lng";
            latField = "lat";
        }
        JSONObject jsonObject = JSONUtil.parseObj(o);
        String lng = jsonObject.getStr(lngField);
        String lat = jsonObject.getStr(latField);
        if (!StrUtil.isAllNotBlank(lng, lat)) {
            throw new RuntimeException(lngField + " or " + latField + " field is not have");
        }
        return this.lng.equals(lng) && this.lat.equals(lat);
    }

}
