package entities;

public class Question {
    private int id;
    private String question;
    private String titreQuiz;
    private String texte;

    public Question() {
    }

    public Question(int id, String question, String titreQuiz, String texte) {
        this.id = id;
        this.question = question;
        this.titreQuiz = titreQuiz;
        this.texte = texte;
    }

    public Question(String question, String titreQuiz, String texte) {
        this.question = question;
        this.titreQuiz = titreQuiz;
        this.texte = texte;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getTitreQuiz() { return titreQuiz; }
    public void setTitreQuiz(String titreQuiz) { this.titreQuiz = titreQuiz; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", question='" + question + '\'' +
                ", titreQuiz='" + titreQuiz + '\'' +
                ", texte='" + texte + '\'' +
                '}';
    }
}
