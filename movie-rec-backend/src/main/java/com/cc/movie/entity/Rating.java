package com.cc.movie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Rating")
public class Rating {
    @JsonIgnore
    private String _id;

    private int uid;

    private int mid;

    private double score;

    private long timestamp;
}
