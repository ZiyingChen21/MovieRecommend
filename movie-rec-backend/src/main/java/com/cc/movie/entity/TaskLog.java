package com.cc.movie.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "TaskLog")
public class TaskLog {
    @Id
    private String taskName; // "DAILY_REC" 或 "MONTHLY_PARAM"
    private Date lastSuccessTime;
}