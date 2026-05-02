package Interffaces;

import java.sql.SQLException;
import java.util.List;

public interface InterfaceCRUD<T> {

    public void ajouter(T t) throws SQLException;
    public void modifier(T t) throws SQLException;
    public void supprimer(int id) throws SQLException;
    public List<T> afficher() throws SQLException;
}
//Un contrat que tous les services doivent respecter. Le <T> est un générique :
// il sera remplacé par RendezVous ou CompteRendu selon le service.