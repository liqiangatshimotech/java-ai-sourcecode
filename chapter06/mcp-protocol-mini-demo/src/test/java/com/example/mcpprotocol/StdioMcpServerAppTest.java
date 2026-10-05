package com.example.mcpprotocol;

import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StdioMcpServerAppTest {
    @Test void writesOnlyResponsesAndIgnoresNotification() throws Exception {
        String requests = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}\n"
                + "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n"
                + "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}\n";
        StringWriter out = new StringWriter();
        StdioMcpServerApp.serve(new StringReader(requests), out);
        assertEquals(2, out.toString().lines().count());
        for (String line : out.toString().lines().toList()) assertTrue(line.startsWith("{\"jsonrpc\":\"2.0\""));
        assertTrue(out.toString().contains("query_order_status"));
    }

    @Test void malformedRequestIsOneJsonErrorAndNextRequestStillWorks() throws Exception {
        StringWriter out = new StringWriter();
        StdioMcpServerApp.serve(new StringReader("not-json\n{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}"), out);
        assertEquals(2, out.toString().lines().count());
        assertTrue(out.toString().contains("-32700"));
        assertTrue(out.toString().contains("query_order_status"));
    }
}
