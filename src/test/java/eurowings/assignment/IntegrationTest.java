package eurowings.assignment;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.net.URLEncoder;
import java.time.OffsetDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@Disabled
public class IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void testApiCall() throws Exception {
        String flightNumber = "EW 4711";
        String scheduledDeparture = "2026-07-21T18:35:00+02:00";
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/flights/stream-alternatives/" + flightNumber)
                .param("scheduledDeparture", scheduledDeparture)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(MockMvcResultMatchers.jsonPath("$.flight").value(CoreMatchers.containsString(flightNumber)));
    }

}
