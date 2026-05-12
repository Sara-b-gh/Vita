public class TestEmail {
    public static void main(String[] args) {
        Services.SmsService.envoyerNotification(
                "tabainour30@gmail.com",      // ← votre propre email pour tester
                "Test VITA",
                "Bonjour, ceci est un test depuis VITA Hôpital !"
        );
    }
}