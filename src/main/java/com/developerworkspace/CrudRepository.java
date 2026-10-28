package com.developerworkspace;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class CrudRepository<T> {
    protected abstract String selectSql();
    protected abstract T map(ResultSet rs) throws SQLException;

    protected Connection connection() throws SQLException {
        return DBConnection.getConnection();
    }

    public List<T> findAll() throws AppException {
        List<T> list = new ArrayList<>();
        try (Connection con = connection();
             PreparedStatement ps = con.prepareStatement(selectSql());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new AppException("Database query failed.", e);
        }
        return list;
    }
}
