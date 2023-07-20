package cn.swj.geo.util;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.swj.geo.dto.LocationDTO;
import cn.swj.geo.dto.RoundnessDTO;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.*;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import java.util.ArrayList;
import java.util.List;

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
    public static LocationDTO getMinDistanceLocationDTO(LineString lineString, Point targetPoint) {
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

        if (!StrUtil.isAllNotEmpty(x, y)) {
            throw new RuntimeException("x or y is empty");
        }
    }

    //check params
    private static void pointListIsContainedRegionCheck(List<LocationDTO> regionLocationDTOList, List<LocationDTO> locationDTOList) {
        if (CollectionUtil.isEmpty(regionLocationDTOList)) {
            throw new RuntimeException("regionLocationDTOList is empty");
        }

        regionLocationDTOList.stream().forEach(item -> item.check());

        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }

        locationDTOList.stream().forEach(item -> item.check());
    }

    //check params
    private static void pointIsContainedRoundRegionCheck(RoundnessDTO roundnessDTO, String x, String y) {

        if (ObjectUtil.isEmpty(roundnessDTO)) {
            throw new RuntimeException("roundnessDTO is null");
        }

        roundnessDTO.check();

        if (!StrUtil.isAllNotEmpty(x, y)) {
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
    private static void calculateVerticalDistanceFromLineCheck(List<LocationDTO> locationDTOList, String x, String y) {
        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("locationDTOList is empty");
        }

        if (!StrUtil.isAllNotEmpty(x, y)) {
            throw new RuntimeException("x or y is empty");
        }

        if (locationDTOList.size() != 2) {
            throw new RuntimeException("locationDTOList size is not two");
        }

        locationDTOList.forEach(item -> item.check());
    }

    //check params
    private static void calculateVerticalDistanceFromLineCheck(List<LocationDTO> locationDTOList, String x, String y, String distance) {
        calculateVerticalDistanceFromLineCheck(locationDTOList, x, y);

        if (StrUtil.isEmpty(distance)) {
            throw new RuntimeException("distance is empty");
        }

        if (!StrUtil.isAllNotEmpty(x, y)) {
            throw new RuntimeException("x or y is empty");
        }
    }


    //check params
    private static void calculateVerticalDistanceFromLine4PointsCheck(List<LocationDTO> lineLocationDTOList, List<LocationDTO> locationDTOList, String distance) {
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
    private static void calculateVerticalDistanceFromCurveCheck(List<LocationDTO> curveLocationDTOList, String x, String y) {
        if (CollectionUtil.isEmpty(curveLocationDTOList)) {
            throw new RuntimeException("curveLocationDTOList is empty");
        }

        if (curveLocationDTOList.size() <= 2) {
            throw new RuntimeException("curveLocationDTOList size must ge 3");
        }

        if (!StrUtil.isAllNotEmpty(x, y)) {
            throw new RuntimeException("x or y is empty");
        }
    }

    //check params
    private static void calculateVerticalDistanceFromCurveCheck(List<LocationDTO> curveLocationDTOList, String x, String y, String distance) {
        calculateVerticalDistanceFromCurveCheck(curveLocationDTOList, x, y);
        if (StrUtil.isEmpty(distance)) {
            throw new RuntimeException("distance is empty");
        }

        if (Double.valueOf(distance) <= 0) {
            throw new RuntimeException("distance is le 0");
        }
    }

    //checkParams
    private static void calculateVerticalDistanceFromCurve4PointsCheck(List<LocationDTO> curveLocationDTOList, List<LocationDTO> locationDTOList, String distance) {
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
     * 按照 locationVOList的点位顺序组成区域*
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
     * 计算俩个点之间的距离*
     *
     * @param startLocation 开始坐标
     * @param endLocation   结束坐标
     * @return
     */
    public static String calculateDistance(LocationDTO startLocation, LocationDTO endLocation) {

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
     * 计算一个经纬度点到由两个经纬度点组成的线的最短距离*
     *
     * @param lineLocationList 组成线得俩个点位
     * @param x                x
     * @param y                y
     * @return
     */
    public static String calculateVerticalDistanceFromLine(List<LocationDTO> lineLocationList, String x, String y) {

        calculateVerticalDistanceFromLineCheck(lineLocationList, x, y);

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
    public static boolean calculateVerticalDistanceFromLine(List<LocationDTO> lineLocationList, String x, String y, String distance) {

        calculateVerticalDistanceFromLineCheck(lineLocationList, x, y, distance);

        String shortestDistance = calculateVerticalDistanceFromLine(lineLocationList, x, y);

        return Double.valueOf(shortestDistance) < Double.valueOf(distance);
    }

    /**
     * 判断多个个经纬度点到由两个经纬度点组成的线的垂直距离 是否 小于指定值 distance*
     *
     * @param lineLocationDTOList 线的点
     * @param locationDTOList     点位列表
     * @param distance            指定最大距离
     * @return
     */
    public static List<LocationDTO> calculateVerticalDistanceFromLine4Points(List<LocationDTO> lineLocationDTOList, List<LocationDTO> locationDTOList, String distance) {

        calculateVerticalDistanceFromLine4PointsCheck(lineLocationDTOList, locationDTOList, distance);

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
     * 计算一个经纬度点到由多个个经纬度点组成的曲线线的最短距离*
     *
     * @param curveLocationDTOList
     * @param x
     * @param y
     * @return
     */
    public static String calculateVerticalDistanceFromCurve(List<LocationDTO> curveLocationDTOList, String x, String y) {

        calculateVerticalDistanceFromCurveCheck(curveLocationDTOList, x, y);

        // 创建 Coordinate 数组并添加经纬度点坐标
        LineString lineString = createLineString(curveLocationDTOList);

        // 创建 Point 对象
        Point point = geometryFactory.createPoint(createCoordinate(x, y));


        LocationDTO minDistanceLocationDTO = getMinDistanceLocationDTO(lineString, point);

        String distance = calculateDistance(new LocationDTO(x, y), minDistanceLocationDTO);

        return distance;
    }

    /**
     * 判断一个经纬度点到由两个经纬度点组成的线的垂直距离 是否 小于指定值 distance*
     *
     * @param locationDTOList
     * @param x
     * @param y
     * @param distance        指定最大距离
     * @return
     */
    public static boolean calculateVerticalDistanceFromCurve(List<LocationDTO> locationDTOList, String x, String y, String distance) {

        calculateVerticalDistanceFromCurveCheck(locationDTOList, x, y, distance);

        String oDistance = calculateVerticalDistanceFromCurve(locationDTOList, x, y);

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
    public static List<LocationDTO> calculateVerticalDistanceFromCurve4Points(List<LocationDTO> curveLocationDTOList, List<LocationDTO> locationDTOList, String distance) {

        calculateVerticalDistanceFromCurve4PointsCheck(curveLocationDTOList, locationDTOList, distance);

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

}
