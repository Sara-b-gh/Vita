package services;

import Entites.Disponibilite;
import utils.MyBD;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DisponibiliteCRUD {
    private Connection connection;

    public DisponibiliteCRUD() {
        try {
            this.connection = MyBD.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DisponibiliteCRUD(Connection connection) {
        this.connection = connection;
    }

    // Ajouter
    public boolean ajouter(Disponibilite dispo) {
        String sql = "INSERT INTO disponibilites (medecin_id, date_dispo, heure_debut, heure_fin, statut, id_rdv) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, dispo.getMedecin_id());
            stmt.setObject(2, dispo.getDate_dispo());
            stmt.setObject(3, dispo.getHeure_debut());
            stmt.setObject(4, dispo.getHeure_fin());
            stmt.setString(5, dispo.getStatut());
            if (dispo.getId_rdv() != null) {
                stmt.setInt(6, dispo.getId_rdv());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    dispo.setId_dispo(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Créneaux libres
    public List<Disponibilite> getCreneauxLibres(int medecinId) {
        List<Disponibilite> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilites WHERE medecin_id = ? AND statut = 'libre' AND date_dispo >= CURDATE() ORDER BY date_dispo, heure_debut";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, medecinId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(extractDisponibilite(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Toutes les disponibilités par médecin
    public List<Disponibilite> getByMedecin(int medecinId) {
        List<Disponibilite> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilites WHERE medecin_id = ? ORDER BY date_dispo, heure_debut";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, medecinId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(extractDisponibilite(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Modifier le statut
    public boolean updateStatut(int idDispo, String nouveauStatut, Integer idRdv) {
        String sql = "UPDATE disponibilites SET statut = ?";
        if (idRdv != null) {
            sql += ", id_rdv = ?";
        }
        sql += " WHERE id_dispo = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nouveauStatut);
            if (idRdv != null) {
                stmt.setInt(2, idRdv);
                stmt.setInt(3, idDispo);
            } else {
                stmt.setInt(2, idDispo);
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Occuper un créneau
    public boolean occuperCreneau(int idDispo, int idRdv) {
        return updateStatut(idDispo, "occupee", idRdv);
    }

    // Supprimer
    public boolean delete(int idDispo) {
        String sql = "DELETE FROM disponibilites WHERE id_dispo = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idDispo);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean supprimer(int idDispo) {
        return delete(idDispo);
    }

    private Disponibilite extractDisponibilite(ResultSet rs) throws SQLException {
        Disponibilite d = new Disponibilite();
        d.setId_dispo(rs.getInt("id_dispo"));
        d.setMedecin_id(rs.getInt("medecin_id"));
        d.setDate_dispo(rs.getObject("date_dispo", LocalDate.class));
        d.setHeure_debut(rs.getObject("heure_debut", LocalTime.class));
        d.setHeure_fin(rs.getObject("heure_fin", LocalTime.class));
        d.setStatut(rs.getString("statut"));
        int idRdv = rs.getInt("id_rdv");
        if (!rs.wasNull()) {
            d.setId_rdv(idRdv);
        }
        return d;
    }
}