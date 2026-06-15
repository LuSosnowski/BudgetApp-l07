package pk.ls.pasir.sosnowski_lukasz.controller;

import pk.ls.pasir.sosnowski_lukasz.info.InfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    @GetMapping("/api/info")
    public InfoResponse getInfo() {
        return new InfoResponse(
                "Aplikacja Budżetowa",
                "1.0",
                "Witaj w aplikacji budżetowej stworzonej ze Spring Boot!"
        );
    }
}