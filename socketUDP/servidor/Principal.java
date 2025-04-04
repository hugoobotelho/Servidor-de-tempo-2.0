/* ***************************************************************
* Autor............: Hugo Botelho Santana
* Matricula........: 202210485
* Inicio...........: 29/03/2025
* Ultima alteracao.: 01/04/2025
* Nome.............: Servidor de tempo
* Funcao...........: Oferecer um servico de tempo aos clientes
*************************************************************** */

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

public class Principal {

  private static LocalTime horarioMaquina; // Remover o static daqui

  public Principal() {
    this.horarioMaquina = LocalTime.now();
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
    Principal app = new Principal(); // Criando instância

    // Inicia o servidor UDP em uma thread separada
    Thread servidorUDPThread = new Thread(() -> {
      ServidorTempoUDP servidorTempoUDP = new ServidorTempoUDP();
      servidorTempoUDP.iniciar();
    });
    // Inicia as threads
    servidorUDPThread.start();

    Thread sincronizacaoUDP = new Thread(() -> {
      Sincronizacao sincronizacao = new Sincronizacao(app);
      sincronizacao.iniciarSincronizacao();
    });
    // Inicia as threads
    sincronizacaoUDP.start(); // Inicia sincronização automática

    iniciarHorario();

    System.out.println("Servidor UDP iniciado...");
  }

  /*
   * ***************************************************************
   * Metodo: getHorarioMaquina.
   * Funcao: retorna o horario
   * Parametros: sem parametros.
   * Retorno: rotorna o horario.
   * ***************************************************************
   */
  public LocalTime getHorarioMaquina() {
    return horarioMaquina;
  }

  /*
   * ***************************************************************
   * Metodo: setHorarioMaquina.
   * Funcao: atualiza o horario
   * Parametros: recebe um horario do tipo LocalTime.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  public void setHorarioMaquina(LocalTime horarioMaquina) {
    this.horarioMaquina = horarioMaquina;
  }

  /*
   * ***************************************************************
   * Metodo: iniciarHorario.
   * Funcao: cria uma thread para servidor como relogio virtual
   * Parametros: sem paramentros.
   * Retorno: sem retorno.
   * ***************************************************************
   */
  public static void iniciarHorario() {
    new Thread(() -> {
      while (true) {
        horarioMaquina = horarioMaquina.plusSeconds(1);
        // System.out.println("O horario e: " + horarioMaquina);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
}
