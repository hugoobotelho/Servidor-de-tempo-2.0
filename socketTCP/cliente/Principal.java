/* ***************************************************************
* Autor............: Hugo Botelho Santana
* Matricula........: 202210485
* Inicio...........: 18/03/2025
* Ultima alteracao.: 20/03/2025
* Nome.............: Servidor de tempo
* Funcao...........: Receber um servico de tempo do servidor
*************************************************************** */

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Principal extends Application {
    private StackPane root = new StackPane();
    private VBox mainLayout = new VBox(); // Layout principal que contém o header
    private String ipServidor = ""; // IP padrão do servidor
    private Label headerTitulo = new Label("Relogio");
    private HBox headerHbox = new HBox(); // Mantém o cabeçalho fixo
    private StackPane contentPane = new StackPane(); // area que troca as telas
    private Region spacer = new Region(); // Espaçador entre o header e o conteúdo
    TelaRelogio telaRelogio = new TelaRelogio(this);
    TelaConfiguracoes telaConfiguracoes = new TelaConfiguracoes(this);
    private ClienteTempoTCP clienteTCP; // Instancia do cliente TCP
    // private List<String> servidoresConhecidos;

    public void start(Stage primaryStage) {

        root.setStyle("-fx-background-color: #07121F; -fx-padding: 24px;");
        Scene scene = new Scene(root, 750, 430);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Aplicativo de Tempo");
        primaryStage.setResizable(false);
        primaryStage.show();

        // Configura o cabeçalho
        configurarHeader();

        // Define altura fixa do espaçador (80px)
        spacer.setMinHeight(80);
        spacer.setPrefHeight(80);

        // Adiciona o header, espaçador e a área de conteúdo no layout principal
        mainLayout.getChildren().addAll(headerHbox, spacer, contentPane);
        VBox.setVgrow(contentPane, Priority.ALWAYS); // Faz a parte inferior expandir

        // Mostra a tela do relógio ao iniciar
        mostrarTelaRelogio();

        // Define o layout principal na raiz
        root.getChildren().setAll(mainLayout);

        // Criando e conectando o cliente TCP
        criarClienteTCP(ipServidor, 2025);

        // Configura o evento de encerramento do aplicativo
        primaryStage.setOnCloseRequest(t -> {
            if (clienteTCP != null) {
                // clienteTCP.fechar(); // Fecha o cliente TCP
            }
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Método para criar o cliente TCP e iniciar a escuta de mensagens.
     */
    public void criarClienteTCP(String ipServidor, int porta) {
        try {
            if (clienteTCP != null){
                clienteTCP.setIpServidor(ipServidor); //atualiza o ip do servidor caso o usuario mude na tela de configuracoes
            }
            else{
                clienteTCP = new ClienteTempoTCP(this, ipServidor, porta); // Inicializa o cliente TCP
            }
            System.out.println("Cliente TCP criado e conectado ao servidor " + ipServidor + ":" + porta);
            // iniciarThreadRecebimentoTCP(); // Inicia a thread para receber mensagens via TCP
        } catch (Exception e) {
            System.err.println("Erro ao criar ClienteTCP: " + e.getMessage());
        }
    }
 
    /**
     * Método que inicia uma thread para escutar mensagens recebidas via TCP.
     */
    // private void iniciarThreadRecebimentoTCP() {
    //     new Thread(() -> {
    //         try {
    //             while (true) {
    //                 String mensagemRecebida = clienteTCP.receberHorario(); // Aguarda mensagens do servidor
    //                 System.out.println("Mensagem recebida via TCP: " + mensagemRecebida);

    //                 // Criar uma thread para processar e renderizar a mensagem recebida
    //                 new Thread(() -> processarMensagemRecebida(mensagemRecebida)).start();
    //             }
    //         } catch (Exception e) {
    //             System.err.println("Erro ao receber mensagem TCP: " + e.getMessage());
    //         }
    //     }).start();
    // }

    public void processarMensagemRecebida(Object mensagemRecebida, long tempoDecorrido) {
        try {
            System.out.println("Processando mensagem recebida: " + mensagemRecebida);
            // Separar os campos da mensagem
            // String[] partes = mensagemRecebida.split("\\|");
            // if (partes.length < 2 || !"RES".equals(partes[0])) {
            //     System.err.println("Formato de mensagem inválido: " + mensagemRecebida);
            //     return;
            // }
    
            // String horario = partes[1];
    
            // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
            // // Converte a string para LocalTime
            // LocalTime horaConvertida = LocalTime.parse(horario, formatter);
    
            // // Calcula o tempo ajustado
            // long tempoAjustadoNanos = (tempoDecorrido / 2) * 1_000_000; // Converte milissegundos para nanossegundos
 
            // // Corrige a hora
            // LocalTime horaCorrigida = horaConvertida.plusNanos(tempoAjustadoNanos);
        
            // // Atualiza a interface gráfica
            // TelaRelogio.atualizarTempoAtual(horaCorrigida);

            // System.out.println("Hora convertida: " + horaConvertida + 
            //                    " | Hora corrigida: " + horaCorrigida + 
            //                    " | Tempo ajustado: " + tempoAjustadoNanos + " ns" +
            //                    " | Tempo decorrido: " + tempoDecorrido );
    
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem recebida: " + e.getMessage());
        }
    }
    

    // Método para configurar o cabeçalho
    private void configurarHeader() {
        headerTitulo.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Espaçador para alinhamento
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Criando a imagem do botão de configurações
        Image iconImage = new Image("./assets/configIcon.png");
        ImageView iconImageView = new ImageView(iconImage);

        // Criando o botão de configurações
        Button btnConfig = new Button();
        btnConfig.setGraphic(iconImageView);
        btnConfig.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
        btnConfig.setOnAction(event -> mostrarTelaConfiguracoes()); // Alterna para a tela de configurações

        // Configurando o layout do cabeçalho
        headerHbox.getChildren().addAll(headerTitulo, spacer, btnConfig);
        headerHbox.setMaxWidth(Double.MAX_VALUE);
        headerHbox.setSpacing(10);
    }

    // Método para exibir a tela do relógio
    public void mostrarTelaRelogio() {
        headerTitulo.setText("Relogio"); // Atualiza o título do cabeçalho
        // TelaRelogio telaRelogio = new TelaRelogio(this);
        contentPane.getChildren().setAll(telaRelogio.getLayout()); // Substitui o conteúdo
    }

    // Método para exibir a tela de configurações
    public void mostrarTelaConfiguracoes() {
        headerTitulo.setText("Configuracoes"); // Atualiza o título do cabeçalho
        // TelaConfiguracoes telaConfiguracoes = new TelaConfiguracoes(this);
        contentPane.getChildren().setAll(telaConfiguracoes.getLayout()); // Substitui o conteúdo
    }

    // Método para armazenar o IP digitado pelo usuário
    public void setIpServidor(String ip) {
        this.ipServidor = ip;
        System.out.println("IP do servidor atualizado para: " + ip);
    }

    public String getIpServidor() {
        return ipServidor;
    }

    public ClienteTempoTCP getClienteTCP() {
        return clienteTCP;
    }

    public static void main(String[] args) {
        launch(args);
    }

}