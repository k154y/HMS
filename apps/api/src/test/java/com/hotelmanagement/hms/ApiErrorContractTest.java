package com.hotelmanagement.hms;

import com.hotelmanagement.hms.shared.web.*;
import com.hotelmanagement.hms.reservation.dto.ReservationRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class ApiErrorContractTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Errors())
            .setControllerAdvice(new GlobalApiExceptionHandler()).addFilters(new RequestCorrelationFilter()).build();
    @RestController static class Errors {
        @PostMapping("/validation") void validate(@Valid @RequestBody ReservationRequest body) {}
        @GetMapping("/error/{kind}") void fail(@PathVariable String kind) throws Exception {
            switch(kind) {
                case "business" -> throw new ApiException(409,"ROOM_UNAVAILABLE","This room is no longer available for the selected dates.");
                case "auth" -> throw new BadCredentialsException("private credential detail");
                case "permission" -> throw new AccessDeniedException("private role detail");
                case "missing" -> throw ApiException.notFound();
                case "resource" -> throw new NoResourceFoundException(HttpMethod.GET,"internal-resource-path","/error/resource");
                case "state" -> throw new IllegalStateException("SQL secret table constraint password=NEVER_EXPOSE");
                case "input" -> throw new IllegalArgumentException("internal column/path password=NEVER_EXPOSE");
                default -> throw new RuntimeException("SQL password=NEVER_EXPOSE token=NEVER_EXPOSE account=NEVER_EXPOSE /private/file");
            }
        }
    }
    @Test void requiredFieldsHaveSafeMessages() throws Exception {
        var response=mvc.perform(post("/validation").contentType("application/json").content("{\"adults\":1,\"children\":0,\"rooms\":[]}")).andReturn().getResponse();
        assertEquals(400,response.getStatus());
        var body=json.readTree(response.getContentAsString());
        assertEquals("VALIDATION_FAILED",body.path("code").asString());
        assertTrue(body.path("fieldErrors").toString().contains("Customer is required."),response.getContentAsString());
        assertTrue(body.path("fieldErrors").toString().contains("Check-in date is required."));
        assertTrue(body.path("fieldErrors").toString().contains("Check-out date is required."));
    }
    @Test void statusesAndSafeMessagesAreStable() throws Exception {
        for(var item:java.util.Map.of("business",409,"auth",401,"permission",403,"missing",404,"resource",404,"state",500,"input",400).entrySet()) {
            var response=mvc.perform(get("/error/"+item.getKey())).andReturn().getResponse();
            assertEquals(item.getValue(),response.getStatus());
            assertFalse(response.getContentAsString().contains("NEVER_EXPOSE"));
            assertFalse(response.getContentAsString().contains("private credential"));
            assertFalse(response.getContentAsString().contains("internal-resource-path"));
            assertEquals(response.getHeader("X-Request-ID"),json.readTree(response.getContentAsString()).path("requestId").asString());
        }
        assertTrue(mvc.perform(get("/error/business")).andReturn().getResponse().getContentAsString().contains("no longer available"));
    }
    @Test void unexpectedFaultIsCorrelatedWithoutLeakingSecretsToResponseOrLogs() throws Exception {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(GlobalApiExceptionHandler.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try {
            var response=mvc.perform(get("/error/fault").header("X-Request-ID","support-reference-123")).andReturn().getResponse();
            assertEquals(500,response.getStatus());
            var body=json.readTree(response.getContentAsString());
            assertEquals("INTERNAL_ERROR",body.path("code").asString());
            assertEquals("support-reference-123",body.path("requestId").asString());
            assertTrue(body.path("message").asString().contains("system problem"));
            assertTrue(body.path("message").asString().contains("support-reference-123"));
            assertFalse(response.getContentAsString().contains("NEVER_EXPOSE"));
            assertFalse(response.getContentAsString().contains("RuntimeException"));
            assertFalse(response.getContentAsString().contains("/private/file"));
            assertTrue(appender.list.stream().anyMatch(event->event.getFormattedMessage().contains("support-reference-123")));
            assertTrue(appender.list.stream().noneMatch(event->event.getFormattedMessage().contains("NEVER_EXPOSE") || event.getThrowableProxy()!=null));
        } finally { logger.detachAppender(appender);appender.stop(); }
    }
}
