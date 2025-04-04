import java.io.*;
import java.net.*;
import java.util.*;

public class ClienteTempoTCP {
    private String servidorAtivo;
    private int porta;
    private Principal app;
    private Set<String> servidoresConhecidos = new HashSet<>();
    private boolean tentandoReconectar = false; // Indica se está no modo de reconexão
    private String ipServidor;

    // Construtor
    public ClienteTempoTCP(Principal app, String ipInicial, int porta) {
        this.app = app;
        this.porta = porta;
        this.ipServidor = ipInicial;
        conectarServidor(ipServidor);
    }

    // Conectar a um servidor e obter a lista de servidores conhecidos
    public void conectarServidor(String ip) {
        while (true) { // Loop infinito até encontrar um servidor ativo
            try (Socket socket = new Socket(ip, porta);
                    ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())) {

                servidorAtivo = ip; // Define o servidor ativo
                System.out.println("Conectado ao servidor ativo: " + servidorAtivo);

                long inicioTimer = System.currentTimeMillis();

                saida.writeObject("REQ");
                saida.flush();

                // Recebe a lista de servidores conhecidos
                // Recebe resposta
                String resposta = (String) entrada.readObject();
                List<String> novosServidores = (List<String>) entrada.readObject();
                // if (resposta instanceof ArrayList) {
                servidoresConhecidos.addAll((ArrayList<String>) novosServidores); // Agora não duplica!
                System.out.println("Servidores conhecidos: " + servidoresConhecidos);
                // }
                // Object resposta = entrada.readObject();
                long tempoDecorrido = System.currentTimeMillis() - inicioTimer;

                app.processarMensagemRecebida(resposta, tempoDecorrido);

                tentandoReconectar = false; // Conseguiu conectar, então para a tentativa
                return; // Sai do loop pois conseguiu conectar

            } catch (Exception e) {
                System.err.println("Erro ao conectar com " + ip + ": " + e.getMessage());
                escolherNovoServidor(); // Se falhar, tenta outro servidor
            }
        }
    }

    // Escolher um novo servidor ativo
    private void escolherNovoServidor() {
        if (servidoresConhecidos.isEmpty()) {
            System.err.println("Nenhum servidor conhecido. Tentando novamente em 5 segundos...");
            tentarReconectar();
            return;
        }

        for (String ip : servidoresConhecidos) {
            if (!ip.equals(servidorAtivo)) {
                System.out.println("Tentando novo servidor: " + ip);
                conectarServidor(ip);
                if (servidorAtivo.equals(ip))
                    return; // Se conseguiu conectar, sai
            }
        }

        System.err.println("Nenhum servidor disponível no momento. Tentando novamente...");
        tentarReconectar();
    }

    // Modo de reconexão: tenta conectar a cada 5 segundos
    private void tentarReconectar() {
        if (tentandoReconectar)
            return; // Já está tentando, não precisa duplicar

        tentandoReconectar = true;
        new Thread(() -> {
            while (tentandoReconectar) {
                for (String ip : servidoresConhecidos) {
                    System.out.println("Verificando disponibilidade de: " + ip);
                    try (Socket socket = new Socket(ip, porta)) {
                        System.out.println("Servidor voltou: " + ip);
                        conectarServidor(ip);
                        return; // Se conseguir conectar, sai do loop
                    } catch (IOException ignored) {
                        // Servidor ainda indisponível
                    }
                }
                try {
                    Thread.sleep(5000); // Espera 5 segundos antes de tentar de novo
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    // // Enviar requisição ao servidor ativo
    // public void enviarRequisicao(String req) {
    //     new Thread(() -> {
    //         long tempoDecorrido = conectar(req);
    //         if (tempoDecorrido == -1) {
    //             System.err.println(" Servidor caiu! Buscando novo ativo...");
    //             escolherNovoServidor();
    //         }
    //     }).start();
    // }

    // // Tentar conexão e envio de requisição
    // private long conectar(String tipoMensagem) {
    //     try (Socket socket = new Socket(servidorAtivo, porta);
    //             ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
    //             ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())) {

    //         long inicioTimer = System.currentTimeMillis();
    //         saida.writeObject(tipoMensagem);
    //         saida.flush();

    //         String resposta = (String) entrada.readObject();
    //         long tempoDecorrido = System.currentTimeMillis() - inicioTimer;

    //         app.processarMensagemRecebida(resposta, tempoDecorrido);
    //         return tempoDecorrido;
    //     } catch (Exception e) {
    //         return -1; // Indica falha na conexão
    //     }
    // }

    public void setIpServidor(String ip) {
        ipServidor = ip;
        servidorAtivo = ipServidor;
    }
}
