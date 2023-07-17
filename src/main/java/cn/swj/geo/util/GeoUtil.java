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

import javax.annotation.PostConstruct;
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
     * @param roundnessDTO
     * @return
     */
    private static Geometry createCircle(RoundnessDTO roundnessDTO) {
        // 圆心坐标
        double centerLon = Double.valueOf(roundnessDTO.getCenterPoint().getLng()); // 圆心经度
        double centerLat = Double.valueOf(roundnessDTO.getCenterPoint().getLat()); // 圆心纬度

        // 圆的半径（以米为单位）
        double radius = Double.valueOf(roundnessDTO.getRadius());


        // 创建圆心点
        Coordinate centerCoordinate = new Coordinate(centerLon, centerLat);
        Point centerPoint = geometryFactory.createPoint(centerCoordinate);

        // 创建圆形区域
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        GeodeticCalculator calculator = new GeodeticCalculator(crs);
        calculator.setStartingGeographicPoint(centerLon, centerLat);
        calculator.setDirection(0.0, radius);
        DirectPosition edgePos = null;
        try {
            edgePos = calculator.getDestinationPosition();
        } catch (TransformException e) {
            throw new  RuntimeException("计算地图中真正对应的半径失败");
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
            polygon= geometryFactory.createPolygon(pointCoordinates);
        }
        if(ObjectUtil.isEmpty(polygon)) {
            throw new RuntimeException("polygon is empty");
        }

        return polygon;
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

    //-------------------------------------------------------------------------------------------------------------

    /**
     * 按照 locationVOList的点位顺序 组成区域,在判断xy这个坐标是否包含在里面*
     *
     * @param locationDTOList 几何图形组成的点位
     * @param x               经度
     * @param y               纬度
     * @return
     */
    public static boolean pointIsContainedRegionSequence(List<LocationDTO> locationDTOList, String x, String y) {

        pointIsContainedRegionCheck(locationDTOList, x, y);

        //几何图形
        Polygon polygon = getPolygon(locationDTOList, false);

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
     * @param locationDTOList 几何图形组成的点位
     * @param x               经度
     * @param y               纬度
     * @return
     */
    public static boolean pointIsContainedRegion(List<LocationDTO> locationDTOList, String x, String y) {

        pointIsContainedRegionCheck(locationDTOList, x, y);

        //几何图形
        Polygon polygon = getPolygon(locationDTOList, true);

        //create point
        Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf(x), Double.valueOf(y)));

        return polygon.contains(point);
    }

    /**
     * 判断point 是否在 locationVOList这个区域中，自动取最大几何图形*
     *
     * @param locationDTOList 几何图形组成的点位
     * @param point           点位
     * @return
     */
    @Deprecated
    public static boolean pointIsContainedRegion(List<LocationDTO> locationDTOList, Point point) {

        if (CollectionUtil.isEmpty(locationDTOList)) {
            throw new RuntimeException("list is null");
        }

        Polygon polygon = getPolygon(locationDTOList, true);

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
     * @param locationDTOList        待判断的点位
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
            if(ObjectUtil.isEmpty(locationDTO)) {
                continue;
            }
            Point point = geometryFactory.createPoint(new Coordinate(Double.valueOf(locationDTO.getLng()), Double.valueOf(locationDTO.getLat())));
            if(circle.contains(point)) {
                resList.add(locationDTO);
            }
        }

        return resList;
    }

    /**
     * 计算俩个点之间的距离*
     *
     * @param startLocation
     * @param endLocation
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
        calculator.setDestinationGeographicPoint(Double.valueOf(endLocation.getLng()),  Double.valueOf(endLocation.getLat()));

        // 计算直线距离
        double distance = calculator.getOrthodromicDistance();

        return String.valueOf(distance);
    }

}
