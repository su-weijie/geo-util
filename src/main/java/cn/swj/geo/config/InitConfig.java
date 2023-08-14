package cn.swj.geo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @Author swj
 * @Date 2023/8/11 10:53
 * @Description: TODO
 * @Version 1.0
 */
@ApiIgnore
@Controller
public class InitConfig {

    /**
     * 访问根目录定位到doc.html*
     * @return
     */
    @GetMapping("/")
    public String redirectToKnife4j() {
        return "forward:/doc.html"; // 转发到Knife4j的页面
    }

}
