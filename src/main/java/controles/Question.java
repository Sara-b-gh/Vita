package controles;

import javafx.beans.property.*;

public class Question {

    private final IntegerProperty id           = new SimpleIntegerProperty();
    private final StringProperty  question     = new SimpleStringProperty();
    private final StringProperty  reponseA     = new SimpleStringProperty();
    private final StringProperty  reponseB     = new SimpleStringProperty();
    private final StringProperty  reponseC     = new SimpleStringProperty();
    private final StringProperty  reponseD     = new SimpleStringProperty();
    private final StringProperty  bonneReponse = new SimpleStringProperty();

    public Question(int id, String question,
                    String a, String b, String c, String d,
                    String bonneReponse) {
        this.id.set(id);
        this.question.set(question);
        this.reponseA.set(a);
        this.reponseB.set(b);
        this.reponseC.set(c);
        this.reponseD.set(d);
        this.bonneReponse.set(bonneReponse);
    }

    // --- id ---
    public int getId()                        { return id.get(); }
    public void setId(int v)                  { id.set(v); }
    public IntegerProperty idProperty()       { return id; }

    // --- question ---
    public String getQuestion()               { return question.get(); }
    public void setQuestion(String v)         { question.set(v); }
    public StringProperty questionProperty()  { return question; }

    // --- reponseA ---
    public String getReponseA()               { return reponseA.get(); }
    public void setReponseA(String v)         { reponseA.set(v); }
    public StringProperty reponseAProperty()  { return reponseA; }

    // --- reponseB ---
    public String getReponseB()               { return reponseB.get(); }
    public void setReponseB(String v)         { reponseB.set(v); }
    public StringProperty reponseBProperty()  { return reponseB; }

    // --- reponseC ---
    public String getReponseC()               { return reponseC.get(); }
    public void setReponseC(String v)         { reponseC.set(v); }
    public StringProperty reponseCProperty()  { return reponseC; }

    // --- reponseD ---
    public String getReponseD()               { return reponseD.get(); }
    public void setReponseD(String v)         { reponseD.set(v); }
    public StringProperty reponseDProperty()  { return reponseD; }

    // --- bonneReponse ---
    public String getBonneReponse()                  { return bonneReponse.get(); }
    public void setBonneReponse(String v)             { bonneReponse.set(v); }
    public StringProperty bonneReponseProperty()      { return bonneReponse; }
}
