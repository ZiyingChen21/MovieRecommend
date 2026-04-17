package com.cc.movie.entity.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private List<T> list;   // 当前页的数据列表
    private long total;     // 满足条件的总记录数
}