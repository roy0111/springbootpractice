package com.learn.restapi.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Aggregated DTO combining a {@link Post} with its {@link PostComment} list.
 *
 * <p>Built by zipping two parallel reactive streams:
 * <pre>
 *   Mono.zip(
 *       getPostById(id),          // Mono&lt;Post&gt;
 *       getCommentsForPost(id)    // Mono&lt;List&lt;PostComment&gt;&gt;
 *   ).map(tuple -> new PostWithComments(tuple.getT1(), tuple.getT2()))
 * </pre>
 *
 * <p>Both API calls are made <b>concurrently</b> — {@code Mono.zip} subscribes
 * to both publishers at the same time, so latency ≈ max(t_post, t_comments)
 * rather than t_post + t_comments.
 */
@Schema(description = "Post along with all its comments — fetched concurrently via Mono.zip")
public class PostWithComments {

    @Schema(description = "The post details")
    private Post post;

    @Schema(description = "All comments on the post")
    private List<PostComment> comments;

    @Schema(description = "Total number of comments", example = "5")
    private int commentCount;

    // ── Constructors ─────────────────────────────────────────────────────────

    public PostWithComments() {}

    public PostWithComments(Post post, List<PostComment> comments) {
        this.post = post;
        this.comments = comments;
        this.commentCount = comments != null ? comments.size() : 0;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Post getPost()              { return post; }
    public void setPost(Post post)     { this.post = post; }

    public List<PostComment> getComments()                   { return comments; }
    public void setComments(List<PostComment> comments)      { this.comments = comments; }

    public int getCommentCount()                             { return commentCount; }
    public void setCommentCount(int commentCount)            { this.commentCount = commentCount; }
}
