/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/license/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software     
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel;

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Map;
import java.util.HashMap;

import java.io.IOException;

public class FedoraPathTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testPath() throws Exception {
        final String path = "/test/a/b/c/d";

        // Assertions
        resultEndpoint.expectedMessageCount(3);

        // Setup
        Map<String, Object> setupHeaders = new HashMap<String, Object>();
        setupHeaders.put(Exchange.HTTP_METHOD, "PUT");
        setupHeaders.put("FCREPO_IDENTIFIER", path);
        setupHeaders.put(Exchange.CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeaders("direct:setup", FedoraTestUtils.getTurtleDocument(), setupHeaders);
 
        // Test
        template.sendBodyAndHeader(null, "org.fcrepo.jms.identifier", path);
        template.sendBodyAndHeader(null, "FCREPO_IDENTIFIER", path);
        template.sendBody("direct:start2", null);

        // Teardown
        Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put("FCREPO_IDENTIFIER", path);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that assertions passed
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {

                String fcrepo_uri = FedoraTestUtils.getFcrepoEndpointUri();

                Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

                from("direct:setup")
                    .to(fcrepo_uri);
                
                from("direct:start")
                    .to(fcrepo_uri)
                    .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#Resource']", ns)
                    .to("mock:result");

                from("direct:start2")
                    .to(fcrepo_uri + "/test/a/b/c/d")
                    .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#Resource']", ns)
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
