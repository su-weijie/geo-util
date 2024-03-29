package cn.swj.geo.util;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.swj.geo.dto.LocationDTO;
import cn.swj.geo.dto.RoundnessDTO;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.*;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @Author swj
 * @Date 2023/7/17 13:16
 * @Description: 地理空间信息处理工具类
 * @Version 1.0
 */
public class GeoUtil {

    // 创建GeometryFactory
    private static GeometryFactory geometryFactory;

    static {
        geometryFactory = JTSFactoryFinder.getGeometryFactory();
    }

    private static final String MARS02_EPSG_CODE = "EPSG:4490";
    private static final String WGS84_EPSG_CODE = "EPSG:4326";

    /**
     * createCircle*
     *
     * @param roundnessDTO 构造圆形区域的参数
     * @return
     */
    private static Geometry createCircle(RoundnessDTO roundnessDTO) {
        // 圆心坐标
        String centerLon = roundnessDTO.getCenterPoint().getLng(); // 圆心经度
        String centerLat = roundnessDTO.getCenterPoint().getLat(); // 圆心纬度

        // 圆的半径（以米为单位）
        double radius = Double.valueOf(roundnessDTO.getRadius());


        // 创建圆心点
        Coordinate centerCoordinate = createCoordinate(centerLon, centerLat);
        Point centerPoint = geometryFactory.createPoint(centerCoordinate);

        // 创建圆形区域
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        GeodeticCalculator calculator = new GeodeticCalculator(crs);
        calculator.setStartingGeographicPoint(Double.valueOf(centerLon), Double.valueOf(centerLat));
        calculator.setDirection(0.0, radius);
        DirectPosition edgePos = null;
        try {
            edgePos = calculator.getDestinationPosition();
        } catch (TransformException e) {
            throw new RuntimeException("计算地图中真正对应的半径失败");
        }
        Coordinate edgeCoordinate = new Coordinate(edgePos.getOrdinate(0), edgePos.getOrdinate(1));
        Point edgePoint = geometryFactory.createPoint(edgeCoordinate);
        double edgeDistance = centerPoint.distance(edgePoint);
        Geometry circle = centerPoint.buffer(edgeDistance);

        if (circle.isEmpty()) {
            throw new RuntimeException("Circle is empty");
        }

        return circle;
    }

    /**
     * 构造几何图形*
     *
     * @param locationDTOList 构造几何图形的点位
     * @param izUseAlg        是否使用凹凸算法
     * @return
     */
    private static Polygon getPolygon(List<LocationDTO> locationDTOList, boolean izUseAlg) {
        List<Coordinate> coordinateList = new ArrayList<>();

        Polygon polygon = null;

        for (LocationDTO locationDTO : locationDTOList) {
            double longitude = Double.parseDouble(locationDTO.getLng());
            double latitude = Double.parseDouble(locationDTO.getLat());
            Coordinate coordinate = new Coordinate(longitude, latitude);
            coordinateList.add(coordinate);
        }

        // 优化点集
        coordinateList = optimizeCoordinates(coordinateList);

        Coordinate[] pointCoordinates = coordinateList.toArray(new Coordinate[0]);

        if (izUseAlg) {
            // 创建Polygon对象
            ConvexHull convexHull = new ConvexHull(pointCoordinates, geometryFactory);
            Geometry convexPolygon = convexHull.getConvexHull();

            // 确保生成的是多边形
            if (convexPolygon instanceof Polygon) {
                polygon = (Polygon) convexPolygon;
            } else {
                throw new RuntimeException("Failed to generate convex polygon");
            }

        } else {
            polygon = geometryFactory.createPolygon(pointCoordinates);
        }
        if (ObjectUtil.isEmpty(polygon)) {
            throw new RuntimeException("polygon is empty");
        }

        return polygon;
    }

    //createLineString
    private static LineString createLineString(List<LocationDTO> lineLocationList) {
        Coordinate[] coordinates = new Coordinate[lineLocationList.size()];
        for (int i = 0; i < lineLocationList.size(); i++) {
            LocationDTO location = lineLocationList.get(i);
            coordinates[i] = createCoordinate(location.getLng(), location.getLat());
        }


        // 创建 LineString 对象
        return geometryFactory.createLineString(coordinates);
    }

    /**
     * 在一个曲线上获取距离目标坐标最近的一个点*
     *
     * @param lineString
     * @param targetPoint
     * @return
     */
    private static LocationDTO getMinDistanceLocationDTO(LineString lineString, Point targetPoint) {
        // 初始化最小距离和最近的坐标点
        double minDistance = Double.MAX_VALUE;
        Coordinate closestPoint = null;

        // 遍历曲线上的所有坐标点，找到最近的坐标点
        for (Coordinate curveCoordinate : lineString.getCoordinates()) {
            Point curvePoint = geometryFactory.createPoint(curveCoordinate);
            double temp = curvePoint.distance(targetPoint);
            if (temp < minDistance) {
                minDistance = temp;
                closestPoint = curveCoordinate;
            }
        }

        return new LocationDTO(String.valueOf(closestPoint.getX()), String.valueOf(closestPoint.getY()));
    }

