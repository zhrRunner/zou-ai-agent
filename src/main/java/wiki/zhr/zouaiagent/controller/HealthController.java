package wiki.zhr.zouaiagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName HealthController
 * @Description TODO
 * @Author hrz
 * @Date 2025/6/29 11:15
 **/
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public String health() {
        return "ok";
    }
}
