package com.github.henriquemb.ticketsystem.database.controller;

import com.github.henriquemb.ticketsystem.database.factory.ConnectionFactory;
import com.github.henriquemb.ticketsystem.database.model.ReportModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ReportController {
    public int create(String player, String reported, String evidence, String reason) {
        Connection conn = null;
        String sql = "INSERT INTO report (player, reported, reason, evidence)  VALUES (?, ?, ?, ?)";
        int id = 0;

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, player);
            pstm.setString(2, reported);
            pstm.setString(3, reason);
            pstm.setString(4, evidence);

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if(rs.next()) id = rs.getInt(1);
        }
        catch (Exception e) {
            System.out.println("Erro ao criar report");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return id;
    }

    public void delete(int id) {
        Connection conn = null;
        String sql = "DELETE FROM report WHERE id = ?";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, id);

            pstm.execute();
        }
        catch (Exception e) {
            System.out.println("Erro ao deletar report");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }
    }

    public void update(ReportModel report) {
        Connection conn = null;
        String sql = "UPDATE report SET reason = ?, evidence = ?, verified = ?, verifiedBy = ?, verifiedAt = ?, status = ? WHERE id = ?";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, report.getReason());
            pstm.setString(2, report.getEvidence());
            pstm.setBoolean(3, report.isVerified());
            pstm.setString(4, report.getVerifiedBy());
            pstm.setTimestamp(5, report.getVerifiedAt());
            pstm.setInt(6, report.getStatus());
            pstm.setInt(7, report.getId());

            pstm.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Erro ao atualizar report");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }
    }

    public ReportModel fetchById(int id) {
        Connection conn = null;
        ReportModel report = null;
        String sql = "SELECT * FROM report WHERE id = ?";

        if (id == 0) return null;

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, id);

            ResultSet rset = pstm.executeQuery();

            report = new ReportModel(
                    rset.getInt(1),
                    rset.getString(2),
                    rset.getString(3),
                    rset.getString(4),
                    rset.getString(5),
                    rset.getBoolean(6),
                    rset.getString(7),
                    rset.getTimestamp(8),
                    rset.getInt(9),
                    rset.getTimestamp(10)
            );
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar reports");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return report;
    }

    public List<ReportModel> fetchAll() {
        Connection conn = null;
        List<ReportModel> reports = new ArrayList<>();
        String sql = "SELECT * FROM report";

        try {
            conn = ConnectionFactory.createConnection();
            ResultSet rset = conn.prepareStatement(sql).executeQuery();

            while (rset.next()) {
                reports.add(new ReportModel(
                        rset.getInt(1),
                        rset.getString(2),
                        rset.getString(3),
                        rset.getString(4),
                        rset.getString(5),
                        rset.getBoolean(6),
                        rset.getString(7),
                        rset.getTimestamp(8),
                        rset.getInt(9),
                        rset.getTimestamp(10)
                ));
            }
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar reports");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return reports;
    }

    public List<ReportModel> fetchNotVerified(List<String> players) {
        Connection conn = null;
        List<ReportModel> reports = new ArrayList<>();
        String sql = "SELECT * FROM report WHERE NOT verified";

        try {
            conn = ConnectionFactory.createConnection();
            ResultSet rset = conn.prepareStatement(sql).executeQuery();

            while (rset.next()) {
                if (players.contains(rset.getString(3)))
                    reports.add(new ReportModel(
                            rset.getInt(1),
                            rset.getString(2),
                            rset.getString(3),
                            rset.getString(4),
                            rset.getString(5),
                            rset.getBoolean(6),
                            rset.getString(7),
                            rset.getTimestamp(8),
                            rset.getInt(9),
                            rset.getTimestamp(10)
                    ));
            }
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar reports");
            e.printStackTrace();
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return reports;
    }

    public List<ReportModel> fetchByPlayer(String player) {
        Connection conn = null;
        List<ReportModel> reports = new ArrayList<>();
        String sql = "SELECT * FROM report WHERE player = ?";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, player);

            ResultSet rset = pstm.executeQuery();

            while (rset.next()) {
                reports.add(new ReportModel(
                        rset.getInt(1),
                        rset.getString(2),
                        rset.getString(3),
                        rset.getString(4),
                        rset.getString(5),
                        rset.getBoolean(6),
                        rset.getString(7),
                        rset.getTimestamp(8),
                        rset.getInt(9),
                        rset.getTimestamp(10)
                ));
            }
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar reports");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return reports;
    }
}
