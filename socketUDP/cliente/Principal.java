/* ***************************************************************
* Autor............: Hugo Botelho Santana
* Matricula........: 202210485
* Inicio...........: 29/03/2025
* Ultima alteracao.: 01/04/2025
* Nome.............: Servidor de tempo
* Funcao...........: Recebe um servico de tempo de varios servidores
*************************************************************** */

import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    private LayoutServidores layoutServidores = new LayoutServidores(this);
    // TelaConfiguracoes telaConfiguracoes = new TelaConfiguracoes(this);
    private ClienteTempoUDP clienteUDP; // Instancia do cliente UDP
    private long inicioTimer = 0; // Guarda o tempo de envio
    private Hashtable<InetAddress, Boolean> servidores = new Hashtable<>();
    private InetAddress servidorAtivo = null;

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

        ServidoresAtivos servidoresAtivos = new ServidoresAtivos(this);
        servidoresAtivos.iniciarSincronizacao();

        // Define o layout principal na raiz
        root.getChildren().setAll(mainLayout);

        // Criando e conectando o cliente UDP
        // criarClienteUDP(ipServidor, 6789);

        // Configura o evento de encerramento do aplicativo
        primaryStage.setOnCloseRequest(t -> {
            if (clienteUDP != null) {
                clienteUDP.fechar(); // Fecha o cliente UDP
            }
            Platform.exit();
            System.exit(0);
        });
    }

    /*
     * ***************************************************************
     * Metodo: criarClienteUDP
     * Funcao: cria um cliente udp se nao existir, se ja existir, atualiza apenas o
     * IP do servidor
     * Parametros: recebe o ip do servidor e uma porta.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void criarClienteUDP(String ipServidor, int porta) {
        try {
            if (clienteUDP != null) {
                clienteUDP.setIpServidor(ipServidor); // atualiza o ip do servidor caso o usuario mude na tela de
                                                      // configuracoes
            } else {
                clienteUDP = new ClienteTempoUDP(ipServidor, porta); // Inicializa o cliente UDP
            }
            System.out.println("Cliente UDP criado e conectado ao servidor " + ipServidor + ":" + porta);
            iniciarThreadRecebimentoUDP(); // Inicia a thread para receber mensagens via UDP
        } catch (Exception e) {
            System.err.println("Erro ao criar ClienteUDP: " + e.getMessage());
        }
    }

    /*
     * ***************************************************************
     * Metodo: iniciarThreadRecebimentoUDP
     * Funcao: cria uma nova thread para ficar esperando a resposta do servidor
     * Parametros: sem parametro.
     * Retorno: sem retorno
     * ***************************************************************
     */
    private void iniciarThreadRecebimentoUDP() {
        new Thread(() -> {
            try {
                while (true) {
                    String mensagemRecebida = clienteUDP.receberHorario(); // Aguarda mensagens do servidor
                    System.out.println("Mensagem recebida via UDP: " + mensagemRecebida);

                    // Criar uma thread para processar e renderizar a mensagem recebida
                    new Thread(() -> processarMensagemRecebida(mensagemRecebida)).start();
                }
            } catch (Exception e) {
                System.err.println("Erro ao receber mensagem UDP: " + e.getMessage());
            }
        }).start();
    }

    /*
     * ***************************************************************
     * Metodo: processarMensagemRecebida
     * Funcao: pega a mensagem que o servidor mandou e processa para ver se esta de
     * acordo com a APDU e o formato esperado
     * Parametros: recebe uma mensagem do tipo string.
     * Retorno: sem retorno
     * ***************************************************************
     */
    private void processarMensagemRecebida(String mensagemRecebida) {
        try {
            // Separar os campos da mensagem
            String[] partes = mensagemRecebida.split("\\|");
            if (partes.length < 2 || !"RES".equals(partes[0])) {
                System.err.println("Formato de mensagem invalido: " + mensagemRecebida);
                return;
            }

            String horario = partes[1];

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            // Converte a string para LocalTime
            LocalTime horaConvertida = LocalTime.parse(horario, formatter);

            long tempoDecorrido = pararTimer(); // Tempo de ida e volta em milissegundos
            long tempoAjustadoNanos = (tempoDecorrido / 2) * 1_000_000; // Converte milissegundos para nanossegundos

            LocalTime horaCorrigida = horaConvertida.plusNanos(tempoAjustadoNanos);

            TelaRelogio.atualizarTempoAtual(horaCorrigida);

            System.out.println("Hora convertida: " + horaConvertida + " e hora corrigida: " + horaCorrigida
                    + " tempo ajustado: " + tempoAjustadoNanos);

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem recebida: " + e.getMessage());
        }
    }

    /*
     * ***************************************************************
     * Metodo: configurarHeader
     * Funcao: cria um header que contem o titulo Relogio e um scrollpane com os
     * servidores
     * Parametros: sem parametro.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void configurarHeader() {
        Platform.runLater(() -> {
            headerTitulo.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: white;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            layoutServidores.atualizarListaServidores(); // Atualiza sempre que chamar

            headerHbox.getChildren().clear(); // Limpa antes de adicionar novos elementos
            headerHbox.getChildren().addAll(headerTitulo, spacer, layoutServidores.getScrollPane());
            headerHbox.setMaxWidth(Double.MAX_VALUE);
            headerHbox.setSpacing(10);
        });

    }

    /*
     * ***************************************************************
     * Metodo: mostrarTelaRelogio
     * Funcao: adiciona o layout da tela
     * Parametros: sem parametro.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void mostrarTelaRelogio() {
        headerTitulo.setText("Relogio"); // Atualiza o título do cabeçalho
        // TelaRelogio telaRelogio = new TelaRelogio(this);
        contentPane.getChildren().setAll(telaRelogio.getLayout()); // Substitui o conteúdo
    }

    /*
     * ***************************************************************
     * Metodo: setIpServidor
     * Funcao: atualiza o ip do servidor
     * Parametros: string ip.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void setIpServidor(String ip) {
        this.ipServidor = ip;
        System.out.println("IP do servidor atualizado para: " + ip);
    }

    /*
     * ***************************************************************
     * Metodo: getIpServidor
     * Funcao: retorna o ip do servidor
     * Parametros: sem parametro.
     * Retorno: retorna o ip do servidor
     * ***************************************************************
     */
    public String getIpServidor() {
        return ipServidor;
    }

    /*
     * ***************************************************************
     * Metodo: getClienteUDP
     * Funcao: retorna o clienteUDP
     * Parametros: sem parametro.
     * Retorno: retorna o clienteUDP
     * ***************************************************************
     */
    public ClienteTempoUDP getClienteUDP() {
        return clienteUDP;
    }

    /*
     * ***************************************************************
     * Metodo: iniciarTimer
     * Funcao: guarda em uma variavel inicioTimer, o tempo em que essa funcao foi
     * chamada
     * Parametros: sem parametro.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void iniciarTimer() {
        inicioTimer = System.currentTimeMillis();
    }

    /*
     * ***************************************************************
     * Metodo: pararTimer
     * Funcao: calcula o tempo decorrido
     * Parametros: sem parametro.
     * Retorno: retorna o tempo decorrido
     * ***************************************************************
     */
    public long pararTimer() {
        if (inicioTimer == 0) {
            return 0;
        }
        long tempoDecorrido = System.currentTimeMillis() - inicioTimer;
        // System.out.println("Entao caiu aqui e o tempo é: " + tempoDecorrido);
        inicioTimer = 0;
        return tempoDecorrido;
    }

    /*
     * ***************************************************************
     * Metodo: atualizarServidores
     * Funcao: adiciona ou atualiza um servidor no hash de servidores que contem o
     * seu ip e o seu estado de disponivel ou indisponivel
     * Parametros: recebe o ip e o estado do servidor.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void atualizaServidores(InetAddress ip, Boolean isAlive) {
        servidores.put(ip, isAlive); // Apenas adiciona ou atualiza o status
        System.out.println("Servidor atualizado");
    }

    /*
     * ***************************************************************
     * Metodo: setAllServidoresFalse
     * Funcao: seta o estado de todos os servidores como indisponivel
     * Parametros: sem parametro.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void setAllServidoresFalse() {
        for (InetAddress ip : servidores.keySet()) {
            servidores.put(ip, false); // Marca todos como inativos
        }
    }

    /*
     * ***************************************************************
     * Metodo: getServidoresAtivos
     * Funcao: guarda em uma lista apenas os servidores que estao ativos
     * Parametros: sem parametro.
     * Retorno: retorna os servidores ativos
     * ***************************************************************
     */
    public List<InetAddress> getServidoresAtivos() {
        List<InetAddress> ativos = new ArrayList<>();
        for (Map.Entry<InetAddress, Boolean> entry : servidores.entrySet()) {
            if (entry.getValue()) {
                ativos.add(entry.getKey());
            }
        }
        return ativos;
    }

    /*
     * ***************************************************************
     * Metodo: getServidores
     * Funcao: retorna todos os servidores
     * Parametros: sem parametro.
     * Retorno: retorna todos os servidores
     * ***************************************************************
     */
    public Hashtable<InetAddress, Boolean> getServidores() {
        return servidores;
    }

    /*
     * ***************************************************************
     * Metodo: getServidorAtivo
     * Funcao: retorna o servidor que esta ativo no momento, se existir
     * Parametros: sem parametro.
     * Retorno: retorn o servidor ativo
     * ***************************************************************
     */
    public String getServidorAtivo() {
        if (servidorAtivo == null) {
            return null;
        }
        return servidorAtivo.getHostAddress();
    }

    /*
     * ***************************************************************
     * Metodo: setServidorAtivo
     * Funcao: atualiza o servidor que esta ativo
     * Parametros: recebe o ip do servidor ativo.
     * Retorno: sem retorno
     * ***************************************************************
     */
    public void setServidorAtivo(InetAddress servidorAtivo) {
        this.servidorAtivo = servidorAtivo;

        if (servidorAtivo == null) { // Se não houver servidor disponível
            // System.out.println("Nenhum servidor disponível para se tornar o ativo!");
            setIpServidor("");
            return;
        }

        // String requisisaoAPDU = "REQ";
        setIpServidor(servidorAtivo.getHostAddress()); // Agora só chamamos se não for null
        System.out.println("O IP DO SERVIDOR E: " + ipServidor);

        criarClienteUDP(ipServidor, 6789);
        // iniciarTimer();
        // getClienteUDP().enviarRequisicao(requisisaoAPDU);
    }

    /*
     * ***************************************************************
     * Metodo: main.
     * Funcao: metodo para iniciar a aplicacao.
     * Parametros: padrao java.
     * Retorno: sem retorno.
     * ***************************************************************
     */
    public static void main(String[] args) {
        launch(args);
    }

}