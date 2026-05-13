package entities;
import java.time.LocalDateTime;

public class Reponse {
    private int idReponse;
    private int idPatient;
    private int idQuestion;
    private String reponse;
    private LocalDateTime dateReponse;

    public Reponse() {
    }

    public Reponse(int idReponse, int idPatient, int idQuestion, String reponse, LocalDateTime dateReponse) {
        this.idReponse = idReponse;
        this.idPatient = idPatient;
        this.idQuestion = idQuestion;
        this.reponse = reponse;
        this.dateReponse = dateReponse;
    }

    public Reponse(int idPatient, int idQuestion, String reponse, LocalDateTime dateReponse) {
        this.idPatient = idPatient;
        this.idQuestion = idQuestion;
        this.reponse = reponse;
        this.dateReponse = dateReponse;
    }

    public int getIdReponse() { return idReponse; }
    public void setIdReponse(int idReponse) { this.idReponse = idReponse; }

    public int getIdPatient() { return idPatient; }
    public void setIdPatient(int idPatient) { this.idPatient = idPatient; }

    public int getIdQuestion() { return idQuestion; }
    public void setIdQuestion(int idQuestion) { this.idQuestion = idQuestion; }

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }

    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; }

    @Override
    public String toString() {
        return "Reponse{" +
                "idReponse=" + idReponse +
                ", idPatient=" + idPatient +
                ", idQuestion=" + idQuestion +
                ", reponse='" + reponse + '\'' +
                ", dateReponse=" + dateReponse +
                '}';
    }
}
