package com.techcourse.service;

import com.interface21.dao.DataAccessException;
import com.interface21.jdbc.core.JdbcTemplate;
import com.techcourse.dao.UserHistoryDaoImpl;
import com.techcourse.domain.UserHistory;
import java.sql.Connection;

public class MockUserHistoryDao extends UserHistoryDaoImpl {

    public MockUserHistoryDao(final JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void log(final Connection conn, final UserHistory userHistory) {
        throw new DataAccessException();
    }
}
