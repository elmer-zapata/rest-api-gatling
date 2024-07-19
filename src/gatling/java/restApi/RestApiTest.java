package restApi;

import java.time.Duration;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class RestApiTest extends Simulation {
    // Define basic variables
    String baseUrl = System.getProperty("baseUrl", "https://api.restful-api.dev");
    String concurrentUsers = System.getProperty("concurrentUsers", "10");
    String contentHeader = "Content-Type";
    String applicationHeader = "application/json";
    // Define the data
    FeederBuilder.FileBased<Object> feeder = jsonFile("data/restapi.json").circular();
    // We want to create rest API with 10 objects
    //
    // Define the base URL and headers
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl);

    // Define the scenario
    ScenarioBuilder scn = scenario("Rest API Test")
            .feed(feeder)
            // Create a new object using a feeder
            .exec(http("Create Object")
                    .post("/objects")
                    .header(contentHeader, applicationHeader)
                    .body(StringBody(
                            """
                               {\"name\": \"#{name}\",
                               \"data\": {\"year\": "#{data.year}\",
                               \"price\": \"#{data.price}\",
                               \"model\": \"#{data.model}\"}}
                              """
                    )).asJson()
                    .check(jmesPath("id").find().saveAs("id"))
                    .check(bodyString().saveAs("body"))
                    .check(status().is(200))
            )

            .exec(
                    session -> {
                        System.out.println("Created Object Id: " + session.getString("id"));
                        System.out.println("Response Body: " + session.getString("body"));
                        return session;
                    }
            )

            // Update a created object and add a new attribute to the object
            .exec(http("Update Object")
                    .put("/objects/#{id}")
                    .header(contentHeader, applicationHeader)
                    .body(StringBody(
                            """
                               {\"name\": \"#{name}\",
                               \"data\": {\"year\": \"#{data.year}\",
                               \"price\": \"#{data.price}\",
                               \"model\": \"#{data.model}\",
                               \"color\": \"#{data.color}\"}}
                              """
                    )).asJson()
                    .check(bodyString().saveAs("body"))
                    .check(status().is(200))
            )

            .exec(
                    session -> {
                        System.out.println(" Updated Response Body: " + session.getString("body"));
                        return session;
                    }
            )

            // Get an object that was created previously and verify that the data is correct
            .exec(http("Get Object")
                    .get("/objects/#{id}")
                    .header(contentHeader, applicationHeader)
                    .check(bodyString().saveAs("body"))
                    .check(status().is(200))
                    .check(jmesPath("id").isEL("#{id}"))
                    .check(jmesPath("name").isEL("#{name}"))
                    .check(jmesPath("data.year").isEL("#{data.year}"))
                    .check(jmesPath("data.price").isEL("#{data.price}"))
                    .check(jmesPath("data.model").isEL("#{data.model}"))
                    .check(jmesPath("data.color").isEL("#{data.color}"))
            )

            .exec(
                    session -> {
                        System.out.println("Get Response Body: " + session.getString("body"));
                        return session;
                    }
            )

            // Delete the created object in order to clean up
            .exec(http("Delete Object")
                    .delete("/objects/#{id}")
                    .header(contentHeader, applicationHeader)
                    .check(bodyString().saveAs("body"))
                    .check(status().is(200))
            )

            .exec(
                    session -> {
                        System.out.println("Deleted Response Body: " + session.getString("body"));
                        return session;
                    }
            )

            // Get a deleted object and verify that 404 status code is returned.
            .exec(http("Get Deleted Object")
                    .get("/objects/#{id}")
                    .header(contentHeader, applicationHeader)
                    .check(bodyString().saveAs("body"))
                    .check(status().is(404))
            )

            .exec(
                    session -> {
                        System.out.println("Get Deleted Response Body: " + session.getString("body"));
                        return session;
                    }
            );
    // Set up the scenario

    {
        setUp(
                scn.injectClosed(
                        constantConcurrentUsers(Integer.parseInt(concurrentUsers)).during(Duration.ofSeconds(10)
                        )
                )
        ).protocols(httpProtocol);
    }
}
