package edu.rit.swen755.auth.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one service anyone may call without credentials.
 */
@RestController
public class PublicController {

    /**
     * The JSON body of {@code GET /api/public/info}.
     *
     * @param clinic the clinic name
     * @param status whether the clinic is open
     */
    public record Info(String clinic, String status) {
    }

    /**
     * {@code GET /api/public/info}.
     *
     * @return {@code {"clinic":"SWEN-755 Clinic","status":"open"}}
     */
    @GetMapping("/api/public/info")
    public Info info() {
        return new Info("SWEN-755 Clinic", "open");
    }
}
