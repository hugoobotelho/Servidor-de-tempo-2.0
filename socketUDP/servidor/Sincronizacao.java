import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.geometry.HorizontalDirection;

public class Sincronizacao {
  private final static int porta = 2025;
  private static final String broadCast = obterBroadcast();
  private static final int timeOutMs = 2000;
  private static final int intervaloDeSincronizacao = 5000; // Tempo entre sincronizações (5s)
  private Principal app;
  private InetAddress ipLocal;

  Sincronizacao(Principal app) {
    this.app = app;
    try {
      this.ipLocal = InetAddress.getLocalHost();
    } catch (Exception e) {
      System.err.println("Erro ao obter IP local: " + e.getMessage());
    }
  }

  /*
   * ***************************************************************
   * Metodo: iniciarSincronizacao.
   * Funcao: cria duas threads, uma para enviar mensagens para descobrir os
   * servidores disponiveis e outra para receber essas mensagens e responder os
   * outros servidores
   * Parametros: sem paramentros.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  public void iniciarSincronizacao() {

    new Thread(this::enviarSinc).start();
    new Thread(this::receberSinc).start();
  }

  /*
   * ***************************************************************
   * Metodo: enviarSinc.
   * Funcao: envia uma APDU SINC para os servidores via broadcast e espera uma
   * resposta que contem o horario da maquina do servidor que respondeu
   * Parametros: sem paramentros.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  private void enviarSinc() {
    try {
      DatagramSocket socket = new DatagramSocket();
      while (true) {
        byte[] mensagem = "SINC".getBytes();
        DatagramPacket pacote = new DatagramPacket(mensagem, mensagem.length, InetAddress.getByName(broadCast), porta);
        socket.send(pacote);
        System.out.println("APDU SINC enviada via broadcast");

        // espera respostas
        List<LocalTime> temposRecebidos = new ArrayList<>();
        long inicio = System.currentTimeMillis();

        while (System.currentTimeMillis() - inicio < timeOutMs) {
          byte[] buffer = new byte[1024];
          DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
          try {
            socket.setSoTimeout(timeOutMs);
            socket.receive(resposta);

            if (!resposta.getAddress().equals(ipLocal)) { // ignora se foi ele mesmo que se respondeu
              String msg = new String(resposta.getData(), 0, resposta.getLength());

              if (msg.startsWith("HORA|")) {
                LocalTime horaRecebida = LocalTime.parse(msg.split("\\|")[1]);
                temposRecebidos.add(horaRecebida);
                System.out.println("Resposta recebida de " + resposta.getAddress() + ": " + horaRecebida);
              }
            }

          } catch (SocketTimeoutException e) {
            // TODO: handle exception
          }
        }

        if (!temposRecebidos.isEmpty()) {
          ajustarHorario(temposRecebidos);
        }

        Thread.sleep(intervaloDeSincronizacao);
      }

    } catch (Exception e) {
      System.err.println("Erro ao enviar APDU SINC: " + e.getMessage());
    }
  }

  /*
   * ***************************************************************
   * Metodo: receberSinc.
   * Funcao: fica apto a receber mensagens de outros servidores ou dos clientes,
   * caso a APDU que chegar for SINC, entao ele responde com o seu horario, se for
   * AREYOUALIVE, ele responde ao cliente que esta disponivel
   * Parametros: sem paramentros.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  private void receberSinc() {
    try {
      DatagramSocket socket = new DatagramSocket(porta);
      while (true) {
        byte[] buffer = new byte[1024];
        DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);
        socket.receive(pacoteRecebido);

        String mensagem = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());

        if (mensagem.equals("SINC")) {
          String horaAtual = app.getHorarioMaquina().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
          byte[] resposta = ("HORA|" + horaAtual).getBytes();

          DatagramPacket pacoteResposta = new DatagramPacket(resposta, resposta.length, pacoteRecebido.getAddress(),
              pacoteRecebido.getPort());
          socket.send(pacoteResposta);
          System.out.println("Respondi com meu horário: " + horaAtual);

        } else if (mensagem.equals("AREYOUALIVE")) {
          byte[] resposta = ("IMALIVE").getBytes();

          DatagramPacket pacoteResposta = new DatagramPacket(resposta, resposta.length, pacoteRecebido.getAddress(),
              pacoteRecebido.getPort());
          socket.send(pacoteResposta);
          System.out.println("Respondi que estou ativo");
        }
      }
    } catch (Exception e) {
      System.err.println("Erro ao receber APDU SINC: " + e.getMessage());
    }
  }

  /*
   * ***************************************************************
   * Metodo: ajustarHorario.
   * Funcao: chama a funcao calcular media que calcula a media dos horarios
   * recebidos e atualiza o horario da maquina
   * Parametros: recebe uma lista de tempos recebidos.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  private void ajustarHorario(List<LocalTime> temposRecebidos) {
    LocalTime media = calcularMedia(temposRecebidos);
    System.out.println("Ajustando horário para: " + media);
    app.setHorarioMaquina(media);

  }

  /*
   * ***************************************************************
   * Metodo: calcularMedia.
   * Funcao: calcula a media dos tempos recebidos
   * Parametros: recebe uma lista de tempos recebidos.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  private LocalTime calcularMedia(List<LocalTime> tempos) {
    long somaSegundos = tempos.stream().mapToLong(t -> t.toSecondOfDay()).sum();
    long mediaSegundos = somaSegundos / tempos.size();
    return LocalTime.ofSecondOfDay(mediaSegundos);
  }

  /*
   * ***************************************************************
   * Metodo: obterBroadCast
   * Funcao: retorna o ip de broadcast da rede
   * Parametros: sem parametro.
   * Retorno: retorna um string
   * ***************************************************************
   */
  public static String obterBroadcast() {
    try {
      InetAddress ipLocal = InetAddress.getLocalHost();
      NetworkInterface netInterface = NetworkInterface.getByInetAddress(ipLocal);

      for (InterfaceAddress interfaceAddress : netInterface.getInterfaceAddresses()) {
        InetAddress broadcast = interfaceAddress.getBroadcast();
        if (broadcast != null) {
          return broadcast.getHostAddress();
        }
      }
    } catch (Exception e) {
      System.err.println("Erro ao obter o endereço de broadcast: " + e.getMessage());
    }
    return "255.255.255.255"; // Padrão genérico caso não seja possível detectar
  }

}
