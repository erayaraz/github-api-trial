package com.github.api.arazgithubapi.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class GithubReadController {

    private final String apiToken;

    public GithubReadController(@Value("${github.api.token}") String apiToken) {
        this.apiToken = apiToken;
    }

    @GetMapping("/getGithubUserInfo")
    public String getGithubUserInfo() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.github.com/user")
                .addHeader("Authorization", "token " + apiToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return responseBody;
        }
    }
    @GetMapping("/getGithubReposInfo")
    public List<String> getGithubReposInfo() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.github.com/user/repos")
                .addHeader("Authorization", "token " + apiToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> repositories = objectMapper.readValue(responseBody,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            List<String> repoNames = new ArrayList<>();
            for (Map<String, Object> repository : repositories) {
                String repoName = (String) repository.get("name");
                repoNames.add(repoName);
            }
            return repoNames;
        }
    }

    @GetMapping("/getTotalContributions/{username}")
    public int getTotalContributions(@PathVariable("username") String username) throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.github.com/users/"+username+"/repos")
                .addHeader("Authorization", "token " + apiToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JSONArray repositories = new JSONArray(responseBody);

            int totalContributions = 0;
            for (int i = 0; i < repositories.length(); i++) {
                JSONObject repository = repositories.getJSONObject(i);
                String repoFullName = repository.getString("full_name");
                totalContributions += getRepoContributions(repoFullName,username);
            }

            return totalContributions;
        }
    }

    private int getRepoContributions(String repoFullName,String username) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + repoFullName + "/contributors")
                .addHeader("Authorization", "token " + apiToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JSONArray contributors = new JSONArray(responseBody);

            for (int i = 0; i < contributors.length(); i++) {
                JSONObject contributor = contributors.getJSONObject(i);
                String contributorLogin = contributor.getString("login");
                if (contributorLogin.equals(username)) {
                    return contributor.getInt("contributions");
                }
            }
        }

        return 0;
    }


}