    // 优化点集
    private static List<Coordinate> optimizeCoordinates(List<Coordinate> coordinates) {
        List<Coordinate> optimizedCoordinates = new ArrayList<>();
        optimizedCoordinates.add(coordinates.get(0));

        for (int i = 1; i < coordinates.size(); i++) {
            Coordinate currentCoordinate = coordinates.get(i);
            Coordinate previousCoordinate = coordinates.get(i - 1);

            if (!currentCoordinate.equals2D(previousCoordinate)) {
                optimizedCoordinates.add(currentCoordinate);
            }
        }

        optimizedCoordinates.add(coordinates.get(0)); // 封闭坐标

        return optimizedCoordinates;
    }

    //creat point
    private static Point createPoint(String lng, String lat) {
        return geometryFactory.createPoint(new Coordinate(Double.valueOf(lng), Double.valueOf(lat)));
    }

    //createCoordinate
    private static Coordinate createCoordinate(String lng, String lat) {
        return new Coordinate(Double.valueOf(lng), Double.valueOf(lat));
    }

    //check params
    private static void pointIsContainedRegionCheck(List<LocationDTO> locationDTOList, String x, String y) {
        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is null");
        }

        //不允许点位中 存在有空值
        locationDTOList.stream().forEach(item -> item.check());

