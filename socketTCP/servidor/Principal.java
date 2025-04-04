/* ***************************************************************
* Autor............: Hugo Botelho Santana
* Matricula........: 202210485
* Inicio...........: 18/03/2025
* Ultima alteracao.: 20/03/2025
* Nome.............: Servidor de tempo
* Funcao...........: Oferecer um servico de tempo aos clientes
*************************************************************** */

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Principal {

    private static LocalTime horarioMaquina; // Remover o static daqui
    private static List<String> servidoresConhecidos;
    private static Boolean isPrincipal;

    public Principal() {
        this.horarioMaquina = LocalTime.now();
        servidoresConhecidos = new ArrayList<String>();
    }

    public static void main(String[] args) {
        Principal app = new Principal(); // Criando instância
        Scanner ler = new Scanner(System.in);
        // antes de iniciar o servidor, vai perguntar se esse servidor e o principal, se
        // for, pede para inserir os ips dos outros servidores e esse servidor vai
        // informar cada outro servidor os ips que ele conhece
        System.out.println("Esse e o servidor principal? y/n");
        // recebe a resposta
        String resposta = ler.next();
        if (resposta.equals("y")) {
            isPrincipal = true;
            System.out.println("Qual e a quantidade de servidores contando com esse?");
            int qtdServidores = ler.nextInt();
            // recebe a resposta
            for (int i = 0; i < qtdServidores - 1; i++) {
                System.out.println("Digite o ip do servidor:");
                // recebe a resposta
                String ip = ler.next();
                servidoresConhecidos.add(ip);
            }

        } else {
            isPrincipal = false;
        }

        // Inicia o servidor TCP em uma thread separada
        Thread servidorTCPThread = new Thread(() -> {
            ServidorTempoTCP servidorTempoTCP = new ServidorTempoTCP();
            servidorTempoTCP.iniciar();
        });

        Thread sincronizacaoTCP = new Thread(() -> {
            Sincronizacao sincronizacao = new Sincronizacao(app, servidoresConhecidos);
            sincronizacao.iniciarSincronizacao();
        });
        // Inicia as threads
        sincronizacaoTCP.start(); // Inicia sincronização automática

        // Inicia as threads
        servidorTCPThread.start();

        iniciarHorario();

        System.out.println("Servidor TCP iniciado...");
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