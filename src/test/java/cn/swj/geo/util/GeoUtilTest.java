package cn.swj.geo.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.swj.geo.dto.LocationDTO;
import cn.swj.geo.dto.RoundnessDTO;
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

    @Test
    void calculateShortestDistanceFromLine4Objs() {
        //List<LocationDTO> lineLocationDTOList = new ArrayList<>();
        //lineLocationDTOList.add(new LocationDTO("113.12345", "34.56789"));
        //lineLocationDTOList.add(new LocationDTO("113.54321", "34.98765"));
        //
        //
        //
        //List<JSONObject> objectList = new ArrayList<>();
        //objectList.add(JSONUtil.createObj().put("lng", "113.15678").put("lat", "34.56789").put("name", "Location A"));
        //objectList.add(JSONUtil.createObj().put("lng", "113.18765").put("lat", "34.87654").put("name", "Location B"));
        //objectList.add(JSONUtil.createObj().put("lng", "115.23456").put("lat", "34.34567").put("name", "Location C"));
        //
        //String distance = "10000";
        //
        //List<JSONObject> jsonObjects = GeoUtil.calculateShortestDistanceFromLine4Objs(lineLocationDTOList, objectList, distance);
        //System.out.println("在指定距离内的点位列表:");
        //jsonObjects.forEach(System.out::println);
        List<LocationDTO> lineLocationDTOList = new ArrayList<>();
        lineLocationDTOList.add(new LocationDTO("113.12345", "34.56789"));
        lineLocationDTOList.add(new LocationDTO("113.54321", "34.98765"));



        List<JSONObject> objectList = new ArrayList<>();
        objectList.add(JSONUtil.createObj().put("ln", "113.15678").put("la", "34.56789").put("name", "Location A"));
        objectList.add(JSONUtil.createObj().put("ln", "113.18765").put("la", "34.87654").put("name", "Location B"));
        objectList.add(JSONUtil.createObj().put("ln", "115.23456").put("la", "34.34567").put("name", "Location C"));

        String distance = "10000";

        List<JSONObject> jsonObjects = GeoUtil.calculateShortestDistanceFromLine4Objs(lineLocationDTOList, objectList, distance,"ln","la");
        System.out.println("在指定距离内的点位列表:");
        jsonObjects.forEach(System.out::println);
    }

    @Test
    void objListIsContainedRegion() {
        List<LocationDTO> regionLocationDTOList = new ArrayList<>();
        regionLocationDTOList.add(new LocationDTO("113.12345", "34.56789"));
        regionLocationDTOList.add(new LocationDTO("113.54321", "34.98765"));
        regionLocationDTOList.add(new LocationDTO("113.87654", "34.12345"));


        List<JSONObject> objectList = new ArrayList<>();
        objectList.add(JSONUtil.createObj().put("lng", "113.45678").put("lat", "34.56789").put("name", "Location A"));
        objectList.add(JSONUtil.createObj().put("lng", "113.98765").put("lat", "34.87654").put("name", "Location B"));
        objectList.add(JSONUtil.createObj().put("lng", "113.23456").put("lat", "34.34567").put("name", "Location C"));


        List<JSONObject> jsonObjectList = GeoUtil.objListIsContainedRegionSequence(regionLocationDTOList, objectList);
        System.out.println("在指定区域内的点位列表:");
        jsonObjectList.forEach(System.out::println);

    }

    @Test
    void objListIsContainedRegionSequence() {
        //List<LocationDTO> regionLocationDTOList = new ArrayList<>();
        //regionLocationDTOList.add(new LocationDTO("113.12345", "34.56789"));
        //regionLocationDTOList.add(new LocationDTO("113.54321", "34.98765"));
        //regionLocationDTOList.add(new LocationDTO("113.87654", "34.12345"));
        //
        //
        //List<JSONObject> objectList = new ArrayList<>();
        //objectList.add(JSONUtil.createObj().put("lng", "113.45678").put("lat", "34.56789").put("name", "Location A"));
        //objectList.add(JSONUtil.createObj().put("lng", "113.98765").put("lat", "34.87654").put("name", "Location B"));
        //objectList.add(JSONUtil.createObj().put("lng", "113.23456").put("lat", "34.34567").put("name", "Location C"));
        //
        //
        //List<JSONObject> jsonObjectList = GeoUtil.objListIsContainedRegionSequence(regionLocationDTOList, objectList);
        //System.out.println("在指定区域内的点位列表:");
        //jsonObjectList.forEach(System.out::println);

        List<LocationDTO> regionLocationDTOList = new ArrayList<>();
        regionLocationDTOList.add(new LocationDTO("113.12345", "34.56789"));
        regionLocationDTOList.add(new LocationDTO("113.54321", "34.98765"));
        regionLocationDTOList.add(new LocationDTO("113.87654", "34.12345"));


        List<JSONObject> objectList = new ArrayList<>();
        objectList.add(JSONUtil.createObj().put("ln", "113.45678").put("la", "34.56789").put("name", "Location A"));
        objectList.add(JSONUtil.createObj().put("ln", "113.98765").put("la", "34.87654").put("name", "Location B"));
        objectList.add(JSONUtil.createObj().put("ln", "113.23456").put("la", "34.34567").put("name", "Location C"));


        List<JSONObject> jsonObjectList = GeoUtil.objListIsContainedRegionSequence(regionLocationDTOList, objectList,"ln","la");
        System.out.println("在指定区域内的点位列表:");
        jsonObjectList.forEach(System.out::println);

    }

    @Test
    void objListIsContainedRoundRegion() {

        //RoundnessDTO roundnessDTO = new RoundnessDTO(new LocationDTO("113.12345", "34.56789"), "1000");
        //
        //
        //List<JSONObject> objectList = new ArrayList<>();
        //objectList.add(JSONUtil.createObj().put("lng", "113.12365").put("lat", "34.56789").put("name", "Location A"));
        //objectList.add(JSONUtil.createObj().put("lng", "113.98765").put("lat", "34.87654").put("name", "Location B"));
        //objectList.add(JSONUtil.createObj().put("lng", "113.23456").put("lat", "34.34567").put("name", "Location C"));
        //
        //List<JSONObject> jsonObjects = GeoUtil.objListIsContainedRoundRegion(roundnessDTO, objectList);
        //System.out.println("在指定圆形区域内的点位列表:");
        //jsonObjects.forEach(System.out::println);

        RoundnessDTO roundnessDTO = new RoundnessDTO(new LocationDTO("113.12345", "34.56789"), "1000");


        List<JSONObject> objectList = new ArrayList<>();
        objectList.add(JSONUtil.createObj().put("ln", "113.12365").put("la", "34.56789").put("name", "Location A"));
        objectList.add(JSONUtil.createObj().put("ln", "113.98765").put("la", "34.87654").put("name", "Location B"));
        objectList.add(JSONUtil.createObj().put("ln", "113.23456").put("la", "34.34567").put("name", "Location C"));

        List<JSONObject> jsonObjects = GeoUtil.objListIsContainedRoundRegion(roundnessDTO, objectList,"ln","la");
        System.out.println("在指定圆形区域内的点位列表:");
        jsonObjects.forEach(System.out::println);


    }

    @Test
    void calculateDistanceByObj() {
        LocationDTO startLocation = new LocationDTO("113.12345", "34.56789");
        LocationDTO endLocation = new LocationDTO("113.98765", "34.87654");

        JSONObject start = JSONUtil.createObj().put("lng", "113.12345").put("lat", "34.56789").put("name", "Location B");
        JSONObject end = JSONUtil.createObj().put("lng", "113.98765").put("lat", "34.87654").put("name", "Location B");

        String distance = GeoUtil.calculateDistanceByObj(start, end);
        System.out.println("起点和终点之间的距离为: " + distance + " 米");

    }


    @Test
    void calculateShortestDistanceFromCurve4Objs() {
        //List<LocationDTO> curveLocationDTOList = new ArrayList<>();
        //curveLocationDTOList.add(new LocationDTO("113.12345", "34.56789"));
        //curveLocationDTOList.add(new LocationDTO("113.54321", "34.98765"));
        //curveLocationDTOList.add(new LocationDTO("113.87654", "34.12345"));
        //
        //List<JSONObject> objectList = new ArrayList<>();
        //objectList.add(JSONUtil.createObj().put("lng", "113.45678").put("lat", "34.56789").put("name", "Location A"));
        //objectList.add(JSONUtil.createObj().put("lng", "113.98765").put("lat", "34.87654").put("name", "Location B"));
        //objectList.add(JSONUtil.createObj().put("lng", "116.23456").put("lat", "34.34567").put("name", "Location C"));
        //
        //String distance = "100000";
        //
        //List<JSONObject> jsonObjectList = GeoUtil.calculateShortestDistanceFromCurve4Objs(curveLocationDTOList, objectList, distance);
        //jsonObjectList.forEach(System.out::println);
        List<LocationDTO> curveLocationDTOList = new ArrayList<>();
        curveLocationDTOList.add(new LocationDTO("113.12345", "34.56789"));
        curveLocationDTOList.add(new LocationDTO("113.54321", "34.98765"));
        curveLocationDTOList.add(new LocationDTO("113.87654", "34.12345"));

        List<JSONObject> objectList = new ArrayList<>();
        objectList.add(JSONUtil.createObj().put("ln", "113.45678").put("la", "34.56789").put("name", "Location A"));
        objectList.add(JSONUtil.createObj().put("ln", "113.98765").put("la", "34.87654").put("name", "Location B"));
        objectList.add(JSONUtil.createObj().put("ln", "116.23456").put("la", "34.34567").put("name", "Location C"));

        String distance = "100000";

        List<JSONObject> jsonObjectList = GeoUtil.calculateShortestDistanceFromCurve4Objs(curveLocationDTOList, objectList, distance,"ln","la");
        jsonObjectList.forEach(System.out::println);
    }
}

