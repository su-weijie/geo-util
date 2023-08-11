package cn.swj.geo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Author swj
 * @Date 2023/8/11 10:38
 * @Description: TODO
 * @Version 1.0
 */
@Slf4j
@SpringBootApplication
public class GeoApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(GeoApplication.class, args);
        Environment env = applicationContext.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String path = env.getProperty("server.servlet.context-path").trim();
        log.info("\n------------------------------------------------------------------\n\t" +
                "Application geoUtil is running! Access URLs:\n\t" +
                "Local: \t \t \t http://localhost:" + port + path + "/\n\t" +
                "External: \t \t http://" + ip + ":" + port + path + "/\n\t" +
                "Swagger文档: \t http://" + ip + ":" + port + path + "/doc.html\n" +
                "------------------------------------------------------------------");
    }

}