        if (!StrUtil.isAllNotBlank(x, y)) {
            throw new RuntimeException("x or y is empty");
        }
    }

    //check params
    private static void pointListIsContainedRegionCheck(List<LocationDTO> regionLocationDTOList, List<LocationDTO> locationDTOList) {
        if (CollectionUtil.isEmpty(regionLocationDTOList)) {
            throw new RuntimeException("regionLocationDTOList is empty");
        }

        regionLocationDTOList.parallelStream().forEach(item -> item.check());

        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }

        locationDTOList.parallelStream().forEach(item -> item.check());
    }

    //check params
    private static void pointIsContainedRoundRegionCheck(RoundnessDTO roundnessDTO, String x, String y) {

        if (ObjectUtil.isEmpty(roundnessDTO)) {
            throw new RuntimeException("roundnessDTO is null");
        }

        roundnessDTO.check();

        if (!StrUtil.isAllNotBlank(x, y)) {
            throw new RuntimeException("x or y is empty");
        }

    }

    //check params
    private static void pointListIsContainedRoundRegionCheck(RoundnessDTO roundnessDTO, List<LocationDTO> locationDTOList) {

        if (ObjectUtil.isEmpty(roundnessDTO)) {
            throw new RuntimeException("roundnessDTO is null");
        }

        roundnessDTO.check();

        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }

        locationDTOList.stream().forEach(item -> item.check());
    }

    //check params
    private static void calculateShortestDistanceFromLineCheck(List<LocationDTO> locationDTOList, String x, String y) {
        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }

        if (!StrUtil.isAllNotBlank(x, y)) {
            throw new RuntimeException("x or y is empty");
        }

        if (locationDTOList.size() != 2) {
            throw new RuntimeException("locationDTOList size is not two");
        }

        locationDTOList.forEach(item -> item.check());
    }

    //check params
    private static void calculateShortestDistanceFromLineCheck(List<LocationDTO> locationDTOList, String x, String y, String distance) {
        calculateShortestDistanceFromLineCheck(locationDTOList, x, y);

        if (StrUtil.isBlank(distance)) {
            throw new RuntimeException("distance is empty");
        }

        if (!StrUtil.isAllNotBlank(x, y)) {
            throw new RuntimeException("x or y is empty");
        }
    }


    //check params
    private static void calculateShortestDistanceFromLine4PointsCheck(List<LocationDTO> lineLocationDTOList, List<LocationDTO> locationDTOList, String distance) {
        if (CollectionUtil.isEmpty(lineLocationDTOList)) {
            throw new RuntimeException("lineLocationDTOList is empty");
        }

        lineLocationDTOList.forEach(item -> item.check());

        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }

        locationDTOList.forEach(item -> item.check());
    }

    //check params
    private static void calculateShortestDistanceFromCurveCheck(List<LocationDTO> curveLocationDTOList, String x, String y) {
        if (CollectionUtil.isEmpty(curveLocationDTOList)) {
            throw new RuntimeException("curveLocationDTOList is empty");
        }

        if (curveLocationDTOList.size() <= 2) {
            throw new RuntimeException("curveLocationDTOList size must ge 3");
        }

        if (!StrUtil.isAllNotBlank(x, y)) {
            throw new RuntimeException("x or y is empty");
        }
    }

    //check params
    private static void calculateShortestDistanceFromCurveCheck(List<LocationDTO> curveLocationDTOList, String x, String y, String distance) {
        calculateShortestDistanceFromCurveCheck(curveLocationDTOList, x, y);
        if (StrUtil.isBlank(distance)) {
            throw new RuntimeException("distance is empty");
        }

        if (Double.valueOf(distance) <= 0) {
            throw new RuntimeException("distance is le 0");
        }
    }

    //checkParams
    private static void calculateShortestDistanceFromCurve4PointsCheck(List<LocationDTO> curveLocationDTOList, List<LocationDTO> locationDTOList, String distance) {
        if (CollectionUtil.isEmpty(curveLocationDTOList)) {
            throw new RuntimeException("curveLocationDTOList is empty");
        }

        curveLocationDTOList.forEach(item -> item.check());

        if (curveLocationDTOList.size() <= 2) {
            throw new RuntimeException("curveLocationDTOList size must ge 3");
        }

        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }

        locationDTOList.forEach(item -> item.check());

        if (Double.valueOf(distance) <= 0) {
            throw new RuntimeException("distance is le 0");
        }
    }

    /**
     * 校验对象中是否有经纬度俩个属性*
     *
     * @param objectList
     */
    private static void checkObjectListIzContainField(List<?> objectList, String lngField, String latField) {
        if (CollectionUtil.isEmpty(objectList)) {
            throw new RuntimeException("objectList is empty");
        }

        objectList.stream().forEach(item -> {
            JSONObject jsonObject = JSONUtil.parseObj(item);
            String lng = jsonObject.getStr(lngField);
            String lat = jsonObject.getStr(latField);
            if (!StrUtil.isAllNotBlank(lng, lat)) {
                throw new RuntimeException("There are objects in the collection without one of the fields (" + lngField + "," + latField + ")");
            }
        });
    }

    //checkParam
    private static void adaptLocationToObjectsCheck(List<LocationDTO> locationDTOList, List<?> objectList, String lngField, String latField) {
        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }
        locationDTOList.parallelStream().forEach(item -> item.check());

        checkObjectListIzContainField(objectList, lngField, latField);

    }

    /**
     * 获取有在当前区域的点位*
     *
     * @param locationDTOList
     * @param polygon
     * @return
     */
    private static List<LocationDTO> getLocationDTOS(List<LocationDTO> locationDTOList, Polygon polygon) {
        //return List
        List<LocationDTO> resList = new ArrayList<>();

        for (LocationDTO locationDTO : locationDTOList) {
            if (ObjectUtil.isEmpty(locationDTO)) {
                continue;
            }
            Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf(locationDTO.getLng()), Double.valueOf(locationDTO.getLat())));

            if (polygon.contains(point)) {
                resList.add(locationDTO);
            }
        }

        return resList;
    }

    //-------------------------------------------------------------------------------------------------------------


    /**
     * 按照 regionLocationList 的点位顺序 组成区域,在判断xy这个坐标是否包含在里面*
     *
     * @param regionLocationList 几何图形组成的点位
     * @param x                  经度
     * @param y                  纬度
     * @return
     */
    public static boolean pointIsContainedRegionSequence(List<LocationDTO> regionLocationList, String x, String y) {

        pointIsContainedRegionCheck(regionLocationList, x, y);

        //几何图形
        Polygon polygon = getPolygon(regionLocationList, false);

        //create point
        Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf(x), Double.valueOf(y)));

        if (polygon.contains(point)) {
            return true;
        }

        return false;
    }


    /**
     * 判断x y 是否在 locationVOList这个区域中，自动取最大几何图形*
     *
     * @param regionLocationList 几何图形组成的点位
     * @param x                  经度
     * @param y                  纬度
     * @return
     */
    public static boolean pointIsContainedRegion(List<LocationDTO> regionLocationList, String x, String y) {

        pointIsContainedRegionCheck(regionLocationList, x, y);

        //几何图形
        Polygon polygon = getPolygon(regionLocationList, true);

        //create point
        Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf(x), Double.valueOf(y)));

        return polygon.contains(point);
    }


    /**
     * 判断point 是否在 locationVOList这个区域中，自动取最大几何图形*
     *
     * @param regionLocationList 几何图形组成的点位
     * @param point              点位
     * @return
     */
    @Deprecated
    public static boolean pointIsContainedRegion(List<LocationDTO> regionLocationList, Point point) {

        if (CollectionUtil.isEmpty(regionLocationList)) {
            throw new RuntimeException("list is null");
        }

        Polygon polygon = getPolygon(regionLocationList, true);

        return polygon.contains(point);
    }


    /**
     * 判断 locationDTOList 里面的点 是否在 locationVOList这个区域中，自动取最大几何图形*
     *
     * @param regionLocationDTOList 几何图形组成的点位
     * @param locationDTOList       待比较的点
     * @return 有在里面的点位信息
     */
    public static List<LocationDTO> pointListIsContainedRegion(List<LocationDTO> regionLocationDTOList, List<LocationDTO> locationDTOList) {

        pointListIsContainedRegionCheck(regionLocationDTOList, locationDTOList);

        //几何图形
        Polygon polygon = getPolygon(regionLocationDTOList, true);

        //return List
        List<LocationDTO> resList = getLocationDTOS(locationDTOList, polygon);

        return resList;
    }


    /**
     * 判断 对象集合 里面的点 是否在 locationVOList这个区域中，自动取最大几何图形*
     *
     * @param regionLocationDTOList 几何图形组成的点位
     * @param objList               待比较的点
     * @return 有在里面的点位信息
     */
    public static <T> List<T> objListIsContainedRegion(List<LocationDTO> regionLocationDTOList, List<T> objList) {
        return objListIsContainedRegion(regionLocationDTOList, objList, "lng", "lat");
    }


    /**
     * 判断 对象集合 里面的点 是否在 locationVOList这个区域中，自动取最大几何图形*
     *
     * @param regionLocationDTOList 几何图形组成的点位
     * @param objList               待比较的点
     * @return 有在里面的点位信息
     */
    public static <T> List<T> objListIsContainedRegion(List<LocationDTO> regionLocationDTOList, List<T> objList, String lngField, String latField) {

        List<LocationDTO> locationDTOList = convert2LocationDTOList(objList, lngField, latField);

        List<LocationDTO> resList = pointListIsContainedRegion(regionLocationDTOList, locationDTOList);

        if (CollectionUtil.isEmpty(resList)) {
            return Collections.emptyList();
        }

        List<T> tList = adaptLocationToObjects(resList, objList, lngField, latField);

        return tList;
    }


    /**
     * 按照 regionLocationDTOList 的点位顺序组成区域,并判断 locationDTOList 里的点位是否在这区域里面*
     *
     * @param regionLocationDTOList 几何图形组成的点位
     * @param locationDTOList       待判断的点位
     * @return
     */
    public static List<LocationDTO> pointListIsContainedRegionSequence(List<LocationDTO> regionLocationDTOList, List<LocationDTO> locationDTOList) {

        pointListIsContainedRegionCheck(regionLocationDTOList, locationDTOList);

        //几何图形
        Polygon polygon = getPolygon(regionLocationDTOList, false);

        List<LocationDTO> resList = getLocationDTOS(locationDTOList, polygon);

        return resList;
    }


    /**
     * 按照 regionLocationDTOList 的点位顺序组成区域,并判断 locationDTOList 里的点位是否在这区域里面*
     *
     * @param regionLocationDTOList 几何图形组成的点位
     * @param objList               对象集合
     * @return
     */
    public static <T> List<T> objListIsContainedRegionSequence(List<LocationDTO> regionLocationDTOList, List<T> objList) {
        return objListIsContainedRegionSequence(regionLocationDTOList, objList, "lng", "lat");
    }


    /**
     * 按照 regionLocationDTOList 的点位顺序组成区域,并判断 locationDTOList 里的点位是否在这区域里面*
     *
     * @param regionLocationDTOList 几何图形组成的点位
     * @param objList               对象集合
     * @return
     */
    public static <T> List<T> objListIsContainedRegionSequence(List<LocationDTO> regionLocationDTOList, List<T> objList, String lngField, String latField) {

        List<LocationDTO> locationDTOList = convert2LocationDTOList(objList, lngField, latField);

        List<LocationDTO> resList = pointListIsContainedRegionSequence(regionLocationDTOList, locationDTOList);

        if (CollectionUtil.isEmpty(resList)) {
            return Collections.emptyList();
        }

        List<T> tList = adaptLocationToObjects(resList, objList, lngField, latField);

        return tList;
    }


    /**
     * 判断x y这个坐标 是否在 roundnessDTO 这个圆中*
     *
     * @param roundnessDTO 圆心和半径
     * @param x            经度
     * @param y            纬度
     * @return
     */
    public static boolean pointIsContainedRoundRegion(RoundnessDTO roundnessDTO, String x, String y) {

        pointIsContainedRoundRegionCheck(roundnessDTO, x, y);

        Geometry circle = createCircle(roundnessDTO);

        // 地址点位坐标
        Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf(x), Double.valueOf(y)));

        // 判断地址点位是否在圆形区域内
        return circle.contains(point);
    }


    /**
     * 判断locationDTOList这些坐标 是否在 roundnessDTO 这个圆中*
     *
     * @param roundnessDTO    圆心和半径
     * @param locationDTOList 待比较的点
     * @return
     */
    public static List<LocationDTO> pointListIsContainedRoundRegion(RoundnessDTO roundnessDTO, List<LocationDTO> locationDTOList) {

        pointListIsContainedRoundRegionCheck(roundnessDTO, locationDTOList);

        Geometry circle = createCircle(roundnessDTO);

        List<LocationDTO> resList = new ArrayList<>();

        for (LocationDTO locationDTO : locationDTOList) {
            if (ObjectUtil.isEmpty(locationDTO)) {
                continue;
            }
            Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf(locationDTO.getLng()), Double.valueOf(locationDTO.getLat())));
            if (circle.contains(point)) {
                resList.add(locationDTO);
            }
        }

        return resList;
    }

    /**
     * 判断 objList 这些对象集合中的坐标 是否在 roundnessDTO 这个圆中*
     *
     * @param roundnessDTO 圆心和半径
     * @param objList      待比较的点
     * @return
     */
    public static <T> List<T> objListIsContainedRoundRegion(RoundnessDTO roundnessDTO, List<T> objList) {
        return objListIsContainedRoundRegion(roundnessDTO, objList, "lng", "lat");
    }

    /**
     * 判断 objList 这些对象集合中的坐标 是否在 roundnessDTO 这个圆中*
     *
     * @param roundnessDTO 圆心和半径
     * @param objList      待比较的点
     * @return
     */
    public static <T> List<T> objListIsContainedRoundRegion(RoundnessDTO roundnessDTO, List<T> objList, String lngField, String latField) {

        List<LocationDTO> locationDTOList = convert2LocationDTOList(objList, lngField, latField);

        List<LocationDTO> resList = pointListIsContainedRoundRegion(roundnessDTO, locationDTOList);

        if (CollectionUtil.isEmpty(resList)) {
            return Collections.emptyList();
        }

        List<T> tList = adaptLocationToObjects(resList, objList, lngField, latField);

        return tList;
    }


    /**
     * 计算俩个点之间的距离*
     *
     * @param startLocation 开始坐标
     * @param endLocation   结束坐标
     * @return
     */
    public static String calculateDistance(LocationDTO startLocation, LocationDTO endLocation) {

        startLocation.check();
        endLocation.check();

        // 定义坐标参考系统（这里使用默认的 WGS84 坐标系）
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;

        // 创建地理计算器
        GeodeticCalculator calculator = new GeodeticCalculator(crs);

        // 设置起始点坐标
        calculator.setStartingGeographicPoint(Double.valueOf(startLocation.getLng()), Double.valueOf(startLocation.getLat()));

        // 设置目标点坐标
        calculator.setDestinationGeographicPoint(Double.valueOf(endLocation.getLng()), Double.valueOf(endLocation.getLat()));

        // 计算直线距离
        double distance = calculator.getOrthodromicDistance();

        return String.valueOf(distance);
    }

    /**
     * 计算俩个点之间的距离*
     *
     * @param startObj 开始坐标
     * @param endObj   结束坐标
     * @return
     */
    public static String calculateDistanceByObj(Object startObj, Object endObj) {

        LocationDTO startLocation = convert2LocationDTO(startObj);
        LocationDTO endLocation = convert2LocationDTO(endObj);

        return calculateDistance(startLocation, endLocation);
    }


    /**
     * 计算一个经纬度点到由两个经纬度点组成的线的最短距离*
     *
     * @param lineLocationList 组成线得俩个点位
     * @param x                x
     * @param y                y
     * @return
     */
    public static String calculateShortestDistanceFromLine(List<LocationDTO> lineLocationList, String x, String y) {

        calculateShortestDistanceFromLineCheck(lineLocationList, x, y);

        LineString lineString = createLineString(lineLocationList);

        LocationDTO minDistanceLocationDTO = getMinDistanceLocationDTO(lineString, createPoint(x, y));

        // 计算点到线的最短距离
        String minDistance = calculateDistance(new LocationDTO(x, y), minDistanceLocationDTO);

        return minDistance;
    }

    /**
     * 判断一个经纬度点到由两个经纬度点组成的线的垂直距离 是否 小于指定值 distance*
     *
     * @param lineLocationList 线的俩个 点
     * @param x                x
     * @param y                y
     * @param distance         指定最大距离
     * @return
     */
    public static boolean calculateShortestDistanceFromLine(List<LocationDTO> lineLocationList, String x, String y, String distance) {

        calculateShortestDistanceFromLineCheck(lineLocationList, x, y, distance);

        String shortestDistance = calculateShortestDistanceFromLine(lineLocationList, x, y);

        return Double.valueOf(shortestDistance) < Double.valueOf(distance);
    }

    /**
     * 判断多个个经纬度点到由两个经纬度点组成的线的最短距离 是否 小于指定值 distance*
     *
     * @param lineLocationDTOList 线的点
     * @param locationDTOList     点位列表
     * @param distance            指定最大距离
     * @return
     */
    public static List<LocationDTO> calculateShortestDistanceFromLine4Points(List<LocationDTO> lineLocationDTOList, List<LocationDTO> locationDTOList, String distance) {

        calculateShortestDistanceFromLine4PointsCheck(lineLocationDTOList, locationDTOList, distance);

        //在指定范围内的经纬度
        List<LocationDTO> resLocationList = new ArrayList<>();

        LineString lineString = createLineString(lineLocationDTOList);

        // 创建线段


        for (LocationDTO locationDTO : locationDTOList) {
            // 将点投影到线上

            Point point = createPoint(locationDTO.getLng(), locationDTO.getLat());

            LocationDTO minDistanceLocationDTO = getMinDistanceLocationDTO(lineString, point);

            // 计算点到线的最短距离
            String minDistace = calculateDistance(locationDTO, minDistanceLocationDTO);

            if (Double.valueOf(minDistace) < Double.valueOf(distance)) {
                resLocationList.add(locationDTO);
            }
        }

        return resLocationList;
    }


    /**
     * 判断多个个经纬度点到由两个经纬度点组成的线的最短距离 是否 小于指定值 distance*
     *
     * @param lineLocationDTOList 线的点
     * @param objList             对象列表
     * @param distance            指定最大距离
     * @return
     */
    public static <T> List<T> calculateShortestDistanceFromLine4Objs(List<LocationDTO> lineLocationDTOList, List<T> objList, String distance) {
        return calculateShortestDistanceFromLine4Objs(lineLocationDTOList, objList, distance, "lng", "lat");
    }


    /**
     * 判断多个个经纬度点到由两个经纬度点组成的线的最短距离 是否 小于指定值 distance*
     *
     * @param lineLocationDTOList 线的点
     * @param objList             对象列表
     * @param distance            指定最大距离
     * @param lngField            经度的字段名
     * @param latField            纬度的字段名
     * @return
     */
    public static <T> List<T> calculateShortestDistanceFromLine4Objs(List<LocationDTO> lineLocationDTOList, List<T> objList, String distance, String lngField, String latField) {

        List<LocationDTO> locationDTOList = convert2LocationDTOList(objList, lngField, latField);

        List<LocationDTO> resList = calculateShortestDistanceFromLine4Points(lineLocationDTOList, locationDTOList, distance);

        if (CollectionUtil.isEmpty(resList)) {
            return Collections.emptyList();
        }

        List<T> tList = adaptLocationToObjects(resList, objList, lngField, latField);

        return tList;
    }

    /**
     * 计算一个经纬度点到由多个个经纬度点组成的曲线线的最短距离*
     *
     * @param curveLocationDTOList
     * @param x
     * @param y
     * @return
     */
    public static String calculateShortestDistanceFromCurve(List<LocationDTO> curveLocationDTOList, String x, String y) {

        calculateShortestDistanceFromCurveCheck(curveLocationDTOList, x, y);

        // 创建 Coordinate 数组并添加经纬度点坐标
        LineString lineString = createLineString(curveLocationDTOList);

        // 创建 Point 对象
        Point point = geometryFactory.createPoint(createCoordinate(x, y));

        LocationDTO minDistanceLocationDTO = getMinDistanceLocationDTO(lineString, point);

        String distance = calculateDistance(new LocationDTO(x, y), minDistanceLocationDTO);

        return distance;
    }

    /**
     * 判断一个经纬度点到由两个经纬度点组成的线的最短距离 是否 小于指定值 distance*
     *
     * @param locationDTOList
     * @param x
     * @param y
     * @param distance        指定最大距离
     * @return
     */
    public static boolean calculateShortestDistanceFromCurve(List<LocationDTO> locationDTOList, String x, String y, String distance) {

        calculateShortestDistanceFromCurveCheck(locationDTOList, x, y, distance);

        String oDistance = calculateShortestDistanceFromCurve(locationDTOList, x, y);

        return Double.valueOf(oDistance) < Double.valueOf(distance);
    }

    /**
     * 判断多个个经纬度点到由两个经纬度点组成的线的最短距离 是否 小于指定值 distance*
     *
     * @param curveLocationDTOList 线的点
     * @param locationDTOList      点位列表
     * @param distance             指定最大距离
     * @return
     */
    public static List<LocationDTO> calculateShortestDistanceFromCurve4Points(List<LocationDTO> curveLocationDTOList, List<LocationDTO> locationDTOList, String distance) {

        calculateShortestDistanceFromCurve4PointsCheck(curveLocationDTOList, locationDTOList, distance);

        //在指定范围内的经纬度
        List<LocationDTO> resLocationList = new ArrayList<>();

        // 创建 Coordinate 数组并添加经纬度点坐标
        LineString lineString = createLineString(curveLocationDTOList);

        for (LocationDTO locationDTO : locationDTOList) {

            Point targetPoint = createPoint(locationDTO.getLng(), locationDTO.getLat());

            LocationDTO minDistanceLocationDTO = getMinDistanceLocationDTO(lineString, targetPoint);

            String distanceInMeters = calculateDistance(locationDTO, minDistanceLocationDTO);

            if (Double.valueOf(distanceInMeters) <= Double.valueOf(distance)) {
                resLocationList.add(locationDTO);
            }
        }

        return resLocationList;
    }

    /**
     * 判断多个个经纬度点到由两个经纬度点组成的线的最短距离 是否 小于指定值 distance*
     *
     * @param curveLocationDTOList 线的点
     * @param objList              对象列表
     * @param distance             指定最大距离
     * @return
     */
    public static <T> List<T> calculateShortestDistanceFromCurve4Objs(List<LocationDTO> curveLocationDTOList, List<T> objList, String distance) {
        return calculateShortestDistanceFromCurve4Objs(curveLocationDTOList, objList, distance, "lng", "lat");
    }

    /**
     * 判断多个个经纬度点到由两个经纬度点组成的线的最短距离 是否 小于指定值 distance*
     *
     * @param curveLocationDTOList 线的点
     * @param objList              对象列表
     * @param distance             指定最大距离
     * @param lngField             经度的字段名
     * @param latField             纬度的字段名
     * @return
     */
    public static <T> List<T> calculateShortestDistanceFromCurve4Objs(List<LocationDTO> curveLocationDTOList, List<T> objList, String distance, String lngField, String latField) {

        List<LocationDTO> locationDTOList = convert2LocationDTOList(objList, lngField, latField);

        List<LocationDTO> resList = calculateShortestDistanceFromCurve4Points(curveLocationDTOList, locationDTOList, distance);

        if (CollectionUtil.isEmpty(resList)) {
            return Collections.emptyList();
        }

        List<T> tList = adaptLocationToObjects(resList, objList, lngField, latField);

        return tList;
    }


    /**
     * 将对象转换为LocationDTO*
     *
     * @param objectList 对象集合
     * @return
     */
    public static List<LocationDTO> convert2LocationDTOList(List<?> objectList) {
        return convert2LocationDTOList(objectList, "lng", "lat");
    }

    /**
     * 将对象转换为LocationDTO*
     *
     * @param obj 对象
     * @return
     */
    public static LocationDTO convert2LocationDTO(Object obj) {
        return convert2LocationDTOList(Arrays.asList(obj), "lng", "lat").get(0);
    }

    /**
     * 将对象转换为LocationDTO,并指定经纬度的字段*
     *
     * @param objectList 对象集合
     * @return
     */
    public static List<LocationDTO> convert2LocationDTOList(List<?> objectList, String lngField, String latField) {
        if (!StrUtil.isAllNotBlank(lngField, latField)) {
            return Collections.emptyList();
        }
        if (CollectionUtil.isEmpty(objectList)) {
            throw new RuntimeException("objectList is empty");
        }
        List<LocationDTO> locationDTOList = objectList.parallelStream().map(item -> {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(item);
                String lng = jsonObject.getStr(lngField);
                String lat = jsonObject.getStr(latField);
                if (StrUtil.isBlank(lng) || StrUtil.isBlank(lat)) {
                    return null;
                }
                return new LocationDTO(lng, lat);
            } catch (Exception e) {
                return null;
            }

        }).filter(item -> item instanceof LocationDTO).collect(Collectors.toList());
        return locationDTOList;
    }

    /**
     * 对传入的坐标集合、跟对象集合做一个适配*
     *
     * @param locationDTOList 坐标集合
     * @param objectList      对象集合
     * @return
     */
    public static <T> List<T> adaptLocationToObjects(List<LocationDTO> locationDTOList, List<T> objectList) {
        return adaptLocationToObjects(locationDTOList, objectList, "lng", "lat");
    }

    /**
     * 对传入的坐标集合、跟对象集合做一个适配*
     *
     * @param locationDTOList 坐标集合
     * @param objectList      对象集合
     * @return
     */
    public static <T> List<T> adaptLocationToObjects(List<LocationDTO> locationDTOList, List<T> objectList, String lngField, String latField) {

        if (!StrUtil.isAllNotBlank(lngField, latField)) {
            return Collections.emptyList();
        }

        adaptLocationToObjectsCheck(locationDTOList, objectList, lngField, latField);

        //转成hash
        Map<String, T> map = new HashMap<>();

        objectList.parallelStream().forEach(item -> {
            JSONObject jsonObject = JSONUtil.parseObj(item);
            map.put(jsonObject.getStr(lngField) + "_" + jsonObject.getStr(latField), item);
        });

        List<T> resList = locationDTOList.parallelStream().map(item -> {

            T t = map.get(item.getLng() + "_" + item.getLat());

            if (ObjectUtil.isNotEmpty(t)) {
                return t;
            }
            return null;
        }).filter(item -> item != null).collect(Collectors.toList());

        return resList;
    }

    //============================== 坐标系转换 =========================================

    private static final double x_PI = 3.14159265358979324 * 3000.0 / 180.0;
    private static final double PI = 3.1415926535897932384626;
    private static final double a = 6378245.0;
    private static final double ee = 0.00669342162296594323;

    /**
     * 百度坐标（BD09）转 GCJ02
     *
     * @param lng 百度经度
     * @param lat 百度纬度
     * @return GCJ02 坐标：[经度，纬度]
     */
    public static double[] transformBD09ToGCJ02(double lng, double lat) {
        double x = lng - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_PI);
        double gcj_lng = z * Math.cos(theta);
        double gcj_lat = z * Math.sin(theta);
        return new double[]{gcj_lng, gcj_lat};
    }

    /**
     * GCJ02 转百度坐标
     *
     * @param lng GCJ02 经度
     * @param lat GCJ02 纬度
     * @return 百度坐标：[经度，纬度]
     */
    public static double[] transformGCJ02ToBD09(double lng, double lat) {
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * x_PI);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * x_PI);
        double bd_lng = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return new double[]{bd_lng, bd_lat};
    }

    /**
     * GCJ02 转 WGS84
     *
     * @param lng 经度
     * @param lat 纬度
     * @return WGS84坐标：[经度，纬度]
     */
    public static double[] transformGCJ02ToWGS84(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        } else {
            double dLat = transformLat(lng - 105.0, lat - 35.0);
            double dLng = transformLng(lng - 105.0, lat - 35.0);
            double radLat = lat / 180.0 * PI;
            double magic = Math.sin(radLat);
            magic = 1 - ee * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * PI);
            dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * PI);
            double mgLat = lat + dLat;
            double mgLng = lng + dLng;
            return new double[]{lng * 2 - mgLng, lat * 2 - mgLat};
        }
    }

    /**
     * WGS84 坐标 转 GCJ02
     *
     * @param lng 经度
     * @param lat 纬度
     * @return GCJ02 坐标：[经度，纬度]
     */
    public static double[] transformWGS84ToGCJ02(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        } else {
            double dLat = transformLat(lng - 105.0, lat - 35.0);
            double dLng = transformLng(lng - 105.0, lat - 35.0);
            double redLat = lat / 180.0 * PI;
            double magic = Math.sin(redLat);
            magic = 1 - ee * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * PI);
            dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(redLat) * PI);
            double mgLat = lat + dLat;
            double mgLng = lng + dLng;
            return new double[]{mgLng, mgLat};
        }
    }

    /**
     * 百度坐标BD09 转 WGS84
     *
     * @param lng 经度
     * @param lat 纬度
     * @return WGS84 坐标：[经度，纬度]
     */
    public static double[] transformBD09ToWGS84(double lng, double lat) {
        double[] lngLat = transformBD09ToGCJ02(lng, lat);

        return transformGCJ02ToWGS84(lngLat[0], lngLat[1]);
    }

    /**
     * WGS84 转 百度坐标BD09
     *
     * @param lng 经度
     * @param lat 纬度
     * @return BD09 坐标：[经度，纬度]
     */
    public static double[] transformWGS84ToBD09(double lng, double lat) {
        double[] lngLat = transformWGS84ToGCJ02(lng, lat);

        return transformGCJ02ToBD09(lngLat[0], lngLat[1]);
    }


    private static double transformLat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    ;

    private static double transformLng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    ;

    /**
     * 判断坐标是否不在国内
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 坐标是否在国内
     */
    public static boolean outOfChina(double lng, double lat) {
        return (lng < 72.004 || lng > 137.8347) || (lat < 0.8293 || lat > 55.8271);
    }

    //=========================== 坐标系转换 end ===========================================

}
