package tw.com.tymgateway.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the gateway's own public BPMN overview (docs/BUSINESS_PROCESS_BPMN.md). */
@RestController
public class GatewayBpmnController {
    @GetMapping(value = "/tymg/api/gateway.bpmn", produces = "application/xml")
    public Resource gatewayProcesses() {
        return new ClassPathResource("gateway.bpmn");
    }
}
