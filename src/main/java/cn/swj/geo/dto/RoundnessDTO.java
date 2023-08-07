package cn.swj.geo.dto;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * @Author swj
 * @Date 2023/7/11 15:27
 * @Description: 构造圆形区域的参数
 * @Version 1.0
 */
@Data
public class RoundnessDTO {

    public RoundnessDTO() {
    }

    public RoundnessDTO(String x, String y, String radius) {
        this.setCenterPoint(new LocationDTO(x, y));
        this.radius = radius;
    }

    public RoundnessDTO(LocationDTO locationDTO, String radius) {
        this.setCenterPoint(locationDTO);
        this.radius = radius;
    }

    /**
     * 圆心*
     */
    private LocationDTO centerPoint;

    /**
     * 半径 以米为单位*
     */
    private String radius;

    public void check() {
        centerPoint.check();
        if (StrUtil.isBlank(radius)) {
            throw new RuntimeException("radius is empty");
        }
    }

}
