package com.cc.movie.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "AverageRating")
public class AverageRating {
    @Id
    private String _id;
    
    private Integer mid;
    
    private Double average;
}