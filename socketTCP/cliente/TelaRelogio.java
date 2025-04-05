import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TelaRelogio {
    private VBox layout = new VBox(30); // Layout da tela inicial
    private Label relogioLabel; // Label do relógio
    private Thread threadRelogio; // Thread para atualizar o relógio
    private volatile boolean rodando = true; // Flag para controle da Thread
    private Principal app; // Referência à classe Principal
    private static LocalTime tempoAtual = LocalTime.now(); // Variável que mantém o tempo atual do relógio

    public TelaRelogio(Principal app) {
        this.app = app; // Inicializa a referência à tela principal
        // this.tempoAtual = LocalTime.now(); // Define o tempo inicial

        // Criando o label do relógio com estilo
        relogioLabel = new Label();
        relogioLabel.setStyle("-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: white;");

        Image atualizarButtonImage = new Image("./assets/atualizarButton.png");
        ImageView atualizarButtonImageView = new ImageView(atualizarButtonImage);
        Image resetarButtonImage = new Image("./assets/resetarButton.png");
        ImageView resetarButtonImageView = new ImageView(resetarButtonImage);

        Button atualizarButton = new Button();
        atualizarButton.setGraphic(atualizarButtonImageView);
        atualizarButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
        atualizarButton.setOnAction(event -> atualizarRelogio());

        Button resetarButton = new Button();
        resetarButton.setGraphic(resetarButtonImageView);
        resetarButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
        resetarButton.setOnAction(event -> resetarRelogio());

        HBox buttonsHbox = new HBox();
        buttonsHbox.getChildren().addAll(resetarButton, atualizarButton);
        buttonsHbox.setAlignment(javafx.geometry.Pos.CENTER);

        // Espaçador para forçar o alinhamento
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Iniciando a thread do relógio
        iniciarRelogio();

        // Adicionando os elementos ao layout
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.getChildren().addAll(relogioLabel, spacer, buttonsHbox);
    }

    /*
     * ***************************************************************
     * Metodo: iniciarRelogio
     * Funcao: Inicia uma thread que atualiza o tempo no rótulo do relógio a cada
     * segundo.
     * Parametros: nenhum.
     * Retorno: void
     * ***************************************************************
     */
    private void iniciarRelogio() {
        threadRelogio = new Thread(() -> {
            while (rodando) {
                try {
                    // Incrementa o tempo armazenado em 1 segundo
                    // LocalTime tempoAtualizado = app.getTempoAtual();
                    // app.setTempoAtual(tempoAtualizado);
                    tempoAtual = tempoAtual.plusSeconds(1);

                    // Formata a hora para exibição
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    String horaFormatada = tempoAtual.format(formatter);

                    // Atualiza o Label na thread JavaFX
                    Platform.runLater(() -> relogioLabel.setText(horaFormatada));

                    // Aguarda 1 segundo antes de atualizar novamente
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    rodando = false; // Se houver erro, para a thread
                }
            }
        });

        threadRelogio.setDaemon(true); // Define a thread como daemon para encerrar automaticamente
        threadRelogio.start(); // Inicia a thread
    }

    /*
     * ***************************************************************
     * Metodo: atualizarRelogio
     * Funcao: Solicita a atualização do relógio conectando-se ao servidor via TCP,
     * caso um IP esteja configurado.
     * Parametros: nenhum.
     * Retorno: void
     * ***************************************************************
     */
    private void atualizarRelogio() {
        if (app.getIpServidor().isEmpty()) {
            System.out.println("Nenhum IP configurado! Indo para tela de configuracoes...");
            app.mostrarTelaConfiguracoes();
        } else {
            System.out.println("Atualizando relogio com IP: " + app.getIpServidor());
            // String requisisaoAPDU = "REQ";
            // app.iniciarTimer();
            app.getClienteTCP().conectarServidor(app.getIpServidor());
        }
    }

    /*
     * ***************************************************************
     * Metodo: atualizarTempoAtual
     * Funcao: Atualiza a variável estática com o novo horário recebido do servidor.
     * Parametros: LocalTime tempo - novo tempo a ser exibido.
     * Retorno: void
     * ***************************************************************
     */
    public static void atualizarTempoAtual(LocalTime tempo) {
        tempoAtual = tempo;
    }

    /*
     * ***************************************************************
     * Metodo: resetarRelogio
     * Funcao: Reinicia o relógio para 00:00:00 e atualiza a interface gráfica.
     * Parametros: nenhum.
     * Retorno: void
     * ***************************************************************
     */
    private void resetarRelogio() {
        // app.setTempoAtual(LocalTime.of(0, 0, 0)); // Define o relógio para 00:00:00
        tempoAtual = LocalTime.of(0, 0, 0);
        Platform.runLater(() -> relogioLabel.setText("00:00:00"));
        System.out.println("Relógio resetado para 00:00:00.");
    }

    /*
     * ***************************************************************
     * Metodo: getLayout
     * Funcao: Retorna o layout principal da tela do relógio para ser exibido na
     * interface.
     * Parametros: nenhum.
     * Retorno: VBox - layout da tela.
     * ***************************************************************
     */
    public VBox getLayout() {
        return layout;
    }

    /*
     * ***************************************************************
     * Metodo: pararRelogio
     * Funcao: Encerra a execução da thread que atualiza o relógio.
     * Parametros: nenhum.
     * Retorno: void
     * ***************************************************************
     */
    public void pararRelogio() {
        rodando = false;
    }
}
