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
    private Map<InetAddress, Boolean> servidoresDisponiveis = new HashMap<>();

    // Construtor
    public ClienteTempoTCP(Principal app, String ipInicial, int porta) {
        this.app = app;
        this.porta = porta;
        this.ipServidor = ipInicial;
        servidoresConhecidos.add(ipInicial);
        // conectarServidor(ipServidor);
    }

    /*
     * ***************************************************************
     * Metodo: conectarServidor
     * Funcao: Tenta se conectar ao servidor especificado, envia uma requisição e
     * processa a resposta.
     * Se falhar, tenta conectar a outros servidores conhecidos.
     * Parametros: String ip - endereço IP do servidor a ser conectado.
     * Retorno: void
     * ***************************************************************
     */
    public void conectarServidor(String ip) {
        if (!ip.equals("")) {
            new Thread(() -> {
                while (true) { // Loop infinito até encontrar um servidor ativo
                    try (Socket socket = new Socket(ip, porta);
                            ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())) {

                        servidorAtivo = ip; // Define o servidor ativo
                        System.out.println("Conectado ao servidor ativo: " + servidorAtivo);
                        InetAddress endereco = InetAddress.getByName(ip);
                        servidoresDisponiveis.put(endereco, true);

                        long inicioTimer = System.currentTimeMillis();

                        saida.writeObject("REQ");
                        saida.flush();

                        // Recebe a lista de servidores conhecidos
                        // Recebe resposta
                        String resposta = (String) entrada.readObject();
                        List<String> novosServidores = (List<String>) entrada.readObject();
                        // if (resposta instanceof ArrayList) {
                        if (!servidorAtivo.equals("")) {
                            servidoresConhecidos.add(servidorAtivo);
                        }
                        servidoresConhecidos.addAll((ArrayList<String>) novosServidores); // Agora não duplica!

                        for (String servidor : servidoresConhecidos) {
                            servidoresDisponiveis.put(InetAddress.getByName(servidor), true);
                        }
                        System.out.println("Servidores conhecidos: " + servidoresConhecidos);
                        // }
                        // Object resposta = entrada.readObject();
                        long tempoDecorrido = System.currentTimeMillis() - inicioTimer;

                        app.processarMensagemRecebida(resposta, tempoDecorrido);

                        tentandoReconectar = false; // Conseguiu conectar, então para a tentativa
                        // app.configurarHeader();

                        return; // Sai do loop pois conseguiu conectar

                    } catch (Exception e) {
                        System.err.println("Erro ao conectar com " + ip + ": " + e.getMessage());
                        try {
                            InetAddress endereco = InetAddress.getByName(ip);
                            servidoresDisponiveis.put(endereco, false); // Marca como indisponível
                        } catch (UnknownHostException ex) {
                            ex.printStackTrace();
                        }

                        // app.configurarHeader();
                        escolherNovoServidor(); // Se falhar, tenta outro
                                                                                         // servidor
                    }
                }
            }).start();
        }
    }

    /*
     * ***************************************************************
     * Metodo: escolherNovoServidor
     * Funcao: Caso o servidor atual falhe, tenta se conectar a um dos outros
     * servidores conhecidos.
     * Parametros: nenhum.
     * Retorno: void
     * ***************************************************************
     */
    private void escolherNovoServidor() {
        List<String> listaServidores = new ArrayList<>(servidoresConhecidos);
    
        if (listaServidores.isEmpty()) return;
    
        int total = listaServidores.size();
        int indiceAtual = listaServidores.indexOf(servidorAtivo);
    
        // Pega apenas o próximo da lista
        int proximoIndice = (indiceAtual + 1) % total;
        String ip = listaServidores.get(proximoIndice);
    
        System.out.println("Tentando próximo servidor: " + ip);
        conectarServidor(ip);
    }
    

    // private void escolherNovoServidor() {
    // // if (servidoresConhecidos.isEmpty()) {
    // // System.err.println("Nenhum servidor conhecido. Tentando novamente em 5
    // segundos...");
    // // tentarReconectar();
    // // return;
    // // }

    // for (String ip : servidoresConhecidos) {
    // if (!ip.equals(servidorAtivo)) {
    // System.out.println("Tentando novo servidor: " + ip);
    // conectarServidor(ip);
    // if (servidorAtivo.equals(ip))
    // return; // Se conseguiu conectar, sai
    // }
    // }

    // System.err.println("Nenhum servidor disponível no momento. Tentando
    // novamente...");
    // tentarReconectar();
    // }

    /*
     * ***************************************************************
     * Metodo: tentarReconectar
     * Funcao: Inicia uma thread que tenta se reconectar aos servidores conhecidos a
     * cada 5 segundos.
     * Parametros: nenhum.
     * Retorno: void
     * ***************************************************************
     */
    // private void tentarReconectar() {
    //     if (tentandoReconectar)
    //         return; // Já está tentando, não precisa duplicar

    //     tentandoReconectar = true;
    //     new Thread(() -> {
    //         while (tentandoReconectar) {
    //             for (String ip : servidoresConhecidos) {
    //                 System.out.println("Verificando disponibilidade de: " + ip);
    //                 try (Socket socket = new Socket(ip, porta)) {
    //                     System.out.println("Servidor voltou: " + ip);
    //                     conectarServidor(ip);
    //                     // app.configurarHeader();
    //                     return; // Se conseguir conectar, sai do loop
    //                 } catch (IOException ignored) {
    //                     // Servidor ainda indisponível
    //                 }
    //             }
    //             try {
    //                 Thread.sleep(5000); // Espera 5 segundos antes de tentar de novo
    //             } catch (InterruptedException ignored) {
    //             }
    //         }
    //     }).start();
    // }

    /*
     * ***************************************************************
     * Metodo: setIpServidor
     * Funcao: Define um novo IP para o servidor ativo.
     * Parametros: String ip - novo endereço IP do servidor.
     * Retorno: void
     * ***************************************************************
     */
    public void setIpServidor(String ip) {
        ipServidor = ip;
        servidorAtivo = ipServidor;
    }

    /*
     * ***************************************************************
     * Metodo: getServidorAtivo
     * Funcao: Retorna o endereço IP do servidor atualmente conectado.
     * Parametros: nenhum.
     * Retorno: String - IP do servidor ativo.
     * ***************************************************************
     */
    public String getServidorAtivo() {
        return servidorAtivo;
    }

    /*
     * ***************************************************************
     * Metodo: getServidores
     * Funcao: Retorna um mapa contendo os servidores conhecidos e sua
     * disponibilidade.
     * Parametros: nenhum.
     * Retorno: Map<InetAddress, Boolean> - mapa de IPs e status de disponibilidade.
     * ***************************************************************
     */
    public Map<InetAddress, Boolean> getServidores() {
        return servidoresDisponiveis;
    }
}
