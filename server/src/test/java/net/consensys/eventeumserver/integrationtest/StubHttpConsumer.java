package net.consensys.eventeumserver.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import net.consensys.eventeum.dto.block.BlockDetails;
import net.consensys.eventeum.dto.event.ContractEventDetails;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class StubHttpConsumer {

    private ObjectMapper objectMapper = new ObjectMapper();

    private WireMockServer wireMockServer;

    private HttpStatus responseStatus;

    public StubHttpConsumer(HttpStatus responseStatus) {
        this.responseStatus = responseStatus;

        wireMockServer = new WireMockServer(wireMockConfig().port(8082));
    }

    public StubHttpConsumer() {
        this(HttpStatus.OK);
    }
    public void start(List<ContractEventDetails> broadcastContractEvents,
                      List<BlockDetails> broadcastBlocks) {
        wireMockServer.start();

        wireMockServer.addStubMapping(post(urlPathEqualTo("/consumer/block-event"))
                .willReturn(aResponse()
                        .withStatus(responseStatus.value())).build());

        wireMockServer.addStubMapping(post(urlPathEqualTo("/consumer/contract-event"))
                .willReturn(aResponse()
                        .withStatus(responseStatus.value())).build());

        wireMockServer.addMockServiceRequestListener((request, response) -> {
            if (request.getUrl().contains("/consumer/contract-event")) {
                final String body = request.getBodyAsString();

                try {
                    broadcastContractEvents.add(objectMapper.readValue(body, ContractEventDetails.class));
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });

        wireMockServer.addMockServiceRequestListener((request, response) -> {
            if (request.getUrl().contains("/consumer/block-event")) {
                final String body = request.getBodyAsString();

                try {
                    broadcastBlocks.add(objectMapper.readValue(body, BlockDetails.class));
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() {
        wireMockServer.stop();
    }

    public String getLatestRequestBody() {
        List<ServeEvent> events = wireMockServer.getAllServeEvents();

        return events.get(events.size() - 1).getRequest().getBodyAsString();
    }
}
