package com.interface21.jdbc.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interface21.dao.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LegacyJdbcTemplateTest {

    private Connection conn;
    private PreparedStatement pstmt;
    private ResultSet rs;
    private LegacyJdbcTemplate legacyJdbcTemplate;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        conn = mock(Connection.class);
        pstmt = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(any())).thenReturn(pstmt);

        legacyJdbcTemplate = new LegacyJdbcTemplate(dataSource);
    }

    @Test
    void 데이터_생성_성공() {
        TestUser user = new TestUser("jojo", "1234");
        String sql = "insert into test-user (account, password) values (?, ?)";

        legacyJdbcTemplate.executeUpdate(sql, user.getAccount(), user.getPassword());

        assertAll(
                () -> verify(pstmt).setObject(1, user.getAccount()),
                () -> verify(pstmt).setObject(2, user.getPassword()),
                () -> verify(pstmt, times(1)).executeUpdate(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close()
        );
    }

    @Test
    void 데이터_생성_예외_발생() throws SQLException {
        TestUser user = new TestUser("jojo", "1234");
        String sql = "insert into test-user (account, password) values (?, ?)";

        when(pstmt.executeUpdate()).thenThrow(SQLException.class);

        assertAll(
                () -> assertThatThrownBy(() -> legacyJdbcTemplate.executeUpdate(sql, user.getAccount(), user.getPassword()))
                        .isInstanceOf(DataAccessException.class),
                () -> verify(pstmt).setObject(1, user.getAccount()),
                () -> verify(pstmt).setObject(2, user.getPassword()),
                () -> verify(pstmt, times(1)).executeUpdate(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close()
        );
    }

    @Test
    void 단일_데이터_조회_성공() throws SQLException {
        TestUser user = new TestUser("jojo", "1234");
        String sql = "select id, account, password from users where account = ?";

        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("account")).thenReturn("jojo");
        when(rs.getString("password")).thenReturn("1234");

        Optional<TestUser> actual = legacyJdbcTemplate.executeQueryWithSingleData(sql, this::generateUser,
                user.getAccount());

        assertAll(
                () -> assertThat(actual).isPresent(),
                () -> assertThat(actual.get().getAccount()).isEqualTo(user.getAccount()),
                () -> verify(pstmt).setObject(1, user.getAccount()),
                () -> verify(pstmt, times(1)).executeQuery(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close(),
                () -> verify(rs, times(1)).close()
        );
    }

    @Test
    void 단일_데이터_조회_에러_발생() throws SQLException {
        TestUser user = new TestUser("jojo", "1234");
        String sql = "select id, account, password from users where account = ?";

        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("id")).thenThrow(SQLException.class);

        assertAll(
                () -> assertThatThrownBy(
                        () -> legacyJdbcTemplate.executeQueryWithSingleData(sql, this::generateUser, user.getAccount()))
                        .isInstanceOf(DataAccessException.class),
                () -> verify(pstmt).setObject(1, user.getAccount()),
                () -> verify(pstmt, times(1)).executeQuery(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close(),
                () -> verify(rs, times(1)).close()
        );
    }

    @Test
    void 단일_데이터_조회_실패() throws SQLException {
        TestUser user = new TestUser("jojo", "1234");
        String sql = "select id, account, password from users where account = ?";

        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<TestUser> actual = legacyJdbcTemplate.executeQueryWithSingleData(sql, this::generateUser,
                user.getAccount());

        assertAll(
                () -> assertThat(actual).isEmpty(),
                () -> verify(pstmt).setObject(1, user.getAccount()),
                () -> verify(pstmt, times(1)).executeQuery(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close(),
                () -> verify(rs, times(1)).close()
        );
    }

    @Test
    void 복수_데이터_조회_성공() throws SQLException {
        String sql = "select id, account, password from users";

        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getLong("id")).thenReturn(1L, 2L);
        when(rs.getString("account")).thenReturn("jojo", "cutehuman");
        when(rs.getString("password")).thenReturn("jojo1234", "cutehuman1234");

        List<TestUser> actual = legacyJdbcTemplate.executeQueryWithMultiData(sql, this::generateUser);

        assertAll(
                () -> assertThat(actual).hasSize(2),
                () -> assertThat(actual.get(0).getAccount()).isEqualTo("jojo"),
                () -> assertThat(actual.get(1).getAccount()).isEqualTo("cutehuman"),
                () -> verify(pstmt, times(1)).executeQuery(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close(),
                () -> verify(rs, times(1)).close()
        );
    }

    @Test
    void 복수_데이터_조회_에러_발생() throws SQLException {
        String sql = "select id, account, password from users";

        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(SQLException.class);

        assertAll(
                () -> assertThatThrownBy(() -> legacyJdbcTemplate.executeQueryWithMultiData(sql, this::generateUser))
                        .isInstanceOf(DataAccessException.class),
                () -> verify(pstmt, times(1)).executeQuery(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close(),
                () -> verify(rs, times(1)).close()
        );
    }

    @Test
    void 복수_데이터_조회_실패() throws SQLException {
        String sql = "select id, account, password from users";

        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<TestUser> actual = legacyJdbcTemplate.executeQueryWithMultiData(sql, this::generateUser);

        assertAll(
                () -> assertThat(actual).isEmpty(),
                () -> verify(pstmt, times(1)).executeQuery(),
                () -> verify(pstmt, times(1)).close(),
                () -> verify(conn, times(1)).close(),
                () -> verify(rs, times(1)).close()
        );
    }

    private TestUser generateUser(ResultSet rs) {
        try {
            return new TestUser(
                    rs.getLong("id"),
                    rs.getString("account"),
                    rs.getString("password")
            );
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    static class TestUser {

        private final Long id;
        private final String account;
        private final String password;

        public TestUser(String account, String password) {
            this(null, account, password);
        }

        public TestUser(Long id, String account, String password) {
            this.id = id;
            this.account = account;
            this.password = password;
        }

        public String getAccount() {
            return account;
        }

        public String getPassword() {
            return password;
        }
    }
}
