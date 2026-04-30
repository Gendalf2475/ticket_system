package com.github.henriquemb.ticketsystem.database.controller;

import com.github.henriquemb.ticketsystem.database.factory.ConnectionFactory;
import com.github.henriquemb.ticketsystem.database.model.SuggestionModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SuggestionController {
    private List<SuggestionModel> search(String sql) {
        Connection conn = null;
        List<SuggestionModel> suggestions = new ArrayList<>();

        try {
            conn = ConnectionFactory.createConnection();
            ResultSet rset = conn.prepareStatement(sql).executeQuery();

            while (rset.next()) {
                suggestions.add(new SuggestionModel(
                        rset.getInt(1),
                        rset.getString(2),
                        rset.getString(3),
                        rset.getString(4),
                        rset.getString(5),
                        rset.getTimestamp(6),
                        rset.getBoolean(7),
                        rset.getTimestamp(8)
                ));
            }
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar sugestão");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return suggestions;
    }

    private List<SuggestionModel> searchByPlayer(String sql, String player) {
        Connection conn = null;
        List<SuggestionModel> suggestions = new ArrayList<>();

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, player);

            ResultSet rset = pstm.executeQuery();

            while (rset.next()) {
                suggestions.add(new SuggestionModel(
                        rset.getInt(1),
                        rset.getString(2),
                        rset.getString(3),
                        rset.getString(4),
                        rset.getString(5),
                        rset.getTimestamp(6),
                        rset.getBoolean(7),
                        rset.getTimestamp(8)
                ));
            }
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar sugestão");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return suggestions;
    }

    public void create(String player, String suggestion) {
        Connection conn = null;
        String sql = "INSERT INTO suggestion (player, suggestion) VALUES (?, ?)";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, player);
            pstm.setString(2, suggestion);

            pstm.execute();
        }
        catch (Exception e) {
            System.out.println("Erro ao criar sugestão");
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

    public void delete(int id) {
        Connection conn = null;
        String sql = "DELETE FROM suggestion WHERE id = ?";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, id);

            pstm.execute();
        }
        catch (Exception e) {
            System.out.println("Erro ao deletar sugestão");
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

    public void update(SuggestionModel suggestion) {
        Connection conn = null;
        String sql = "UPDATE suggestion SET response = ?, respondedBy = ?, respondedAt = ?, send = ? WHERE id = ?";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, suggestion.getResponse());
            pstm.setString(2, suggestion.getRespondedBy());
            pstm.setTimestamp(3, suggestion.getRespondedAt());
            pstm.setBoolean(4, suggestion.getSend());
            pstm.setInt(5, suggestion.getId());

            pstm.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Erro ao deletar sugestão");
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

    public SuggestionModel fetchById(int id) {
        Connection conn = null;
        String sql = "SELECT * FROM suggestion WHERE id = ?";
        SuggestionModel suggestion = null;

        if (id == 0) return null;

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, id);;

            ResultSet rset = pstm.executeQuery();

            suggestion = new SuggestionModel(
                    rset.getInt(1),
                    rset.getString(2),
                    rset.getString(3),
                    rset.getString(4),
                    rset.getString(5),
                    rset.getTimestamp(6),
                    rset.getBoolean(7),
                    rset.getTimestamp(8)
            );
        }
        catch (Exception e) {
            System.out.println("Erro ao criar sugestão");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }
        return suggestion;
    }

    public List<SuggestionModel> fetchAll() {
        String sql = "SELECT * FROM suggestion";
        return search(sql);
    }

    public List<SuggestionModel> fetchNotAnswered() {
        String sql = "SELECT * FROM suggestion WHERE response IS NULL";
        return search(sql);
    }

    public List<SuggestionModel> fetchAnsweredBy(String player) {
        String sql = "SELECT * FROM suggestion WHERE respondedBy = ?";
        return searchByPlayer(sql, player);
    }

    public List<SuggestionModel> fetchNotSendToPlayer(String player) {
        String sql = "SELECT * FROM suggestion WHERE player = ? and NOT send";
        return searchByPlayer(sql, player);
    }
}
