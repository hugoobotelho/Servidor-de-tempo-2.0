import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LayoutServidores {
  private HBox hbox;
  private ScrollPane scrollPane;
  private Principal app;

  public LayoutServidores(Principal app) {
    this.app = app;
    this.hbox = new HBox();
    this.scrollPane = new ScrollPane(hbox);

    // Configuração do ScrollPane
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(false);
    scrollPane.setMaxWidth(400); // Define a largura máxima do ScrollPane

    // Esconder as barras de rolagem
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

    // Permitir rolagem horizontal usando o scroll do mouse
    scrollPane.setOnScroll(event -> {
      double deltaX = event.getDeltaY() * 2; // Ajusta a sensibilidade do scroll
      scrollPane.setHvalue(scrollPane.getHvalue() - deltaX / scrollPane.getWidth());
    });

    atualizarListaServidores();
  }

  /*
   * ***************************************************************
   * Metodo: atualizarListaServidores
   * Funcao: atualiza na tela os servidores, caso o servidor seja o ativo, ele
   * fica verde, caso esteja disponivel ele fica amarelo, por fim, se estiver
   * indisponivel fica vermelho
   * Parametros: sem parametro.
   * Retorno: sem retorno
   * ***************************************************************
   */
  public void atualizarListaServidores() {
    Platform.runLater(() -> {
      hbox.getChildren().clear(); // Limpa antes de adicionar novos servidores
      hbox.setSpacing(10);

      // Obtém os servidores fictícios
      Map<InetAddress, Boolean> servidores = app.getServidores();
      String servidorAtivo = app.getServidorAtivo(); // Obtemos o servidor ativo

      // Listas para armazenar servidores por status
      List<Label> servidoresAtivos = new ArrayList<>();
      List<Label> servidoresDisponiveis = new ArrayList<>();
      List<Label> servidoresIndisponiveis = new ArrayList<>();

      // Separa os servidores nas listas corretas
      for (Map.Entry<InetAddress, Boolean> entry : servidores.entrySet()) {
        String cor;
        String endereco = entry.getKey().getHostAddress();
        Label labelServidor = new Label(endereco);
        labelServidor.setStyle(
            "-fx-text-fill: #07121F;" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10px;" +
                "-fx-background-radius: 10px;" +
                "-fx-min-width: 150px;" +
                "-fx-min-height: 50px;" // Definir altura mínima
        );

        // Verifica se o servidor é o ativo, se for, fica verde
        if (endereco.equals(servidorAtivo)) {
          labelServidor.setStyle("-fx-background-color: green;" + labelServidor.getStyle());
          servidoresAtivos.add(labelServidor); // Coloca o servidor ativo na lista de ativos
        }
        // Caso não seja o servidor ativo, mas esteja disponível (true), fica amarelo
        else if (entry.getValue() != null && entry.getValue()) {
          labelServidor.setStyle("-fx-background-color: yellow;" + labelServidor.getStyle());
          servidoresDisponiveis.add(labelServidor); // Coloca o servidor disponível na lista de disponíveis
        }
        // Se o servidor não estiver disponível (false), fica vermelho
        else {
          labelServidor.setStyle("-fx-background-color: red;" + labelServidor.getStyle());
          servidoresIndisponiveis.add(labelServidor); // Coloca o servidor indisponível na lista de indisponíveis
        }
      }

      // Adiciona os servidores na ordem desejada: ativo, disponível, indisponível
      hbox.getChildren().addAll(servidoresAtivos);
      hbox.getChildren().addAll(servidoresDisponiveis);
      hbox.getChildren().addAll(servidoresIndisponiveis);

      // Ajusta o HBox para que possa crescer conforme necessário
      hbox.setPrefWidth(Region.USE_COMPUTED_SIZE);
    });
  }

  /*
   * ***************************************************************
   * Metodo: getScrollPane
   * Funcao: retorna o scrollPane
   * Parametros: sem parametro.
   * Retorno: retona o scrollPane
   * ***************************************************************
   */
  public ScrollPane getScrollPane() {
    return scrollPane;
  }
}
