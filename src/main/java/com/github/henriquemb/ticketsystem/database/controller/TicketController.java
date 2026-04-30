package com.github.henriquemb.ticketsystem.database.controller;

import com.github.henriquemb.ticketsystem.database.factory.ConnectionFactory;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TicketController {
    private List<TicketModel> search(String sql) {
        Connection conn = null;
        List<TicketModel> tickets = new ArrayList<>();

        try {
            conn = ConnectionFactory.createConnection();
            ResultSet rset = conn.prepareStatement(sql).executeQuery();

            while (rset.next()) {
                tickets.add(new TicketModel(
                        rset.getInt(1),
                        rset.getString(2),
                        rset.getString(3),
                        rset.getString(4),
                        rset.getString(5),
                        rset.getTimestamp(6),
                        rset.getDouble(7),
                        rset.getBoolean(8),
                        rset.getTimestamp(9)
                ));
            }
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar tickets");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return tickets;
    }

    private List<TicketModel> searchByPlayer(String sql, String player) {
        Connection conn = null;
        List<TicketModel> tickets = new ArrayList<>();

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, player);

            ResultSet rset = pstm.executeQuery();

            while (rset.next()) {
                tickets.add(new TicketModel(
                        rset.getInt(1),
                        rset.getString(2),
                        rset.getString(3),
                        rset.getString(4),
                        rset.getString(5),
                        rset.getTimestamp(6),
                        rset.getDouble(7),
                        rset.getBoolean(8),
                        rset.getTimestamp(9)
                ));
            }
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar tickets");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return tickets;
    }

    public int create(String player, String request) {
        Connection conn = null;
        String sql = "INSERT INTO ticket (player, request) VALUES (?, ?)";
        int id = 0;

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, player);
            pstm.setString(2, request);

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if(rs.next()) id = rs.getInt(1);
        }
        catch (Exception e) {
            System.out.println("Erro ao criar ticket");
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
        String sql = "DELETE FROM ticket WHERE id = ?";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, id);

            pstm.execute();
        }
        catch (Exception e) {
            System.out.println("Erro ao deletar ticket");
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

    public void update(TicketModel ticket) {
        Connection conn = null;
        String sql = "UPDATE ticket SET response = ?, respondedBy = ?, respondedAt = ?, rating = ?, send = ? WHERE id = ?";

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, ticket.getResponse());
            pstm.setString(2, ticket.getRespondedBy());
            pstm.setTimestamp(3, ticket.getRespondedAt());
            pstm.setDouble(4, ticket.getRating());
            pstm.setBoolean(5, ticket.getSend());
            pstm.setInt(6, ticket.getId());

            pstm.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Erro ao atualizar ticket");
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

    public TicketModel fetchById(int id) {
        Connection conn = null;
        TicketModel ticket = null;
        String sql = "SELECT * FROM ticket WHERE id = ?";

        if (id == 0) return null;

        try {
            conn = ConnectionFactory.createConnection();
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, id);

            ResultSet rset = pstm.executeQuery();

            ticket = new TicketModel(
                    rset.getInt(1),
                    rset.getString(2),
                    rset.getString(3),
                    rset.getString(4),
                    rset.getString(5),
                    rset.getTimestamp(6),
                    rset.getDouble(7),
                    rset.getBoolean(8),
                    rset.getTimestamp(9)
            );
        }
        catch (Exception e) {
            System.out.println("Erro ao buscar ticket");
        }
        finally {
            try {
                if (conn != null) conn.close();
            }
            catch (Exception e) {
                System.out.println("Erro ao fechar conexão");
            }
        }

        return ticket;
    }

    public List<TicketModel> fetchAll() {
        String sql = "SELECT * FROM ticket";
        return search(sql);
    }

    public List<TicketModel> fetchNotAnswered() {
        String sql = "SELECT * FROM ticket WHERE response IS NULL";
        return search(sql);
    }

    public List<TicketModel> fetchAllAnswered() {
        String sql = "SELECT * FROM ticket WHERE response NOT NULL";
        return search(sql);
    }

    public List<TicketModel> fetchAnsweredBy(String player) {
        String sql = "SELECT * FROM ticket WHERE respondedBy = ?";
        return searchByPlayer(sql, player);
    }

    public List<TicketModel> fetchNotSendToPlayer(String player) {
        String sql = "SELECT * FROM ticket WHERE player = ? and NOT send and response NOT NULL";
        return searchByPlayer(sql, player);
    }
}
