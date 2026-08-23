package com.learn.restapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for a Comment on a Post from JSONPlaceholder API.
 *
 * <p>Maps to: GET https://jsonplaceholder.typicode.com/posts/{id}/comments
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Comment fetched from JSONPlaceholder external API")
public class PostComment {

    @Schema(description = "ID of the post this comment belongs to", example = "1")
    private int postId;

    @Schema(description = "Comment ID", example = "1")
    private int id;

    @Schema(description = "Name of the commenter", example = "id labore ex et quam laborum")
    private String name;

    @Schema(description = "Email of the commenter", example = "Eliseo@gardner.biz")
    private String email;

    @Schema(description = "Comment body", example = "laudantium enim quasi est...")
    private String body;

    // ── Constructors ─────────────────────────────────────────────────────────

    public PostComment() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getPostId()  { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getId()      { return id; }
    public void setId(int id) { this.id = id; }

    public String getName()  { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBody()  { return body; }
    public void setBody(String body) { this.body = body; }
}
