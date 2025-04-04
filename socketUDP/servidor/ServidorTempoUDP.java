import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServidorTempoUDP {

  public ServidorTempoUDP() {
  }

  /*
   * ***************************************************************
   * Metodo: iniciar.
   * Funcao: cria um socket na porta 6789 e fica rodando esperando requisicoes de
   * clientes
   * Parametros: sem paramentros.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  public void iniciar() {
    try {
      DatagramSocket servidorSocket = new DatagramSocket(6789); // Porta do servidor
      System.out.println("Servidor UDP iniciado na porta 6789...");

      while (true) {
        // Buffer para receber dados
        byte[] dadosRecebidos = new byte[1024];
        DatagramPacket pacoteRecebido = new DatagramPacket(dadosRecebidos, dadosRecebidos.length);

        // Aguarda uma mensagem de um cliente
        servidorSocket.receive(pacoteRecebido);

        // Inicia uma nova thread para processar a mensagem recebida
        ProcessaRequisicao thread = new ProcessaRequisicao(pacoteRecebido, servidorSocket);
        thread.start(); // Usa `start()` diretamente, pois agora é uma Thread
      }
    } catch (Exception e) {
      System.err.println("Erro no servidor UDP: " + e.getMessage());
    }
  }

  private class ProcessaRequisicao extends Thread {
    private final DatagramPacket pacoteRecebido;
    private final DatagramSocket servidorSocket;

    public ProcessaRequisicao(DatagramPacket pacoteRecebido, DatagramSocket servidorSocket) {
      this.pacoteRecebido = pacoteRecebido;
      this.servidorSocket = servidorSocket;
    }

    /*
     * ***************************************************************
     * Metodo: run.
     * Funcao: executa a thread responsavel por tratar a mensagem recebida e enviar o horario da maquina para o cliente que fez a requisicao
     * Parametros: sem paramentros.
     * Retorno: sem retorno.
     * ***************************************************************
     */
    @Override
    public void run() {
      try {
        // Converte os dados recebidos em String
        String mensagemRecebida = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
        System.out.println(
            "Mensagem recebida de " + pacoteRecebido.getAddress() + ":" + pacoteRecebido.getPort());
        System.out.println("Conteudo: " + mensagemRecebida);

        // Divide a mensagem pela estrutura definida:
        String[] partes = mensagemRecebida.split("\\|");
        if (partes.length != 1) {
          System.err.println("Formato invalido ou tipo de mensagem desconhecido.");
          return;
        }

        // Extração dos campos
        String apdu = partes[0];

        // Verifica se a mensagem é do tipo REQ
        if (apdu.equals("REQ")) {
          InetAddress enderecoCliente = pacoteRecebido.getAddress();
          int portaCliente = pacoteRecebido.getPort();
          LocalTime tempoAtual = LocalTime.now();
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
          String horaFormatada = tempoAtual.format(formatter);
          byte[] dadosSaida = String.format("RES|%s", horaFormatada).getBytes();
          DatagramPacket pacoteResposta = new DatagramPacket(dadosSaida, dadosSaida.length, enderecoCliente, 9876); // Porta
                                                                                                                    // fixa
                                                                                                                    // para
                                                                                                                    // resposta
          String mensagemEnviada = new String(pacoteResposta.getData(), 0, pacoteResposta.getLength());
          System.out.println(mensagemEnviada);

          servidorSocket.send(pacoteResposta);
        } else {
          System.err.println("Tipo de mensagem desconhecido: " + apdu);
        }
      } catch (Exception e) {
        System.err.println("Erro ao processar mensagem: " + e.getMessage());
      }
    }
  }

  // public static void main(String[] args) {
  // ServidorTempoUDP servidor = new ServidorTempoUDP();
  // servidor.iniciar();
  // }
}
