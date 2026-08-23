package com.learn.restapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity model representing a Tag used for categorizing and labeling entities.
 */
@Table("tags")
@Schema(description = "Tag entity for tagging products or domain items")
public class Tag {

    @Id
    @Schema(description = "Unique identifier of the tag", example = "1")
    private Long id;

    @Schema(description = "Name of the tag", example = "ELECTRONICS")
    private String name;

    @Schema(description = "Color code for UI rendering", example = "#FF5733")
    private String color;

    public Tag() {}

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Tag(Long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "Tag{id=" + id + ", name='" + name + "', color='" + color + "'}";
    }
}
