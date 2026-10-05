package com.example.agentscope2.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.agentscope2.config.CourseModelFactory;
import com.example.agentscope2.config.CourseModelSettings;
import com.example.agentscope2.tool.CustomerTicketTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentScope2AgentFactoryTest {

    @Test
    void createsReactAndHarnessAgents() {
        CourseModelSettings settings = CourseModelSettings.from(Map.of("AI_PROVIDER", "ollama"));
        Model model = CourseModelFactory.create(settings);
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new CustomerTicketTools());

        ReActAgent reactAgent = AgentScope2AgentFactory.createReActAgent(model, toolkit);
        HarnessAgent harnessAgent = AgentScope2AgentFactory.createHarnessAgent(
                model,
                Path.of("target", "test-workspace"),
                Path.of(".").toAbsolutePath().normalize());

        assertEquals("course-react-agent", reactAgent.getName());
        assertEquals("course-harness-agent", harnessAgent.getName());
        assertNotNull(reactAgent.getToolkit().getTool("query_ticket_summary"));
    }
}
