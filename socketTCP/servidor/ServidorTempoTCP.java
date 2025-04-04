import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.util.Map;

public class ServidorTempoTCP {

    public ServidorTempoTCP() {
    }

    public void iniciar() {
        try {
            int portaLocal = 6789;
            ServerSocket servidorSocket = new ServerSocket(portaLocal); // Socket do servidor
            System.out.println("Servidor TCP iniciado na porta " + portaLocal + "...");

            while (true) {
                Socket conexao = servidorSocket.accept(); // Aceita conexões de clientes
                new Thread(new EnviaResposta(conexao)).start();
            }
        } catch (Exception e) {
            System.err.println("Erro no servidor TCP: " + e.getMessage());
        }
    }

    private class EnviaResposta extends Thread {
        private final Socket conexao;

        public EnviaResposta(Socket conexao) {
            this.conexao = conexao;
        }

        @Override
        public void run() {
            try (ObjectInputStream entrada = new ObjectInputStream(conexao.getInputStream());
                 ObjectOutputStream saida = new ObjectOutputStream(conexao.getOutputStream())) {
        
                String mensagemRecebida = (String) entrada.readObject();
                System.out.println("Mensagem recebida via TCP: " + mensagemRecebida);
        
                String resposta = processarMensagem(mensagemRecebida, conexao);
                if (!resposta.isEmpty()) {
                    saida.writeObject(resposta);
                    saida.flush();
                }
        
            } catch (EOFException e) {
                System.err.println("Cliente desconectou abruptamente.");
            } catch (SocketException e) {
                System.err.println("Conexão com o cliente foi encerrada inesperadamente.");
            } catch (IOException e) {
                System.err.println("Erro de I/O ao processar cliente: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.err.println("Erro ao ler objeto do cliente: " + e.getMessage());
            } finally {
                try {
                    conexao.close();
                } catch (IOException e) {
                    System.err.println("Erro ao fechar a conexão: " + e.getMessage());
                }
            }
        }
        

        private String processarMensagem(String mensagem, Socket conexao) {
            String[] partes = mensagem.split("\\|");
            if (partes.length < 1) {
                return "Erro: Mensagem mal formatada. Esperado REQ.";
            }

            String apdu = partes[0].trim();

            if (apdu.toUpperCase().equals("REQ")) {
                LocalTime tempoAtual = LocalTime.now();
                // Formata a hora para retorno
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String horaFormatada = tempoAtual.format(formatter);

                return "RES|" + horaFormatada;
            }
            return "";
        }
    }
}
