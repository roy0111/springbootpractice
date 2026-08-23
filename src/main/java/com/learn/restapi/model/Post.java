package com.learn.restapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for a Post from JSONPlaceholder API.
 *
 * <p>Maps to: GET https://jsonplaceholder.typicode.com/posts
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} — if the external API
 * adds new fields in the future, Jackson will silently ignore them instead of
 * throwing a deserialization error.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Post fetched from JSONPlaceholder external API")
public class Post {

    @Schema(description = "ID of the user who authored this post", example = "1")
    private int userId;

    @Schema(description = "Post ID", example = "1")
    private int id;

    @Schema(description = "Post title", example = "sunt aut facere repellat provident")
    private String title;

    @Schema(description = "Post body / content", example = "quia et suscipit...")
    private String body;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Post() {}

    public Post(int userId, int id, String title, String body) {
        this.userId = userId;
        this.id = id;
        this.title = title;
        this.body = body;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getUserId()  { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getId()      { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle()  { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody()   { return body; }
    public void setBody(String body) { this.body = body; }

    @Override
    public String toString() {
        return "Post{id=" + id + ", userId=" + userId + ", title='" + title + "'}";
    }
}
