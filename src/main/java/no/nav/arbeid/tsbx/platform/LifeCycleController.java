package no.nav.arbeid.tsbx.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class LifeCycleController {

    private final Logger log = LoggerFactory.getLogger(LifeCycleController.class);

    @GetMapping("/isalive")
    public ResponseEntity<String> isAlive() {
        return ResponseEntity.ok("alive");
    }

    @GetMapping("/isready")
    public ResponseEntity<String> isReady() {
        return ResponseEntity.ok("ready");
    }

    @GetMapping("/stop")
    public ResponseEntity<String> stop() throws Exception {
        log.info("Graceful shutdown requested by platform, delaying 10 seconds ..");
        Thread.sleep(10000);
        return ResponseEntity.ok("shutdown");
    }

}
