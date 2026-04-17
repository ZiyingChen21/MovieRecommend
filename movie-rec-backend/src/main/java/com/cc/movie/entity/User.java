package com.cc.movie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "User")
public class User {
    @Id
    @JsonIgnore
    private String _id;

    @Indexed(unique = true)
    private int uid;

    @Indexed(unique = true)
    private String username;

    private String password;

    private boolean first;

    private long timestamp;

    private List<String> prefGenres = new ArrayList<>();

    private String avatar;
}
