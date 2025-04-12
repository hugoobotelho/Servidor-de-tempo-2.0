import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Sincronizacao {
    private final static int PORTA = 2025;
    private static final int TIMEOUT_MS = 2000;
    private static final int INTERVALO_SINCRONIZACAO = 5000; // 5s entre sincronizações
    private Principal app;
    private InetAddress ipLocal;
    private Set<String> servidoresConhecidos; // Usando um Set para evitar IPs duplicados

    public Sincronizacao(Principal app, List<String> servidoresConhecidos) {
        this.app = app;
        this.servidoresConhecidos = new HashSet<>(servidoresConhecidos);

        try {
            this.ipLocal = InetAddress.getLocalHost();
        } catch (Exception e) {
            System.err.println("Erro ao obter IP local: " + e.getMessage());
        }
    }

    /*
     * ***************************************************************
     * Metodo: iniciarSincronizacao
     * Funcao: Inicia duas threads: uma para atuar como servidor e outra para
     * sincronizar horários com outros servidores.
     * Parametros: nenhum.
     * Retorno: nenhum.
     * ***************************************************************
     */
    public void iniciarSincronizacao() {
        new Thread(this::iniciarServidor).start();
        new Thread(this::enviarSinc).start();
    }

    /*
     * ***************************************************************
     * Metodo: iniciarServidor
     * Funcao: Inicia o servidor TCP que escuta requisições de sincronização.
     * Parametros: nenhum.
     * Retorno: nenhum.
     * ***************************************************************
     */
    private void iniciarServidor() {
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor TCP iniciado na porta " + PORTA);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> processarConexao(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor TCP: " + e.getMessage());
        }
    }

    /*
     * ***************************************************************
     * Metodo: processarConexao
     * Funcao: Processa a conexão recebida, atualiza a lista de servidores e
     * responde com o horário atual.
     * Parametros: Socket socket - conexão recebida.
     * Retorno: nenhum.
     * ***************************************************************
     */
    private void processarConexao(Socket socket) {
        try (ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream())) {

            // Recebe mensagem e servidores conhecidos
            String mensagem = (String) entrada.readObject();
            String ipRemetente = socket.getInetAddress().getHostAddress();

            if (!"REQ".equals(mensagem)) {
                List<String> novosServidores = (List<String>) entrada.readObject();
                // Adiciona novos servidores à lista
                servidoresConhecidos.add(ipRemetente);
                if (novosServidores != null) {
                    servidoresConhecidos.addAll(novosServidores);
                }
            }

            // Processa a mensagem
            if ("SINC".equals(mensagem) || "REQ".equals(mensagem)) {
                String horaAtual = app.getHorarioMaquina().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                saida.writeObject("HORA|" + horaAtual);
                saida.writeObject(new ArrayList<>(servidoresConhecidos)); // Envia lista atualizada
                saida.flush();
                System.out.println("Enviei meu horário para " + ipRemetente + ": " + horaAtual);
            }
            // else if ("AREYOUALIVE".equals(mensagem)) {
            // saida.writeObject("IMALIVE");
            // saida.flush();
            // System.out.println("Respondi que estou ativo para " + ipRemetente);
            // }

        } catch (Exception e) {
            System.err.println("Erro ao processar conexão: " + e.getMessage());
        }
    }

    /*
     * ***************************************************************
     * Metodo: enviarSinc
     * Funcao: Envia requisições de sincronização para os servidores conhecidos,
     * coleta os horários e calcula o novo horário local.
     * Parametros: nenhum.
     * Retorno: nenhum.
     * ***************************************************************
     */
    private void enviarSinc() {
        while (true) {
            List<LocalTime> temposRecebidos = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();
            long inicioTimer = System.currentTimeMillis();
            List<String> ipsParaEnviar = servidoresConhecidos.stream()
                    .filter(ip -> !ip.equals(ipLocal.getHostAddress()))
                    .collect(Collectors.toList());

            for (String ipServidor : ipsParaEnviar) {
                Thread thread = new Thread(() -> {
                    try (Socket socket = new Socket(ipServidor, PORTA);
                            ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())) {

                        // Envia solicitação de sincronização junto com os servidores conhecidos
                        saida.writeObject("SINC");
                        saida.writeObject(new ArrayList<>(servidoresConhecidos));
                        saida.flush();

                        System.out.println("Solicitando horário ao servidor: " + ipServidor);
                        socket.setSoTimeout(TIMEOUT_MS);

                        // Recebe resposta
                        String resposta = (String) entrada.readObject();
                        List<String> novosServidores = (List<String>) entrada.readObject();

                        synchronized (temposRecebidos) { // Sincroniza para evitar concorrência
                            if (resposta.startsWith("HORA|")) {
                                LocalTime horaRecebida = LocalTime.parse(resposta.split("\\|")[1]);
                                temposRecebidos.add(horaRecebida);
                                System.out.println("Recebi horário de " + ipServidor + ": " + horaRecebida);
                            }

                            // Adiciona novos servidores à lista
                            if (novosServidores != null) {
                                servidoresConhecidos.addAll(novosServidores);
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        System.err.println("Tempo limite ao tentar conectar com " + ipServidor);
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("Erro ao se conectar com " + ipServidor + ": " + e.getMessage());
                    }
                });

                thread.start();
                threads.add(thread);
            }

            // Aguarda todas as threads terminarem
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    System.err.println("Erro ao aguardar thread: " + e.getMessage());
                }
            }

            if (!temposRecebidos.isEmpty()) {
                ajustarHorario(temposRecebidos);
            }

            long tempoDecorrido = System.currentTimeMillis() - inicioTimer;
            System.out.println("Tempo decorrido para sincronização: " + tempoDecorrido + "ms");

            try {
                Thread.sleep(INTERVALO_SINCRONIZACAO);
            } catch (InterruptedException e) {
                System.err.println("Erro no intervalo de sincronização: " + e.getMessage());
            }
        }
    }

    /*
     * ***************************************************************
     * Metodo: ajustarHorario
     * Funcao: Define o horário local como a média dos horários recebidos dos outros
     * servidores.
     * Parametros: List<LocalTime> temposRecebidos - lista com os horários
     * recebidos.
     * Retorno: nenhum.
     * ***************************************************************
     */
    private void ajustarHorario(List<LocalTime> temposRecebidos) {
        LocalTime media = calcularMedia(temposRecebidos);
        System.out.println("Ajustando horário para: " + media);
        app.setHorarioMaquina(media);
    }

    /*
     * ***************************************************************
     * Metodo: calcularMedia
     * Funcao: Calcula a média dos horários recebidos em segundos.
     * Parametros: List<LocalTime> tempos - lista com os horários a serem usados no
     * cálculo.
     * Retorno: LocalTime - horário médio resultante.
     * ***************************************************************
     */
    private LocalTime calcularMedia(List<LocalTime> tempos) {
        long somaSegundos = tempos.stream().mapToLong(LocalTime::toSecondOfDay).sum();
        long mediaSegundos = somaSegundos / tempos.size();
        return LocalTime.ofSecondOfDay(mediaSegundos);
    }
}
