package com.junior.api;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Junior Project 2: REST API tests with JDK HttpClient + TestNG.
 * Demo API: https://jsonplaceholder.typicode.com (free fake REST API).
 * Run: mvn test
 */
public class UsersApiTest {

    private HttpClient client;
    private static final String BASE = "https://jsonplaceholder.typicode.com";

    @BeforeClass
    public void setUp() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void getUsersReturns200WithExpectedFields() throws Exception {
        HttpResponse<String> res = get("/users");
        Assert.assertEquals(res.statusCode(), 200);
        String body = res.body();
        Assert.assertTrue(body.contains("Leanne Graham"), "Missing expected user. Body: " + body.substring(0, 200));
        Assert.assertTrue(body.contains("email"), "Response has no email field");
    }

    @Test
    public void getSinglePostReturnsCorrectUserId() throws Exception {
        HttpResponse<String> res = get("/posts/1");
        Assert.assertEquals(res.statusCode(), 200);
        String body = res.body();
        Assert.assertTrue(body.contains("\"userId\": 1") || body.contains("\"userId\":1"),
                "post/1 should belong to userId 1. Body: " + body);
        Assert.assertTrue(body.contains("title"), "Post has no title");
    }

    @Test
    public void createPostReturns201() throws Exception {
        String json = "{\"title\":\"junior test\",\"body\":\"hello\",\"userId\":1}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/posts"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(res.statusCode(), 201, "Body: " + res.body());
        Assert.assertTrue(res.body().contains("junior test"));
    }
}
