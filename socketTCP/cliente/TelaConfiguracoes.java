import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.regex.Pattern;

public class TelaConfiguracoes {
    private VBox layout = new VBox(30); // Layout da tela inicial
    private Label inputLabel; 
    private TextField inputTextField;
    private Principal app; // Referência à classe principal

    public TelaConfiguracoes(Principal app) {
        this.app = app;

        // Criando o label do input
        inputLabel = new Label("Digite o IP do servidor:");
        inputLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Criando o campo de input (TextField)
        inputTextField = new TextField();
        inputTextField.setPromptText("192.168.0.1"); // Placeholder
        inputTextField.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: gray; -fx-padding: 10px; -fx-background-radius: 10; -fx-border-radius: 10; fx-border-width: 2;");

        VBox inputVBox = new VBox();
        inputVBox.getChildren().addAll(inputLabel, inputTextField);
        inputVBox.setSpacing(20);
        inputVBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        inputVBox.setMaxWidth(300);

        // Criando botões com imagens
        Image inserirIpImage = new Image("./assets/inserirIPButton.png");
        ImageView inserirIpImageView = new ImageView(inserirIpImage);
        Image cancelarImage = new Image("./assets/cancelarButton.png");
        ImageView cancelarImageView = new ImageView(cancelarImage);

        Button inserirIPButton = new Button();
        inserirIPButton.setGraphic(inserirIpImageView);
        inserirIPButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
        inserirIPButton.setOnAction(event -> salvarIP());

        Button cancelarButton = new Button();
        cancelarButton.setGraphic(cancelarImageView);
        cancelarButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
        cancelarButton.setOnAction(event -> voltarParaTelaPrincipal());

        HBox buttonsHbox = new HBox();
        buttonsHbox.getChildren().addAll(cancelarButton, inserirIPButton);
        buttonsHbox.setAlignment(javafx.geometry.Pos.CENTER);

        // Espaçador para forçar o alinhamento
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Adicionando os elementos ao layout
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.getChildren().addAll(inputVBox, spacer, buttonsHbox);
    }

    // Método para validar e salvar o IP na classe Principal
    private void salvarIP() {
        String ip = inputTextField.getText().trim();
        if (validarIP(ip)) {
            app.setIpServidor(ip); // Salva o IP na classe principal
            System.out.println("IP salvo: " + ip);
            app.criarClienteTCP(ip, 2025);
            voltarParaTelaPrincipal(); // Volta para a tela principal
        } else {
            System.out.println("IP invalido!");
            // inputTextField.setStyle("-fx-text-fill: red;"); // Altera a cor do texto para vermelho em caso de erro
        }
    }

    // Método para validar o formato do IP
    private boolean validarIP(String ip) {
        String regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return Pattern.matches(regex, ip);
    }

    // Método para voltar à tela principal
    private void voltarParaTelaPrincipal() {
        app.mostrarTelaRelogio();
    }

    public VBox getLayout() {
        return layout;
    }
}
