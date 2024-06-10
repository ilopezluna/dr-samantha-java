package org.example;

import io.restassured.http.Header;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;

public class DrSamanthaContainerTest {

    @Test
    public void drSamantha() {

        try (OllamaContainer ollama = new OllamaContainer(DockerImageName.parse("dr-samantha").asCompatibleSubstituteFor("ollama/ollama:0.1.42"))) {
            try {
                ollama.start();
            } catch (ContainerFetchException ex) {
                createImage();
                ollama.start();
            }

            var system = """
                    You are Doctor Samantha, a virtual AI doctor known for your friendly and approachable demeanor,
                    combined with a deep expertise in the medical field.

                    You're here to provide professional, empathetic, and knowledgeable advice on health-related inquiries.
                    At any given point, you'll only talk like a human and your name is Samantha. I repeat, talk like human
                    who is empathetic and deeply knowledgeable in the medical field.

                    If you're unsure about any information, Don't share false information.
                    """;

            var prompt = """
                    Symptoms:
                    Dizziness, headache, and nausea.
                    
                    What is the differential diagnosis?
                    """;

            CompletionResponse response = given()
                    .baseUri(ollama.getEndpoint())
                    .header(new Header("Content-Type", "application/json"))
                    .body(new CompletionRequest("Dr:latest", prompt, system, false))
                    .post("/api/generate")
                    .getBody().as(CompletionResponse.class);

            System.out.println(response.response());
        }
    }

    public void createImage() {
        var newImageName = "dr-samantha";
        var model = new OllamaContainer.HuggingFaceModel("mradermacher/Dr.Samantha-8B-i1-GGUF", "Dr.Samantha-8B.Q4_K_M.gguf");
        try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.42").withHuggingFaceModel(model)) {
            ollama.start();

            String modelName = given()
                    .baseUri(ollama.getEndpoint())
                    .get("/api/tags")
                    .jsonPath()
                    .getString("models[0].name");
            Assert.assertEquals("Dr:latest", modelName);
            ollama.commitToImage(newImageName);
        }
    }

    record CompletionRequest(String model, String prompt, String system, boolean stream) {
    }

    record CompletionResponse(String response) {
    }

}
