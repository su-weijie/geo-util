package cn.swj.geo.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.swj.geo.dto.LocationDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author swj
 * @Date 2023/8/7 18:05
 * @Description: TODO
 * @Version 1.0
 */
class GeoUtilTest {

    @Test
    void test1() {
        // 准备测试数据
        List<LocationDTO> locationDTOList = new ArrayList<>();
        locationDTOList.add(new LocationDTO("10.123", "20.456"));
        locationDTOList.add(new LocationDTO("30.789", "40.012"));

        List<JSONObject> objectList = new ArrayList<>();
        objectList.add(JSONUtil.createObj().put("lng", "10.123").put("lat", "20.456").put("name", "Location A"));
        objectList.add(JSONUtil.createObj().put("lng", "30.789").put("lat", "40.012").put("name", "Location B"));
        objectList.add(JSONUtil.createObj().put("lng", "50.111").put("lat", "60.222").put("name", "Location C"));

// 调用方法
        List<JSONObject> jsonObjects = GeoUtil.adaptLocationToObjects(locationDTOList, objectList);

        System.out.println(jsonObjects.toString());
    }
}
